package com.orderflow.user.service;

import com.orderflow.user.domain.User;
import com.orderflow.user.domain.UserStatus;
import com.orderflow.user.dto.user.UpdateUserRequest;
import com.orderflow.user.dto.user.UserResponse;
import com.orderflow.user.exception.AccountDisabledException;
import com.orderflow.user.exception.UserNotFoundException;
import com.orderflow.user.repository.UserRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(UUID userId) {

        User user = getActiveUser(userId);

        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateCurrentUser(
            UUID userId,
            UpdateUserRequest request
    ) {

        User user = getActiveUser(userId);

        user.updateDisplayName(request.displayName());

        return UserResponse.from(user);
    }

    private User getActiveUser(UUID userId) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(UserNotFoundException::new);

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountDisabledException();
        }

        return user;
    }
}