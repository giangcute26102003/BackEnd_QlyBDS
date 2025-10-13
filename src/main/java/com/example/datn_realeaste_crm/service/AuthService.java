package com.example.datn_realeaste_crm.service;

import com.example.datn_realeaste_crm.dto.request.LoginRequest;
import com.example.datn_realeaste_crm.dto.response.AuthResponse;
import com.example.datn_realeaste_crm.dto.response.UserResponse;
import com.example.datn_realeaste_crm.entity.Role;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.entity.UserRole;
import com.example.datn_realeaste_crm.exception.InvalidTokenException;
import com.example.datn_realeaste_crm.repository.UserRoleRepository;
import com.example.datn_realeaste_crm.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserDetailsService userDetailsService;
    private final UserRoleRepository userRoleRepository;
    private final RoleService roleService;

    public AuthResponse login(LoginRequest loginRequest) {
        // 1. Kiểm tra username/password
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = (User) authentication.getPrincipal();

        // 2. Kiểm tra xem user có được cấp selectedRole hay không
        Optional<UserRole> userRole = userRoleRepository.findByUserUserIdAndRoleRoleId(
                user.getUserId(), getRoleIdByName(loginRequest.getSelectedRole()));
        
        if (userRole.isEmpty()) {
            throw new BadCredentialsException("User does not have the selected role: " + loginRequest.getSelectedRole());
        }

        // 3. Truy xuất danh sách permission tương ứng với role
        Set<String> permissions = getPermissionsByRole(loginRequest.getSelectedRole());

        // 4. Sinh JWT token chứa username, role và permissions
        String accessToken = tokenProvider.generateAccessToken(user.getEmail(), loginRequest.getSelectedRole(), permissions);
        String refreshToken = tokenProvider.generateRefreshToken(user, loginRequest.getSelectedRole());

        return AuthResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .selectedRole(loginRequest.getSelectedRole())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Lấy role ID theo tên role
     */
    private Integer getRoleIdByName(String roleName) {
        Role role = roleService.findByRoleName(roleName);
        if (role == null) {
            throw new BadCredentialsException("Role not found: " + roleName);
        }
        return role.getRoleId();
    }

    /**
     * Lấy danh sách permissions theo role name
     */
    private Set<String> getPermissionsByRole(String roleName) {
        Role role = roleService.findByRoleName(roleName);
        if (role == null) {
            throw new BadCredentialsException("Role not found: " + roleName);
        }
        
        return role.getRolePermissions().stream()
                .map(rp -> rp.getPermission().getPermissionName())
                .collect(Collectors.toSet());
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        String role = tokenProvider.getRoleFromToken(refreshToken);

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new InvalidTokenException("User not found for refresh token");
        }

        User user = (User) userDetails;
        
        // Kiểm tra lại xem user vẫn có role này không
        Optional<UserRole> userRole = userRoleRepository.findByUserUserIdAndRoleRoleId(
                user.getUserId(), getRoleIdByName(role));
        
        if (userRole.isEmpty()) {
            throw new InvalidTokenException("User no longer has the role: " + role);
        }

        // Lấy permissions mới nhất cho role
        Set<String> permissions = getPermissionsByRole(role);

        // Create new access token with role and permissions
        String newAccessToken = tokenProvider.generateAccessToken(username, role, permissions);

        return AuthResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .selectedRole(role)
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(String refreshToken) {
        tokenProvider.revokeToken(refreshToken);
    }
}