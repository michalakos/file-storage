package com.mvasilakos.FileStorage.mapper;

import com.mvasilakos.FileStorage.dto.UserDto;
import com.mvasilakos.FileStorage.model.User;

public class UserMapper implements EntityMapper<User, UserDto> {

    public UserDto toDto(User entity) {
        if (entity == null) return null;

        return UserDto.builder()
            .id(entity.getId())
            .username(entity.getUsername())
            .build();
    }

    public User toEntity(UserDto dto) {
        if (dto == null) return null;
        User entity = new User();
        entity.setId(dto.id());
        entity.setUsername(dto.username());
        return entity;
    }

}
