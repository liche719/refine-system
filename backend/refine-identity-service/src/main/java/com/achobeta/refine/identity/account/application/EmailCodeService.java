package com.achobeta.refine.identity.account.application;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.identity.account.application.port.VerificationCodeStore;
import com.achobeta.refine.identity.account.application.port.VerificationEmailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;

/**
 * 邮箱验证码：6 位数字、限时有效。
 *
 * <p>安全约束：
 * <ul>
 *   <li>同一账号在 {@code resend-cooldown} 内只能申请一次验证码，避免被当作邮件轰炸入口。</li>
 *   <li>校验失败按账号累计，达到 {@code max-attempts} 后直接作废当前验证码，阻断 6 位码的暴力枚举。</li>
 *   <li>比较使用常量时间比较，避免按响应时间逐位试探。</li>
 * </ul>
 */
@Service
public class EmailCodeService {
    private static final int CODE_INVALID = 2500;
    private static final int TOO_MANY_ATTEMPTS = 2501;
    private static final int RESEND_TOO_FREQUENT = 2502;

    private final VerificationCodeStore codeStore;
    private final VerificationEmailSender emailSender;
    private final Duration ttl;
    private final Duration resendCooldown;
    private final Duration attemptWindow;
    private final int maxAttempts;
    private final SecureRandom random = new SecureRandom();

    public EmailCodeService(VerificationCodeStore codeStore,
                            VerificationEmailSender emailSender,
                            @Value("${refine.mail.code-ttl:PT5M}") Duration ttl,
                            @Value("${refine.mail.resend-cooldown:PT1M}") Duration resendCooldown,
                            @Value("${refine.mail.max-attempts:5}") int maxAttempts,
                            @Value("${refine.mail.attempt-window:PT10M}") Duration attemptWindow) {
        this.codeStore = codeStore;
        this.emailSender = emailSender;
        this.ttl = ttl;
        this.resendCooldown = resendCooldown;
        this.maxAttempts = maxAttempts;
        this.attemptWindow = attemptWindow;
    }

    public void send(String account) {
        if (!codeStore.acquireSendSlot(account, resendCooldown)) {
            throw new AppException(RESEND_TOO_FREQUENT, "verification code was requested too frequently");
        }
        String code = "%06d".formatted(random.nextInt(1_000_000));
        codeStore.save(account, code, ttl);
        codeStore.clearFailedAttempts(account);
        emailSender.send(account, code, ttl);
    }

    public void verify(String account, String code) {
        String expected = codeStore.find(account);
        if (expected == null) {
            throw new AppException(CODE_INVALID, "verification code is invalid or expired");
        }
        if (!constantTimeEquals(expected, code)) {
            long attempts = codeStore.recordFailedAttempt(account, attemptWindow);
            if (attempts >= maxAttempts) {
                codeStore.delete(account);
                codeStore.clearFailedAttempts(account);
                throw new AppException(TOO_MANY_ATTEMPTS, "too many failed attempts, request a new verification code");
            }
            throw new AppException(CODE_INVALID, "verification code is invalid or expired");
        }
        codeStore.delete(account);
        codeStore.clearFailedAttempts(account);
    }

    private static boolean constantTimeEquals(String expected, String provided) {
        if (provided == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                provided.getBytes(StandardCharsets.UTF_8));
    }
}
