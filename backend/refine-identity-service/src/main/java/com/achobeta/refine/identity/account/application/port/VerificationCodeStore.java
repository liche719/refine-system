package com.achobeta.refine.identity.account.application.port;

import java.time.Duration;

public interface VerificationCodeStore {

    void save(String account, String code, Duration ttl);

    String find(String account);

    void delete(String account);

    /**
     * 记录一次校验失败并返回当前窗口内的累计失败次数。
     * 用于阻止对 6 位验证码的暴力枚举。
     */
    long recordFailedAttempt(String account, Duration window);

    /**
     * 校验成功或重新发码后清零失败计数。
     */
    void clearFailedAttempts(String account);

    /**
     * 抢占一次发码名额：冷却期内返回 false，表示本次发码应被拒绝。
     */
    boolean acquireSendSlot(String account, Duration cooldown);
}
