package com.template.springboot.modules.auth.serviceImpl;

import com.template.springboot.common.exception.BadRequestException;
import com.template.springboot.common.exception.DuplicateResourceException;
import com.template.springboot.common.exception.ResourceNotFoundException;
import com.template.springboot.common.security.CustomUserDetails;
import com.template.springboot.common.security.JwtService;
import com.template.springboot.modules.auth.dto.AuthResponse;
import com.template.springboot.modules.auth.dto.LoginRequest;
import com.template.springboot.modules.auth.dto.RefreshRequest;
import com.template.springboot.modules.auth.dto.RegisterRequest;
import com.template.springboot.modules.auth.service.AuthService;
import com.template.springboot.modules.role.entity.Role;
import com.template.springboot.modules.role.enums.RoleName;
import com.template.springboot.modules.role.repository.RoleRepository;
import com.template.springboot.modules.user.entity.User;
import com.template.springboot.modules.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username is already taken");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException("Email is already in use");
        }
        Role defaultRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new ResourceNotFoundException("Default role missing: " + RoleName.USER));

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.getRoles().add(defaultRole);
        userRepository.save(user);

        return buildResponse(new CustomUserDetails(user));
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsernameOrEmail(), request.getPassword()));
        return buildResponse((CustomUserDetails) auth.getPrincipal());
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest request) {
        Claims claims;
        try {
            claims = jwtService.parse(request.getRefreshToken());
        } catch (JwtException ex) {
            throw new BadRequestException("Invalid refresh token");
        }
        if (!jwtService.isRefreshToken(claims)) {
            throw new BadRequestException("Token is not a refresh token");
        }
        Number uid = claims.get("uid", Number.class);
        if (uid == null) throw new BadRequestException("Invalid refresh token");

        User user = userRepository.findWithRolesById(uid.longValue())
                .orElseThrow(() -> new ResourceNotFoundException("User", claims.getSubject()));
        if (!user.isEnabled()) {
            throw new BadRequestException("Account disabled");
        }
        return buildResponse(new CustomUserDetails(user));
    }

    private AuthResponse buildResponse(CustomUserDetails user) {
        Set<String> authorities = user.authoritiesAsStrings();
        String access = jwtService.generateAccessToken(user.getUsername(), user.getId(), authorities.stream().toList());
        String refresh = jwtService.generateRefreshToken(user.getUsername(), user.getId());
        return new AuthResponse(
                "Bearer", access, refresh, jwtService.getAccessTokenTtlSeconds(),
                user.getId(), user.getUsername(), user.getEmail(), authorities);
    }
}
