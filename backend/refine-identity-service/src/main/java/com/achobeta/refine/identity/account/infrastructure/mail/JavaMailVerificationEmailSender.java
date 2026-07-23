package com.achobeta.refine.identity.account.infrastructure.mail;

import com.achobeta.refine.identity.account.application.port.VerificationEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class JavaMailVerificationEmailSender implements VerificationEmailSender {
    private static final Logger log = LoggerFactory.getLogger(JavaMailVerificationEmailSender.class);
    private final JavaMailSender mailSender;
    private final boolean enabled;
    private final String from;

    public JavaMailVerificationEmailSender(ObjectProvider<JavaMailSender> mailSender,
                                           @Value("${refine.mail.enabled:false}") boolean enabled,
                                           @Value("${refine.mail.from:no-reply@refine.local}") String from) {
        this.mailSender = mailSender.getIfAvailable();
        this.enabled = enabled;
        this.from = from;
    }

    @Override
    public void send(String account, String code, Duration ttl) {
        if (!enabled || mailSender == null) {
            log.info("Mail delivery disabled; verification code generated for {}", account);
            log.debug("Development verification code for {} is {}", account, code);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(account);
        message.setSubject("Refine verification code");
        message.setText("Verification code: " + code + ", valid for " + ttl.toMinutes() + " minutes.");
        mailSender.send(message);
    }
}
