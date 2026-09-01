package com.library.validation;

import com.library.exception.ValidationException;
import com.library.model.Member;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class MemberValidatorTest {

    @Test
    void testValidMemberPasses() {
        Member member = new Member();
        member.setName("Ahmad Khan");
        member.setEmail("ahmad@example.com");
        member.setBookLimit(5);

        assertDoesNotThrow(() -> MemberValidator.validate(member));
    }

    @Test
    void testEmptyNameThrowsException() {
        Member member = new Member();
        member.setName("");
        member.setEmail("test@example.com");

        assertThrows(ValidationException.class, () -> MemberValidator.validate(member));
    }

    @Test
    void testInvalidEmailThrowsException() {
        Member member = new Member();
        member.setName("Valid Name");
        member.setEmail("invalid-email-string");

        assertThrows(ValidationException.class, () -> MemberValidator.validate(member));
    }

    @Test
    void testZeroBookLimitThrowsException() {
        Member member = new Member();
        member.setName("Valid Name");
        member.setBookLimit(0);

        assertThrows(ValidationException.class, () -> MemberValidator.validate(member));
    }
}
