package com.mvasilakos.FileStorage.mapper;

import com.mvasilakos.FileStorage.dto.UserDto;
import com.mvasilakos.FileStorage.model.User;

public class UserMapper {

    public static UserDto toDto(User user) {
        return new UserDto(
            user.getId(),
            user.getUsername()
        );
    }

}
