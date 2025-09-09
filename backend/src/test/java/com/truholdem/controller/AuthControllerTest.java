package com.truholdem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.truholdem.dto.*;
import com.truholdem.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import com.truholdem.config.TestSecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private JwtResponseDto jwtResponseDto;
    private MessageResponseDto messageResponseDto;
    private LoginRequestDto loginRequestDto;
    private UserRegistrationDto registrationDto;
    private TokenRefreshRequestDto refreshRequestDto;
    private ChangePasswordRequestDto changePasswordDto;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        jwtResponseDto = new JwtResponseDto();
        jwtResponseDto.setAccessToken("access-token-123");
        jwtResponseDto.setRefreshToken("refresh-token-123");
        jwtResponseDto.setTokenType("Bearer");
        jwtResponseDto.setExpiresIn(3600L);
        jwtResponseDto.setUsername("testuser");
        jwtResponseDto.setEmail("test@example.com");
        jwtResponseDto.setRoles(Collections.singletonList("USER"));

        messageResponseDto = new MessageResponseDto("Success");

        loginRequestDto = new LoginRequestDto();
        loginRequestDto.setUsername("testuser");
        loginRequestDto.setPassword("password");

        registrationDto = new UserRegistrationDto();
        registrationDto.setUsername("newuser");
        registrationDto.setEmail("new@example.com");
        registrationDto.setPassword("password");
        registrationDto.setFirstName("John");
        registrationDto.setLastName("Doe");

        refreshRequestDto = new TokenRefreshRequestDto();
        refreshRequestDto.setRefreshToken("refresh-token-123");

        changePasswordDto = new ChangePasswordRequestDto();
        changePasswordDto.setCurrentPassword("currentPassword");
        changePasswordDto.setNewPassword("newPassword");

        mockUserDetails = User.withUsername("testuser")
                .password("password")
                .authorities("ROLE_USER")
                .build();
    }

    @Test
    void login_ValidCredentials_ReturnsJwtResponse() throws Exception {
        // Given
        when(authService.login(any(LoginRequestDto.class))).thenReturn(jwtResponseDto);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    void login_InvalidCredentials_ReturnsBadRequest() throws Exception {
        // Given
        LoginRequestDto invalidLogin = new LoginRequestDto();
        invalidLogin.setUsername(""); // Invalid - empty username
        invalidLogin.setPassword("password");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidLogin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_ValidRequest_ReturnsSuccessMessage() throws Exception {
        // Given
        when(authService.register(any(UserRegistrationDto.class))).thenReturn(messageResponseDto);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    void register_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        UserRegistrationDto invalidRegistration = new UserRegistrationDto();
        invalidRegistration.setUsername(""); // Invalid - empty username
        invalidRegistration.setEmail("invalid-email"); // Invalid email format
        invalidRegistration.setPassword("123"); // Invalid - too short

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRegistration)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_ValidToken_ReturnsNewJwtResponse() throws Exception {
        // Given
        when(authService.refreshToken(any(TokenRefreshRequestDto.class))).thenReturn(jwtResponseDto);

        // When & Then
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-123"));
    }

    @Test
    void logout_AuthenticatedUser_ReturnsSuccessMessage() throws Exception {
        // Given
        when(authService.logout(eq("testuser"))).thenReturn(messageResponseDto);

        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    void logout_UnauthenticatedUser_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutAllDevices_AuthenticatedUser_ReturnsSuccessMessage() throws Exception {
        // Given
        when(authService.logoutAllDevices(eq("testuser"))).thenReturn(messageResponseDto);

        // When & Then
        mockMvc.perform(post("/api/auth/logout-all")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    void changePassword_AuthenticatedUserValidRequest_ReturnsSuccessMessage() throws Exception {
        // Given
        when(authService.changePassword(eq("testuser"), any(ChangePasswordRequestDto.class)))
                .thenReturn(messageResponseDto);

        // When & Then
        mockMvc.perform(post("/api/auth/change-password")
                        .with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changePasswordDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"));
    }

    @Test
    void changePassword_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Given
        ChangePasswordRequestDto invalidRequest = new ChangePasswordRequestDto();
        invalidRequest.setCurrentPassword(""); // Invalid - empty current password
        invalidRequest.setNewPassword("123"); // Invalid - too short

        // When & Then
        mockMvc.perform(post("/api/auth/change-password")
                        .with(user(mockUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateToken_AuthenticatedUser_ReturnsSuccessMessage() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/validate")
                        .with(user(mockUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Token is valid"));
    }

    @Test
    void validateToken_UnauthenticatedUser_ReturnsUnauthorized() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/auth/validate"))
                .andExpect(status().isUnauthorized());
    }
}
