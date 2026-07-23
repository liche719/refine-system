package com.achobeta.refine.identity.account.application;

public class AccountAlreadyExistsException extends RuntimeException {
    public AccountAlreadyExistsException(Throwable cause) {
        super("account already exists", cause);
    }
}
