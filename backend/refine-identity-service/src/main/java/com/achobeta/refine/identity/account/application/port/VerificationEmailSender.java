package com.achobeta.refine.identity.account.application.port;

import java.time.Duration;

public interface VerificationEmailSender {
    void send(String account, String code, Duration ttl);
}
