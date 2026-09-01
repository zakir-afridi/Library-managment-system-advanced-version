package com.library.validation;

import com.library.exception.ValidationException;
import com.library.model.Member;

/**
 * Validates Member records and constraints.
 */
public class MemberValidator {

    public static void validate(Member member) {
        if (member == null) {
            throw new ValidationException("Member cannot be null.");
        }
        if (member.getName() == null || member.getName().trim().isEmpty()) {
            throw new ValidationException("Member name is required.");
        }
        if (member.getEmail() != null && !member.getEmail().trim().isEmpty()) {
            String email = member.getEmail().trim();
            if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                throw new ValidationException("Invalid member email format.");
            }
        }
        if (member.getBookLimit() < 1) {
            throw new ValidationException("Book limit must be at least 1.");
        }
    }
}
