package com.achobeta.refine.identity.account.api;

import com.achobeta.refine.common.api.AppException;
import com.achobeta.refine.common.api.GlobalExceptionHandler;
import com.achobeta.refine.identity.account.application.AccountService;
import com.achobeta.refine.identity.account.application.EmailCodeService;
import com.achobeta.refine.identity.account.application.model.AccountCommands;
import com.achobeta.refine.identity.account.application.model.AccountResults;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAccountControllerContractTest {
    private static final ObjectMapper JSON = new ObjectMapper();

    @Test
    void registerSuccessMatchesGoldenMaster() throws Exception {
        AccountService accounts = mock(AccountService.class);
        MockMvc mvc = mvc(accounts);

        MvcResult result = mvc.perform(post("/api/userAccount/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"student@example.com\",\"userPassword\":\"Password1\",\"userName\":\"Student\",\"checkCode\":\"123456\"}"))
                .andExpect(status().isOk())
                .andReturn();

        verify(accounts).register(new AccountCommands.Register("student@example.com", "Password1", "Student", "123456"));
        assertGolden("contracts/identity/register-success.json", result);
    }

    @Test
    void registerFailureMatchesGoldenMaster() throws Exception {
        AccountService accounts = mock(AccountService.class);
        doThrow(new AppException(2012, "email is already registered")).when(accounts)
                .register(new AccountCommands.Register("student@example.com", "Password1", "Student", "123456"));

        MvcResult result = mvc(accounts).perform(post("/api/userAccount/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"student@example.com\",\"userPassword\":\"Password1\",\"userName\":\"Student\",\"checkCode\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertGolden("contracts/identity/register-failure.json", result);
    }

    @Test
    void loginSuccessMatchesGoldenMaster() throws Exception {
        AccountService accounts = mock(AccountService.class);
        when(accounts.login(new AccountCommands.Login("student@example.com", "Password1")))
                .thenReturn(new AccountResults.Login("user-1", "Student", "access-token", "refresh-token"));

        MvcResult result = mvc(accounts).perform(post("/api/userAccount/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"student@example.com\",\"userPassword\":\"Password1\"}"))
                .andExpect(status().isOk())
                .andReturn();

        assertGolden("contracts/identity/login-success.json", result);
    }

    @Test
    void passwordErrorMatchesGoldenMaster() throws Exception {
        AccountService accounts = mock(AccountService.class);
        when(accounts.login(new AccountCommands.Login("student@example.com", "WrongPass1")))
                .thenThrow(new AppException(2003, "account or password is incorrect"));

        MvcResult result = mvc(accounts).perform(post("/api/userAccount/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"student@example.com\",\"userPassword\":\"WrongPass1\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertGolden("contracts/identity/login-password-error.json", result);
    }

    @Test
    void disabledAccountMatchesGoldenMaster() throws Exception {
        AccountService accounts = mock(AccountService.class);
        when(accounts.login(new AccountCommands.Login("student@example.com", "Password1")))
                .thenThrow(new AppException(2006, "account is disabled"));

        MvcResult result = mvc(accounts).perform(post("/api/userAccount/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userAccount\":\"student@example.com\",\"userPassword\":\"Password1\"}"))
                .andExpect(status().isBadRequest())
                .andReturn();

        assertGolden("contracts/identity/login-disabled.json", result);
    }

    @Test
    void refreshMatchesGoldenMaster() throws Exception {
        AccountService accounts = mock(AccountService.class);
        when(accounts.refreshToken("old-refresh"))
                .thenReturn(new AccountResults.Refresh("new-access", "new-refresh"));

        MvcResult result = mvc(accounts).perform(post("/api/userAccount/refreshToken")
                        .header("refresh-token", "old-refresh"))
                .andExpect(status().isOk())
                .andReturn();

        assertGolden("contracts/identity/refresh-success.json", result);
    }

    private MockMvc mvc(AccountService accounts) {
        return MockMvcBuilders.standaloneSetup(new UserAccountController(mock(EmailCodeService.class), accounts))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private void assertGolden(String resource, MvcResult result) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertThat(stream).as("golden master %s", resource).isNotNull();
            assertGolden(JSON.readTree(stream), JSON.readTree(result.getResponse().getContentAsString()));
        }
    }

    private void assertGolden(JsonNode expected, JsonNode actual) {
        if (expected.isObject()) {
            assertThat(actual.isObject()).isTrue();
            expected.properties().forEach(entry -> {
                assertThat(actual.has(entry.getKey())).as("field %s", entry.getKey()).isTrue();
                assertGolden(entry.getValue(), actual.get(entry.getKey()));
            });
            return;
        }
        if (expected.isArray()) {
            assertThat(actual.isArray()).isTrue();
            assertThat(actual.size()).isEqualTo(expected.size());
            for (int index = 0; index < expected.size(); index++) assertGolden(expected.get(index), actual.get(index));
            return;
        }
        if (expected.isTextual() && "$any".equals(expected.asText())) return;
        if (expected.isTextual() && "$any-string".equals(expected.asText())) {
            assertThat(actual.isTextual()).isTrue();
            return;
        }
        assertThat(actual).isEqualTo(expected);
    }
}
