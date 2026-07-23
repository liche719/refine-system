package com.achobeta.refine.identity.account.application;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.identity.account.application.port.AccountRepository;
import com.achobeta.refine.identity.account.application.port.LoginEventPort;
import com.achobeta.refine.identity.account.application.port.PasswordHasher;
import com.achobeta.refine.identity.account.application.port.TokenPort;
import com.achobeta.refine.identity.account.application.model.AccountCommands;
import com.achobeta.refine.identity.account.application.model.AccountResults;
import com.achobeta.refine.identity.account.domain.UserAccount;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AccountService {
    private final AccountRepository accounts;
    private final PasswordHasher passwords;
    private final EmailCodeService emailCodes;
    private final TokenPort tokens;
    private final LoginEventPort loginEvents;

    public AccountService(AccountRepository accounts, PasswordHasher passwords, EmailCodeService emailCodes,
                          TokenPort tokens, LoginEventPort loginEvents) {
        this.accounts = accounts; this.passwords = passwords; this.emailCodes = emailCodes;
        this.tokens = tokens; this.loginEvents = loginEvents;
    }

    @Transactional
    public void register(AccountCommands.Register command) {
        emailCodes.verify(command.account(), command.verificationCode());
        if (accounts.findByAccount(command.account()) != null) throw alreadyRegistered();
        UserAccount account = UserAccount.register(UUID.randomUUID().toString(), command.userName(), command.account(),
                passwords.hash(command.password()), LocalDateTime.now());
        try {
            accounts.create(account);
        } catch (AccountAlreadyExistsException exception) {
            throw alreadyRegistered();
        }
    }

    public AccountResults.Login login(AccountCommands.Login command) {
        UserAccount account = accounts.findByAccount(command.account());
        if (account == null || !passwords.matches(command.password(), account.passwordHash())) {
            throw new AppException(2003, "account or password is incorrect");
        }
        if (!account.isEnabled()) throw new AppException(2006, "account is disabled");
        TokenPort.TokenPair pair = tokens.issue(account.userId());
        loginEvents.publish(account.userId());
        return new AccountResults.Login(account.userId(), account.userName(), pair.accessToken(), pair.refreshToken());
    }

    public void logout(String refreshToken) { tokens.invalidate(refreshToken); }

    @Transactional
    public void resetPassword(String accountName, String password, String code) {
        emailCodes.verify(accountName, code);
        UserAccount account = accounts.findByAccount(accountName);
        if (account == null) throw new AppException(2007, "account does not exist");
        accounts.updatePassword(account.changePassword(passwords.hash(password)));
    }

    @Transactional
    public void updatePassword(String userId, String oldPassword, String newPassword) {
        UserAccount account = accounts.findById(userId);
        if (account == null || !passwords.matches(oldPassword, account.passwordHash())) {
            throw new AppException(2003, "original password is incorrect");
        }
        accounts.updatePassword(account.changePassword(passwords.hash(newPassword)));
    }

    public AccountResults.Refresh refreshToken(String refreshToken) {
        TokenPort.RefreshTokens refreshed = tokens.refresh(refreshToken);
        return new AccountResults.Refresh(refreshed.newAccessToken(), refreshed.newRefreshToken());
    }

    private AppException alreadyRegistered() { return new AppException(2012, "email is already registered"); }
}
