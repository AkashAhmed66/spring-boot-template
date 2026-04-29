package com.template.springboot.common.mapper;

import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GenericMapper {

    private final ModelMapper modelMapper;

    public GenericMapper(ModelMapper modelMapper) {
        this.modelMapper = modelMapper;
    }

    public <S, T> T map(S source, Class<T> targetType) {
        return source == null ? null : modelMapper.map(source, targetType);
    }

    public <S, T> void merge(S source, T target) {
        if (source != null && target != null) {
            modelMapper.map(source, target);
        }
    }

    public <S, T> List<T> mapList(List<S> source, Class<T> targetType) {
        return source.stream().map(s -> map(s, targetType)).toList();
    }

    public ModelMapper raw() {
        return modelMapper;
    }
}
