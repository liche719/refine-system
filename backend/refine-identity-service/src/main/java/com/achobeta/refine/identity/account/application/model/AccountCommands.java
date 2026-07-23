package com.achobeta.refine.identity.account.application.model;

public final class AccountCommands {
    private AccountCommands() {
    }

    public record Register(String account, String password, String userName, String verificationCode) { }
    public record Login(String account, String password) { }
}
