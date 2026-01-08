package com.productmanager.service;

import com.productmanager.dto.*;
import com.productmanager.entity.Role;
import com.productmanager.entity.User;
import com.productmanager.exception.DuplicateResourceException;
import com.productmanager.exception.InvalidCredentialsException;
import com.productmanager.exception.ResourceNotFoundException;
import com.productmanager.mapper.UserMapper;
import com.productmanager.repository.UserRepository;
import com.productmanager.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    public AuthResponse login(LoginRequest request) {
        log.info("Attempting login for user: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            User user = (User) authentication.getPrincipal();
            String token = jwtTokenProvider.generateToken(authentication);

            log.info("Login successful for user: {}", request.getUsername());
            return AuthResponse.of(
                    token,
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getRole(),
                    jwtTokenProvider.getExpirationTime());
        } catch (BadCredentialsException e) {
            log.warn("Login failed for user: {}", request.getUsername());
            throw new InvalidCredentialsException();
        }
    }

    public AuthResponse register(UserRegistrationRequest request) {
        log.info("Registering new user: {}", request.getUsername());

        validateUserUniqueness(request.getUsername(), request.getEmail());

        // Default to USER role if not specified (non-admin registration)
        if (request.getRole() == null) {
            request.setRole(Role.USER);
        }

        User user = userMapper.toEntity(request);
        User savedUser = userRepository.save(user);

        String token = jwtTokenProvider.generateToken(savedUser);

        log.info("User registered successfully: {}", savedUser.getUsername());
        return AuthResponse.of(
                token,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getRole(),
                jwtTokenProvider.getExpirationTime());
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        log.debug("Fetching user with id: {}", id);
        User user = findUserById(id);
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        log.debug("Fetching user with username: {}", username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        return userMapper.toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse updateUserRole(Long userId, Role newRole) {
        log.info("Updating role for user {} to {}", userId, newRole);

        User user = findUserById(userId);
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);

        log.info("User role updated successfully");
        return userMapper.toResponse(updatedUser);
    }

    public void deleteUser(Long id) {
        log.info("Deleting user with id: {}", id);

        User user = findUserById(id);
        user.setEnabled(false);
        userRepository.save(user);

        log.info("User disabled successfully: {}", id);
    }

    public User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidCredentialsException("User not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    private void validateUserUniqueness(String username, String email) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("User", "username", username);
        }
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User", "email", email);
        }
    }
}
