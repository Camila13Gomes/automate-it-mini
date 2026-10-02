package com.automateit.backend.service;

import com.automateit.backend.entity.UserAccount;
import com.automateit.backend.repository.UserAccountRepository;
import org.springframework.security.authentication.LockedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserAccountRepository users;

    public LoginAttemptService(UserAccountRepository users) {
        this.users = users;
    }

    @Transactional
    public void ensureNotLocked(String username) {
        users.findByUsernameIgnoreCase(username).ifPresent(user -> {
            LocalDateTime lockedUntil = user.getLockedUntil();
            if (lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now())) {
                throw new LockedException("Account temporarily locked");
            }
            if (lockedUntil != null) {
                user.setLockedUntil(null);
                user.setFailedLoginAttempts(0);
                users.save(user);
            }
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(String username) {
        users.findByUsernameIgnoreCase(username).ifPresent(user -> {
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) return;
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_ATTEMPTS) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
            users.save(user);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(String username) {
        users.findByUsernameIgnoreCase(username).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            user.setLastLoginAt(LocalDateTime.now());
            users.save(user);
        });
    }
}
