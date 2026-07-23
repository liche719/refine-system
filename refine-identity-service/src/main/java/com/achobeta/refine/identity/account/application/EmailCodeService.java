package com.achobeta.refine.identity.account.application;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.identity.account.application.port.VerificationCodeStore;
import com.achobeta.refine.identity.account.application.port.VerificationEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Service
public class EmailCodeService {
    private final VerificationCodeStore codeStore;
    private final VerificationEmailSender emailSender;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public EmailCodeService(VerificationCodeStore codeStore, VerificationEmailSender emailSender,
                            @Value("${refine.mail.code-ttl:PT5M}") Duration ttl) {
        this.codeStore = codeStore; this.emailSender = emailSender; this.ttl = ttl;
    }
    public void send(String account) {
        String code = "%06d".formatted(random.nextInt(1_000_000));
        codeStore.save(account, code, ttl);
        emailSender.send(account, code, ttl);
    }
    public void verify(String account, String code) {
        String expected = codeStore.find(account);
        if (expected == null || !expected.equals(code)) throw new AppException(2500, "verification code is invalid or expired");
        codeStore.delete(account);
    }
}
