package com.achobeta.refine.identity.account.application;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.identity.account.application.port.VerificationCodeStore;
import com.achobeta.refine.identity.account.application.port.VerificationEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailCodeServiceTest {

    private static final String ACCOUNT = "student@example.com";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final Duration COOLDOWN = Duration.ofMinutes(1);
    private static final Duration WINDOW = Duration.ofMinutes(10);
    private static final int MAX_ATTEMPTS = 5;

    private final VerificationEmailSender sender = mock(VerificationEmailSender.class);
    private InMemoryCodeStore store;
    private EmailCodeService service;

    @BeforeEach
    void setUp() {
        store = new InMemoryCodeStore();
        service = new EmailCodeService(store, sender, TTL, COOLDOWN, MAX_ATTEMPTS, WINDOW);
    }

    @Test
    void sendsSixDigitCodeAndRejectsImmediateResend() {
        service.send(ACCOUNT);

        assertThat(store.find(ACCOUNT)).hasSize(6).containsOnlyDigits();
        verify(sender).send(ACCOUNT, store.find(ACCOUNT), TTL);

        assertThatThrownBy(() -> service.send(ACCOUNT))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.getCode()).isEqualTo(2502));
    }

    @Test
    void invalidatesCodeAfterTooManyFailedAttempts() {
        service.send(ACCOUNT);
        String code = store.find(ACCOUNT);
        String wrong = "000000".equals(code) ? "111111" : "000000";

        for (int attempt = 1; attempt < MAX_ATTEMPTS; attempt++) {
            assertThatThrownBy(() -> service.verify(ACCOUNT, wrong))
                    .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.getCode()).isEqualTo(2500));
        }

        assertThatThrownBy(() -> service.verify(ACCOUNT, wrong))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.getCode()).isEqualTo(2501));

        // 计数达到上限后当前验证码被作废，即使拿到正确验证码也必须重新申请
        assertThat(store.find(ACCOUNT)).isNull();
        assertThatThrownBy(() -> service.verify(ACCOUNT, code))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.getCode()).isEqualTo(2500));
    }

    @Test
    void acceptsCorrectCodeOnceAndClearsFailures() {
        service.send(ACCOUNT);
        String code = store.find(ACCOUNT);
        String wrong = "000000".equals(code) ? "111111" : "000000";

        assertThatThrownBy(() -> service.verify(ACCOUNT, wrong))
                .isInstanceOf(AppException.class);
        assertThat(store.failedAttempts(ACCOUNT)).isEqualTo(1);

        assertThatCode(() -> service.verify(ACCOUNT, code)).doesNotThrowAnyException();

        assertThat(store.find(ACCOUNT)).isNull();
        assertThat(store.failedAttempts(ACCOUNT)).isZero();
        assertThatThrownBy(() -> service.verify(ACCOUNT, code)).isInstanceOf(AppException.class);
    }

    @Test
    void resendingResetsFailuresAndIssuesNewCode() {
        service.send(ACCOUNT);
        String firstCode = store.find(ACCOUNT);
        assertThatThrownBy(() -> service.verify(ACCOUNT, "wrong-code")).isInstanceOf(AppException.class);
        assertThat(store.failedAttempts(ACCOUNT)).isEqualTo(1);

        store.clearSendSlot(ACCOUNT);
        service.send(ACCOUNT);

        assertThat(store.failedAttempts(ACCOUNT)).isZero();
        assertThat(store.find(ACCOUNT)).isNotNull();
        assertThatThrownBy(() -> service.verify(ACCOUNT, firstCode))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.getCode()).isEqualTo(2500));
    }

    @Test
    void rejectsVerificationWhenNoCodeWasIssued() {
        assertThatThrownBy(() -> service.verify(ACCOUNT, "123456"))
                .isInstanceOfSatisfying(AppException.class, error -> assertThat(error.getCode()).isEqualTo(2500));
    }

    /** 只实现本用例需要的行为，避免为单测引入 Redis。 */
    private static final class InMemoryCodeStore implements VerificationCodeStore {
        private final Map<String, String> codes = new HashMap<>();
        private final Map<String, Long> attempts = new HashMap<>();
        private final Set<String> sendSlots = new HashSet<>();

        @Override
        public void save(String account, String code, Duration ttl) {
            codes.put(account, code);
        }

        @Override
        public String find(String account) {
            return codes.get(account);
        }

        @Override
        public void delete(String account) {
            codes.remove(account);
        }

        @Override
        public long recordFailedAttempt(String account, Duration window) {
            long next = attempts.merge(account, 1L, Long::sum);
            return next;
        }

        @Override
        public void clearFailedAttempts(String account) {
            attempts.remove(account);
        }

        @Override
        public boolean acquireSendSlot(String account, Duration cooldown) {
            return sendSlots.add(account);
        }

        long failedAttempts(String account) {
            return attempts.getOrDefault(account, 0L);
        }

        void clearSendSlot(String account) {
            sendSlots.remove(account);
        }
    }
}
