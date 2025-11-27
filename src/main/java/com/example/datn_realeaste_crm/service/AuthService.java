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

        // 2. Lấy tất cả các role của user
        Set<String> allRoles = userRoleRepository.findByUserUserId(user.getUserId())
                .stream()
                .map(ur -> ur.getRole().getRoleName())
                .collect(Collectors.toSet());

        if (allRoles.isEmpty()) {
            throw new BadCredentialsException("User does not have any role assigned");
        }

        // 3. Xác định selectedRole để hiển thị (nếu có)
        String selectedRole;
        if (loginRequest.getSelectedRole() != null && !loginRequest.getSelectedRole().trim().isEmpty()) {
            // Nếu có truyền selectedRole, kiểm tra xem user có role đó không
            if (!allRoles.contains(loginRequest.getSelectedRole())) {
                throw new BadCredentialsException("User does not have the selected role: " + loginRequest.getSelectedRole());
            }
            selectedRole = loginRequest.getSelectedRole();
        } else {
            // Nếu không truyền selectedRole, chọn role đầu tiên
            selectedRole = allRoles.stream().findFirst().orElse(null);
        }

        // 4. Truy xuất tất cả permissions từ tất cả các role
        Set<String> allPermissions = getAllPermissionsFromRoles(allRoles);

        // 5. Sinh JWT token chứa username, tất cả roles và permissions
        String accessToken = tokenProvider.generateAccessToken(user.getEmail(), allRoles, allPermissions);
        String refreshToken = tokenProvider.generateRefreshToken(user, allRoles);

        return AuthResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .roles(allRoles)
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

    /**
     * Lấy tất cả permissions từ tất cả các role
     */
    private Set<String> getAllPermissionsFromRoles(Set<String> roleNames) {
        return roleNames.stream()
                .map(this::getPermissionsByRole)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new InvalidTokenException("Invalid refresh token");
        }
        
        String username = tokenProvider.getUsernameFromToken(refreshToken);
        Set<String> tokenRoles = tokenProvider.getRolesFromToken(refreshToken);

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException e) {
            throw new InvalidTokenException("User not found for refresh token");
        }

        User user = (User) userDetails;
        
        // Lấy tất cả role hiện tại của user từ database
        Set<String> currentRoles = userRoleRepository.findByUserUserId(user.getUserId())
                .stream()
                .map(ur -> ur.getRole().getRoleName())
                .collect(Collectors.toSet());
        
        if (currentRoles.isEmpty()) {
            throw new InvalidTokenException("User no longer has any roles assigned");
        }

        // Lấy tất cả permissions từ tất cả các role hiện tại
        Set<String> allPermissions = getAllPermissionsFromRoles(currentRoles);

        // Create new access token with all current roles and permissions
        String newAccessToken = tokenProvider.generateAccessToken(username, currentRoles, allPermissions);

        // Xác định selectedRole (ưu tiên role đầu tiên trong token cũ nếu vẫn tồn tại)
        String selectedRole = tokenRoles.stream()
                .filter(currentRoles::contains)
                .findFirst()
                .orElse(currentRoles.stream().findFirst().orElse(null));

        return AuthResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .roles(currentRoles)
                .accessToken(newAccessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(String refreshToken) {
        tokenProvider.revokeToken(refreshToken);
    }
}