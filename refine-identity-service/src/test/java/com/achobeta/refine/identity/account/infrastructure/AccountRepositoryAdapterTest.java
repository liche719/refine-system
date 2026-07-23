package com.achobeta.refine.identity.account.infrastructure;

import com.achobeta.refine.identity.account.application.AccountAlreadyExistsException;
import com.achobeta.refine.identity.account.domain.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountRepositoryAdapterTest {
    @Test
    void translatesDatabaseDuplicateIntoApplicationConflict() {
        UserAccountMapper mapper = mock(UserAccountMapper.class);
        UserAccount account = UserAccount.register(
                "user-1", "Student", "student@example.com", "hash", LocalDateTime.now());
        when(mapper.insert(account)).thenThrow(new DuplicateKeyException("duplicate account"));

        assertThatThrownBy(() -> new AccountRepositoryAdapter(mapper).create(account))
                .isInstanceOf(AccountAlreadyExistsException.class)
                .hasCauseInstanceOf(DuplicateKeyException.class);
    }
}
