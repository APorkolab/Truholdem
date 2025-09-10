package com.truholdem.controller;

import com.truholdem.dto.*;
import com.truholdem.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Authentication and authorization endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public ResponseEntity<JwtResponseDto> login(@Valid @RequestBody LoginRequestDto loginRequest) {
        logger.info("Login attempt for username: {}", loginRequest.getUsername());
        JwtResponseDto response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Registration successful"),
            @ApiResponse(responseCode = "409", description = "User already exists"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<MessageResponseDto> register(@Valid @RequestBody UserRegistrationDto registrationRequest) {
        logger.info("Registration attempt for username: {}", registrationRequest.getUsername());
        MessageResponseDto response = authService.register(registrationRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh JWT token", description = "Refresh an expired JWT token using refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "403", description = "Invalid refresh token")
    })
    public ResponseEntity<JwtResponseDto> refreshToken(@Valid @RequestBody TokenRefreshRequestDto request) {
        logger.debug("Token refresh request received");
        JwtResponseDto response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Logout user and invalidate refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<MessageResponseDto> logout(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(new MessageResponseDto("User not authenticated"));
        }
        logger.info("Logout request for user: {}", userDetails.getUsername());
        MessageResponseDto response = authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout-all")
    @Operation(summary = "Logout from all devices", description = "Logout user from all devices by invalidating all refresh tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Logout from all devices successful"),
            @ApiResponse(responseCode = "401", description = "User not authenticated")
    })
    public ResponseEntity<MessageResponseDto> logoutAllDevices(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Logout all devices request for user: {}", userDetails.getUsername());
        MessageResponseDto response = authService.logoutAllDevices(userDetails.getUsername());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password", description = "Change user password")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid current password"),
            @ApiResponse(responseCode = "400", description = "Invalid input data")
    })
    public ResponseEntity<MessageResponseDto> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequestDto request) {
        logger.info("Password change request for user: {}", userDetails.getUsername());
        MessageResponseDto response = authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate JWT token", description = "Validate if the current JWT token is valid")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Token is invalid or expired")
    })
    public ResponseEntity<MessageResponseDto> validateToken(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).body(new MessageResponseDto("Token is invalid or expired"));
        }
        logger.debug("Token validation request for user: {}", userDetails.getUsername());
        return ResponseEntity.ok(new MessageResponseDto("Token is valid"));
    }
}
