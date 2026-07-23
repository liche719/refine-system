package com.achobeta.refine.identity.account.api;

import com.achobeta.refine.common.api.Response;
import com.achobeta.refine.common.security.UserContext;
import com.achobeta.refine.identity.account.application.AccountService;
import com.achobeta.refine.identity.account.application.EmailCodeService;
import com.achobeta.refine.identity.account.application.model.AccountCommands;
import com.achobeta.refine.identity.account.application.model.AccountResults;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/userAccount")
public class UserAccountController {
    private final EmailCodeService emailCodeService;
    private final AccountService accountService;

    public UserAccountController(EmailCodeService emailCodeService, AccountService accountService) {
        this.emailCodeService = emailCodeService;
        this.accountService = accountService;
    }

    @PostMapping("/sendEmailCode")
    public Response<Void> sendEmailCode(@RequestParam @NotBlank @Email String userAccount) {
        emailCodeService.send(userAccount);
        return Response.success();
    }

    @PostMapping("/register")
    public Response<Void> register(@Valid @RequestBody AccountDtos.RegisterRequest request) {
        accountService.register(new AccountCommands.Register(request.userAccount(), request.userPassword(),
                request.userName(), request.checkCode()));
        return Response.success();
    }

    @PostMapping("/login")
    public Response<AccountDtos.LoginResponse> login(@Valid @RequestBody AccountDtos.LoginRequest request) {
        AccountResults.Login result = accountService.login(
                new AccountCommands.Login(request.userAccount(), request.userPassword()));
        return Response.success(new AccountDtos.LoginResponse(result.userId(), result.userName(),
                result.accessToken(), result.refreshToken()));
    }

    @PostMapping("/logout")
    public Response<Void> logout(@RequestHeader("refresh-token") String refreshToken) {
        accountService.logout(refreshToken);
        return Response.success();
    }

    @PostMapping("/resetPassword")
    public Response<Void> resetPassword(
            @RequestParam @NotBlank @Email String userAccount,
            @RequestParam @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$") String newPassword,
            @RequestParam @NotBlank String checkCode) {
        accountService.resetPassword(userAccount, newPassword, checkCode);
        return Response.success();
    }

    @PostMapping("/updatePassword")
    public Response<Void> updatePassword(
            @RequestParam @NotBlank String oldPassword,
            @RequestParam @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,64}$") String newPassword) {
        accountService.updatePassword(UserContext.get(), oldPassword, newPassword);
        return Response.success();
    }

    @PostMapping("/refreshToken")
    public Response<AccountDtos.RefreshResponse> refreshToken(@RequestHeader("refresh-token") String refreshToken) {
        AccountResults.Refresh result = accountService.refreshToken(refreshToken);
        return Response.success(new AccountDtos.RefreshResponse(result.newAccessToken(), result.newRefreshToken()));
    }
}
