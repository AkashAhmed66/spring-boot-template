package com.template.springboot.config;

import com.template.springboot.modules.audit.dto.AuditLogResponse;
import com.template.springboot.modules.audit.entity.AuditLog;
import com.template.springboot.modules.permission.dto.PermissionResponse;
import com.template.springboot.modules.permission.entity.Permission;
import com.template.springboot.modules.product.dto.ProductResponse;
import com.template.springboot.modules.product.entity.Product;
import com.template.springboot.modules.role.dto.RoleResponse;
import com.template.springboot.modules.role.entity.Role;
import com.template.springboot.modules.user.dto.UserResponse;
import com.template.springboot.modules.user.entity.User;
import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Single ModelMapper bean configured for end-to-end automatic mapping.
 *
 * <p>How automatic mapping works in this project:
 * <ul>
 *   <li>Request DTO → Entity: field-by-field, no setup needed beyond matching field names.</li>
 *   <li>Entity → Response DTO: same — provided the response is a mutable class with setters
 *       (Lombok {@code @Getter @Setter @NoArgsConstructor}), <b>not</b> a record.</li>
 *   <li>To collapse a relationship like {@code Set<Role> → Set<String>} (role names), a TypeMap
 *       is registered below for the affected entities.</li>
 * </ul>
 * Adding a new field to an entity + DTO with the same name now flows automatically.
 */
@Configuration
class ModelMapperConfig {

    @Bean
    ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration()
                .setMatchingStrategy(MatchingStrategies.STANDARD)
                .setSkipNullEnabled(true)
                .setFieldMatchingEnabled(true)
                .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);

        Converter<Set<Role>, Set<String>> rolesToNames = ctx ->
                ctx.getSource() == null ? Set.of()
                        : ctx.getSource().stream().map(Role::getName).collect(Collectors.toSet());

        Converter<Set<Permission>, Set<String>> permissionsToNames = ctx ->
                ctx.getSource() == null ? Set.of()
                        : ctx.getSource().stream().map(Permission::getName).collect(Collectors.toSet());

        // User → UserResponse: collapse roles to a set of names.
        mapper.createTypeMap(User.class, UserResponse.class)
                .addMappings(m -> m.using(rolesToNames).map(User::getRoles, UserResponse::setRoles));

        // Role → RoleResponse: collapse permissions to a set of names.
        mapper.createTypeMap(Role.class, RoleResponse.class)
                .addMappings(m -> m.using(permissionsToNames).map(Role::getPermissions, RoleResponse::setPermissions));

        // Pre-create remaining TypeMaps so the first call doesn't pay validation cost.
        mapper.createTypeMap(Product.class, ProductResponse.class);
        mapper.createTypeMap(Permission.class, PermissionResponse.class);
        mapper.createTypeMap(AuditLog.class, AuditLogResponse.class);

        return mapper;
    }
}
