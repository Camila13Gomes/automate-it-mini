package com.automateit.backend.controller;

import com.automateit.backend.dto.CreateUserRequest;
import com.automateit.backend.dto.ResetUserPasswordRequest;
import com.automateit.backend.dto.SetUserEnabledRequest;
import com.automateit.backend.dto.UpdateUserRequest;
import com.automateit.backend.dto.UserResponse;
import com.automateit.backend.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserAccountService userService;

    public UserController(UserAccountService userService) { this.userService = userService; }

    @GetMapping
    public List<UserResponse> findAll(Authentication authentication) {
        return userService.findAll(authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request, Authentication authentication) {
        return userService.create(request, authentication);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id,
                               @Valid @RequestBody UpdateUserRequest request,
                               Authentication authentication) {
        return userService.update(id, request, authentication);
    }

    @PatchMapping("/{id}/enabled")
    public UserResponse setEnabled(@PathVariable UUID id,
                                   @Valid @RequestBody SetUserEnabledRequest request,
                                   Authentication authentication) {
        return userService.setEnabled(id, request, authentication);
    }

    @PutMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable UUID id,
                              @Valid @RequestBody ResetUserPasswordRequest request,
                              Authentication authentication) {
        userService.resetPassword(id, request, authentication);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id, Authentication authentication) {
        userService.delete(id, authentication);
    }
}
