package com.library.auth;

import com.library.model.User;
import com.library.service.UserService;

/**
 * AUTH BRANCH — service layer.
 * Thin delegate over UserService; keeps auth logic inside the auth/ branch.
 */
public class AuthService {

    private final UserService userService = new UserService();

    public User authenticate(String username, String password) {
        return userService.authenticate(username, password);
    }

    public boolean changePassword(int userId, String current, String newPass) {
        return userService.changePassword(userId, current, newPass);
    }

    public boolean adminResetPassword(int userId, String newPass) {
        return userService.adminResetPassword(userId, newPass);
    }

    public User getUserByUsername(String username) {
        return userService.getUserByUsername(username);
    }

    public User unlockByRecoveryKey(String username, String key) {
        return userService.unlockByRecoveryKey(username, key);
    }
}
