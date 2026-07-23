package com.achobeta.refine.identity.account.application.port;

import java.time.Duration;

public interface VerificationCodeStore {
    void save(String account, String code, Duration ttl);
    String find(String account);
    void delete(String account);
}
