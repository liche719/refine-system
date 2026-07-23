package com.achobeta.refine.identity.account.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountTest {
    @Test
    void createsEnabledAccountAndChangesPasswordImmutably() {
        UserAccount account = UserAccount.register("u1", " User ", "a@example.com", "hash-1", LocalDateTime.MIN);
        UserAccount changed = account.changePassword("hash-2");
        assertThat(account.isEnabled()).isTrue();
        assertThat(account.userName()).isEqualTo("User");
        assertThat(account.passwordHash()).isEqualTo("hash-1");
        assertThat(changed.passwordHash()).isEqualTo("hash-2");
    }

    @Test
    void rejectsIncompleteRegistration() {
        assertThatThrownBy(() -> UserAccount.register("", "User", "a@example.com", "hash", LocalDateTime.MIN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void identifiesDisabledAccount() {
        UserAccount account = new UserAccount(1L, "u1", "User", "a@example.com", "hash", 0, LocalDateTime.MIN);
        assertThat(account.isEnabled()).isFalse();
    }
}
