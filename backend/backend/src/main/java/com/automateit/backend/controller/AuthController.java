package com.automateit.backend.controller;

import com.automateit.backend.dto.AuthUserResponse;
import com.automateit.backend.dto.CsrfResponse;
import com.automateit.backend.dto.LoginRequest;
import com.automateit.backend.entity.UserAccount;
import com.automateit.backend.service.ProjectService;
import com.automateit.backend.service.UserAccessService;
import com.automateit.backend.service.UserAccountService;
import com.automateit.backend.service.LoginAttemptService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserAccessService access;
    private final UserAccountService users;
    private final ProjectService projects;
    private final LoginAttemptService loginAttempts;
    private final HttpSessionSecurityContextRepository contextRepository = new HttpSessionSecurityContextRepository();

    public AuthController(AuthenticationManager authenticationManager, UserAccessService access,
                          UserAccountService users, ProjectService projects, LoginAttemptService loginAttempts) {
        this.authenticationManager = authenticationManager;
        this.access = access;
        this.users = users;
        this.projects = projects;
        this.loginAttempts = loginAttempts;
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token) {
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/login")
    public AuthUserResponse login(@Valid @RequestBody LoginRequest request,
                                  HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        loginAttempts.ensureNotLocked(request.username());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password()));
        } catch (AuthenticationException exception) {
            loginAttempts.recordFailure(request.username());
            throw exception;
        }
        loginAttempts.recordSuccess(authentication.getName());
        servletRequest.getSession(true);
        servletRequest.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contextRepository.saveContext(context, servletRequest, servletResponse);
        return current(authentication);
    }

    @GetMapping("/me")
    public AuthUserResponse me(Authentication authentication) {
        return current(authentication);
    }

    @PostMapping("/logout")
    public void logout(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
    }

    private AuthUserResponse current(Authentication authentication) {
        UserAccount user = access.currentUser(authentication);
        return users.toAuthResponse(user, projects.findAccessible(authentication));
    }
}
