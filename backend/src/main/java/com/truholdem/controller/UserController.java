package com.truholdem.controller;

import com.truholdem.dto.MessageResponseDto;
import com.truholdem.dto.UserProfileDto;
import com.truholdem.dto.UserUpdateDto;
import com.truholdem.model.User;
import com.truholdem.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "User Management", description = "User profile and management endpoints")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    @Operation(summary = "Get user profile", description = "Get current user's profile information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<UserProfileDto> getCurrentUserProfile(@AuthenticationPrincipal UserDetails userDetails) {
        logger.info("Profile request for user: {}", userDetails.getUsername());
        
        User user = (User) userDetails;
        UserProfileDto profile = userService.getUserProfile(user.getId());
        
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/profile/{userId}")
    @Operation(summary = "Get user profile by ID", description = "Get any user's profile information (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserProfileDto> getUserProfile(@PathVariable UUID userId) {
        logger.info("Admin profile request for user ID: {}", userId);
        UserProfileDto profile = userService.getUserProfile(userId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Operation(summary = "Update user profile", description = "Update current user's profile information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Email already exists")
    })
    public ResponseEntity<UserProfileDto> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UserUpdateDto updateDto) {
        
        logger.info("Profile update request for user: {}", userDetails.getUsername());
        
        User user = (User) userDetails;
        User updatedUser = userService.updateUser(user, updateDto);
        UserProfileDto profile = userService.getUserProfile(updatedUser.getId());
        
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/{userId}/deactivate")
    @Operation(summary = "Deactivate user", description = "Deactivate a user account (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> deactivateUser(@PathVariable UUID userId) {
        logger.info("Admin deactivate user request for user ID: {}", userId);
        userService.deactivateUser(userId);
        return ResponseEntity.ok(new MessageResponseDto("User deactivated successfully"));
    }

    @PostMapping("/{userId}/activate")
    @Operation(summary = "Activate user", description = "Activate a user account (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User activated successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> activateUser(@PathVariable UUID userId) {
        logger.info("Admin activate user request for user ID: {}", userId);
        userService.activateUser(userId);
        return ResponseEntity.ok(new MessageResponseDto("User activated successfully"));
    }

    @PostMapping("/{userId}/roles/{roleName}")
    @Operation(summary = "Add role to user", description = "Add a role to a user (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role added successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
            @ApiResponse(responseCode = "404", description = "User or role not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> addRoleToUser(
            @PathVariable UUID userId,
            @PathVariable String roleName) {
        
        logger.info("Admin add role '{}' to user ID: {}", roleName, userId);
        userService.addRoleToUser(userId, roleName);
        return ResponseEntity.ok(new MessageResponseDto("Role added successfully"));
    }

    @DeleteMapping("/{userId}/roles/{roleName}")
    @Operation(summary = "Remove role from user", description = "Remove a role from a user (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role removed successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required"),
            @ApiResponse(responseCode = "404", description = "User or role not found")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponseDto> removeRoleFromUser(
            @PathVariable UUID userId,
            @PathVariable String roleName) {
        
        logger.info("Admin remove role '{}' from user ID: {}", roleName, userId);
        userService.removeRoleFromUser(userId, roleName);
        return ResponseEntity.ok(new MessageResponseDto("Role removed successfully"));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active users", description = "Get list of all active users (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileDto>> getActiveUsers() {
        logger.info("Admin request for all active users");
        List<User> activeUsers = userService.findAllActiveUsers();
        List<UserProfileDto> profiles = activeUsers.stream()
                .map(user -> userService.getUserProfile(user.getId()))
                .toList();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recently active users", description = "Get list of recently active users (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recently active users retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UserProfileDto>> getRecentlyActiveUsers(
            @RequestParam(defaultValue = "7") int days) {
        
        logger.info("Admin request for users active in last {} days", days);
        Instant since = Instant.now().minusSeconds(days * 24 * 60 * 60);
        List<User> recentUsers = userService.findRecentlyActiveUsers(since);
        List<UserProfileDto> profiles = recentUsers.stream()
                .map(user -> userService.getUserProfile(user.getId()))
                .toList();
        return ResponseEntity.ok(profiles);
    }

    @GetMapping("/stats/new-users")
    @Operation(summary = "Get new user count", description = "Get count of new users in a time period (admin only)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "New user count retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied - admin role required")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Long> getNewUserCount(@RequestParam(defaultValue = "30") int days) {
        logger.info("Admin request for new user count in last {} days", days);
        Instant since = Instant.now().minusSeconds(days * 24 * 60 * 60);
        Long count = userService.countNewUsersInPeriod(since);
        return ResponseEntity.ok(count);
    }
}
