package com.achobeta.refine.identity.account.application.port;

public interface LoginEventPort {
    void publish(String userId);
}
