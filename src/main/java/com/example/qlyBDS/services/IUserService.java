package com.example.qlyBDS.services;

import com.example.qlyBDS.dtos.UpdateUserDTO;
import com.example.qlyBDS.dtos.UserDTO;
import com.example.qlyBDS.models.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserService {

    User createUser(UserDTO userDTO) throws Exception;
    String login(String phoneNumber, String password) throws Exception;
    User getUserDetailsFromToken(String token) throws Exception;
    User updateUser(int userId, UpdateUserDTO updatedUserDTO) throws Exception;
}