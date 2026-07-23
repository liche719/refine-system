package com.achobeta.refine.identity.account.infrastructure;

import com.achobeta.refine.identity.account.application.port.AccountRepository;
import com.achobeta.refine.identity.account.application.AccountAlreadyExistsException;
import com.achobeta.refine.identity.account.domain.UserAccount;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;

@Repository
public class AccountRepositoryAdapter implements AccountRepository {
    private final UserAccountMapper mapper;
    public AccountRepositoryAdapter(UserAccountMapper mapper) { this.mapper = mapper; }
    @Override public UserAccount findByAccount(String account) { return mapper.findByAccount(account); }
    @Override public UserAccount findById(String userId) { return mapper.findById(userId); }
    @Override
    public void create(UserAccount account) {
        try {
            mapper.insert(account);
        } catch (DuplicateKeyException exception) {
            throw new AccountAlreadyExistsException(exception);
        }
    }
    @Override public void updatePassword(UserAccount account) { mapper.updatePasswordById(account.userId(), account.passwordHash()); }
}
