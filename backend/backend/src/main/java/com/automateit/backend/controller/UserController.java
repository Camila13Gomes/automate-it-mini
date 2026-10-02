package com.automateit.backend.controller;

import com.automateit.backend.dto.CreateUserRequest;
import com.automateit.backend.dto.UserResponse;
import com.automateit.backend.service.UserAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserAccountService userService;

    public UserController(UserAccountService userService) { this.userService = userService; }

    @GetMapping
    public List<UserResponse> findAll() { return userService.findAll(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) { return userService.create(request); }
}
