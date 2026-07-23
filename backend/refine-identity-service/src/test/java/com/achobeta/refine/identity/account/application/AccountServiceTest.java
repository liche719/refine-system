package com.achobeta.refine.identity.account.application;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.identity.account.application.model.AccountCommands;
import com.achobeta.refine.identity.account.application.port.AccountRepository;
import com.achobeta.refine.identity.account.application.port.LoginEventPort;
import com.achobeta.refine.identity.account.application.port.PasswordHasher;
import com.achobeta.refine.identity.account.application.port.TokenPort;
import com.achobeta.refine.identity.account.domain.UserAccount;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountServiceTest {
    private final AccountRepository accounts = mock(AccountRepository.class);
    private final PasswordHasher passwords = mock(PasswordHasher.class);
    private final EmailCodeService emailCodes = mock(EmailCodeService.class);
    private final TokenPort tokens = mock(TokenPort.class);
    private final LoginEventPort loginEvents = mock(LoginEventPort.class);
    private final AccountService service = new AccountService(accounts, passwords, emailCodes, tokens, loginEvents);

    @Test
    void registersThroughPortsWithAnEnabledDomainAccount() {
        AccountCommands.Register request = new AccountCommands.Register(
                "student@example.com", "Password1", "Student", "123456");
        when(passwords.hash("Password1")).thenReturn("password-hash");

        service.register(request);

        verify(emailCodes).verify("student@example.com", "123456");
        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(accounts).create(accountCaptor.capture());
        assertThat(accountCaptor.getValue().userId()).isNotBlank();
        assertThat(accountCaptor.getValue().passwordHash()).isEqualTo("password-hash");
        assertThat(accountCaptor.getValue().isEnabled()).isTrue();
    }

    @Test
    void rejectsDisabledAccountWithoutIssuingTokensOrEvents() {
        UserAccount disabled = new UserAccount(1L, "user-1", "Student", "student@example.com",
                "password-hash", 0, LocalDateTime.now());
        when(accounts.findByAccount("student@example.com")).thenReturn(disabled);
        when(passwords.matches("Password1", "password-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.login(new AccountCommands.Login("student@example.com", "Password1")))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getCode()).isEqualTo(2006));
        verify(tokens, never()).issue("user-1");
        verify(loginEvents, never()).publish("user-1");
    }

    @Test
    void updatesPasswordUsingTheDomainTransition() {
        UserAccount account = new UserAccount(1L, "user-1", "Student", "student@example.com",
                "old-hash", 1, LocalDateTime.now());
        when(accounts.findById("user-1")).thenReturn(account);
        when(passwords.matches("OldPassword1", "old-hash")).thenReturn(true);
        when(passwords.hash("NewPassword1")).thenReturn("new-hash");

        service.updatePassword("user-1", "OldPassword1", "NewPassword1");

        ArgumentCaptor<UserAccount> accountCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(accounts).updatePassword(accountCaptor.capture());
        assertThat(accountCaptor.getValue().passwordHash()).isEqualTo("new-hash");
        assertThat(account.passwordHash()).isEqualTo("old-hash");
    }

    @Test
    void translatesConcurrentRegistrationConflictToStableBusinessCode() {
        AccountCommands.Register command = new AccountCommands.Register(
                "student@example.com", "Password1", "Student", "123456");
        when(passwords.hash("Password1")).thenReturn("password-hash");
        doThrow(new AccountAlreadyExistsException(new IllegalStateException("duplicate")))
                .when(accounts).create(org.mockito.ArgumentMatchers.any(UserAccount.class));

        assertThatThrownBy(() -> service.register(command))
                .isInstanceOfSatisfying(AppException.class, exception -> assertThat(exception.getCode()).isEqualTo(2012));
    }
}
