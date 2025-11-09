package com.example.datn_realeaste_crm.security;


import com.example.datn_realeaste_crm.repository.UserRepository;
import com.example.datn_realeaste_crm.security.crypto.DeterministicHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    
    private final UserRepository userRepository;
    private final DeterministicHasher deterministicHasher;
    
    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Normalize email and compute hash for lookup
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        byte[] emailHash = deterministicHasher.emailHash(normalizedEmail);
        
        return userRepository.findByEmailHash(emailHash)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}