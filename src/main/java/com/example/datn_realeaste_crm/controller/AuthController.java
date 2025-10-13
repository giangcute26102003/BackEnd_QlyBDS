package com.example.datn_realeaste_crm.controller;

import com.example.datn_realeaste_crm.dto.request.ForgotPasswordRequest;
import com.example.datn_realeaste_crm.dto.request.LoginRequest;
import com.example.datn_realeaste_crm.dto.request.RefreshTokenRequest;
import com.example.datn_realeaste_crm.dto.request.ResetPasswordRequest;
import com.example.datn_realeaste_crm.dto.response.AuthResponse;
import com.example.datn_realeaste_crm.service.AuthService;
import com.example.datn_realeaste_crm.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication management APIs")
public class AuthController {
    
    private final AuthService authService;
    private final UserService userService;
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authService.login(loginRequest));
    }
    
    @GetMapping("/user-roles")
    public ResponseEntity<List<String>> getUserRoles(@RequestParam String email) {
        List<String> roles = userService.getUserRoles(email);
        return ResponseEntity.ok(roles);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authService.refreshToken(refreshTokenRequest.getRefreshToken()));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        authService.logout(refreshTokenRequest.getRefreshToken());
        return ResponseEntity.ok().build();
    }
    
    /**
     * Gửi email reset password
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        // TODO: Implement forgot password logic
        // authService.sendResetPasswordEmail(request.getEmail());
        return ResponseEntity.ok("If the email exists, a reset password link has been sent.");
    }
    
    /**
     * Reset password với token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        // TODO: Implement reset password logic
        // authService.resetPassword(request.getToken(), request.getNewPassword(), request.getConfirmPassword());
        return ResponseEntity.ok("Password has been reset successfully.");
    }
    
    /**
     * Verify email token (nếu cần email verification)
     */
    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        // TODO: Implement email verification logic
//        authService.verifyEmail(token);
        return ResponseEntity.ok("Email verified successfully.");
    }
}