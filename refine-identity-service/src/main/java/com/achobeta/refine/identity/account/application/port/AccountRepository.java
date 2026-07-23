package com.achobeta.refine.identity.account.application.port;

import com.achobeta.refine.identity.account.domain.UserAccount;

public interface AccountRepository {
    UserAccount findByAccount(String account);
    UserAccount findById(String userId);
    void create(UserAccount account);
    void updatePassword(UserAccount account);
}
