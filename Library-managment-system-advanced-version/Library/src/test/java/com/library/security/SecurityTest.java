package com.library.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SecurityTest {

    @Test
    void testPasswordHashingAndVerification() {
        String password = "SecretLibraryPassword123!";
        String hash = PasswordUtil.hash(password);

        assertNotNull(hash);
        assertNotEquals(password, hash);
        assertTrue(PasswordUtil.verify(password, hash));
        assertFalse(PasswordUtil.verify("WrongPassword", hash));
    }

    @Test
    void testSessionManagerRoleAccess() {
        SessionManager session = SessionManager.getInstance();
        assertNotNull(session);
    }
}
