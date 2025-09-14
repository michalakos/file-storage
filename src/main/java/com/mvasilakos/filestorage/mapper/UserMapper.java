package com.mvasilakos.filestorage.mapper;

import com.mvasilakos.filestorage.dto.UserDto;
import com.mvasilakos.filestorage.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting user details.
 */
@Component
@RequiredArgsConstructor
public class UserMapper implements EntityMapper<User, UserDto> {

  @Override
  public UserDto toDto(User entity) {
    if (entity == null) {
      return null;
    }

    return UserDto.builder()
        .id(entity.getId())
        .username(entity.getUsername())
        .email(entity.getEmail())
        .isBanned(!entity.isEnabled())
        .isAdmin(entity.isAdmin())
        .build();
  }

  @Override
  public User toEntity(UserDto dto) {
    if (dto == null) {
      return null;
    }

    User entity = new User();
    entity.setId(dto.id());
    entity.setUsername(dto.username());
    entity.setEmail(dto.email());
    return entity;
  }

}
