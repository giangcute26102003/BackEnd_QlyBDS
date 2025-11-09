package com.example.datn_realeaste_crm.service;

import com.example.datn_realeaste_crm.entity.Customer;
import com.example.datn_realeaste_crm.entity.User;
import com.example.datn_realeaste_crm.repository.CustomerRepository;
import com.example.datn_realeaste_crm.repository.UserRepository;
import com.example.datn_realeaste_crm.security.crypto.DeterministicHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for backfilling encrypted columns from plaintext columns
 * This is a one-time migration service to populate encrypted fields
 * 
 * WARNING: This assumes old plaintext columns still exist in DB
 * After backfill is complete and verified, old columns can be dropped
 * 
 * To run backfill:
 * 1. Deploy code with new encrypted columns (V7 migration)
 * 2. Call backfillAllUsers() and backfillAllCustomers() via admin endpoint or @PostConstruct
 * 3. Verify data integrity
 * 4. Deploy V8 migration to drop old plaintext columns
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EncryptionBackfillService {

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final DeterministicHasher deterministicHasher;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Backfill all users in batches
     * NOTE: This method assumes old columns (email, phone_number, address) still exist temporarily
     */
    @Transactional
    public void backfillAllUsers() {
        log.info("Starting User encryption backfill...");
        int batchSize = 100;
        int page = 0;
        int totalProcessed = 0;

        Page<User> userPage;
        do {
            userPage = userRepository.findAll(PageRequest.of(page, batchSize));

            for (User user : userPage.getContent()) {
                try {
                    backfillUser(user);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Failed to backfill user ID: {}", user.getUserId(), e);
                }
            }

            log.info("Backfilled {} users (batch {})", totalProcessed, page + 1);
            page++;
        } while (userPage.hasNext());

        log.info("User encryption backfill completed. Total processed: {}", totalProcessed);
    }

    /**
     * Backfill all customers in batches
     */
    @Transactional
    public void backfillAllCustomers() {
        log.info("Starting Customer encryption backfill...");
        int batchSize = 100;
        int page = 0;
        int totalProcessed = 0;

        Page<Customer> customerPage;
        do {
            customerPage = customerRepository.findAll(PageRequest.of(page, batchSize));

            for (Customer customer : customerPage.getContent()) {
                try {
                    backfillCustomer(customer);
                    totalProcessed++;
                } catch (Exception e) {
                    log.error("Failed to backfill customer ID: {}", customer.getCustomerId(), e);
                }
            }

            log.info("Backfilled {} customers (batch {})", totalProcessed, page + 1);
            page++;
        } while (customerPage.hasNext());

        log.info("Customer encryption backfill completed. Total processed: {}", totalProcessed);
    }

    private void backfillUser(User user) {
        boolean needsUpdate = false;

        // Read from old plaintext columns directly via native query
        String oldEmail = getOldEmailFromUser(user.getUserId());
        String oldPhone = getOldPhoneFromUser(user.getUserId());
        String oldAddress = getOldAddressFromUser(user.getUserId());

        // Email
        if (oldEmail != null && user.getEmailHash() == null) {
            String normalizedEmail = normalizeEmail(oldEmail);
            user.setEmail(normalizedEmail); // Re-set to trigger encryption
            user.setEmailHash(deterministicHasher.emailHash(normalizedEmail));
            needsUpdate = true;
        }

        // Phone
        if (oldPhone != null && user.getPhoneHash() == null) {
            String normalizedPhone = normalizePhone(oldPhone);
            user.setPhoneNumber(normalizedPhone); // Re-set to trigger encryption
            user.setPhoneHash(deterministicHasher.phoneHash(normalizedPhone));
            needsUpdate = true;
        }

        // Address (no hash needed, just encryption)
        if (oldAddress != null) {
            user.setAddress(oldAddress); // Re-set to trigger encryption
            needsUpdate = true;
        }

        if (needsUpdate) {
            userRepository.save(user);
        }
    }

    private void backfillCustomer(Customer customer) {
        boolean needsUpdate = false;

        // Read from old plaintext columns directly via native query
        String oldEmail = getOldEmailFromCustomer(customer.getCustomerId());
        String oldPhone = getOldPhoneFromCustomer(customer.getCustomerId());
        String oldAddress = getOldAddressFromCustomer(customer.getCustomerId());

        // Email
        if (oldEmail != null && customer.getEmailHash() == null) {
            String normalizedEmail = normalizeEmail(oldEmail);
            customer.setEmail(normalizedEmail);
            customer.setEmailHash(deterministicHasher.emailHash(normalizedEmail));
            needsUpdate = true;
        }

        // Phone
        if (oldPhone != null && customer.getPhoneHash() == null) {
            String normalizedPhone = normalizePhone(oldPhone);
            customer.setPhoneNumber(normalizedPhone);
            customer.setPhoneHash(deterministicHasher.phoneHash(normalizedPhone));
            needsUpdate = true;
        }

        // Address
        if (oldAddress != null) {
            customer.setAddress(oldAddress);
            needsUpdate = true;
        }

        if (needsUpdate) {
            customerRepository.save(customer);
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("\\D", "");
    }

    // Methods to read from old plaintext columns
    private String getOldEmailFromUser(Integer userId) {
        try {
            String sql = "SELECT email FROM user WHERE user_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, userId);
        } catch (Exception e) {
            log.debug("No old email found for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private String getOldPhoneFromUser(Integer userId) {
        try {
            String sql = "SELECT phone_number FROM user WHERE user_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, userId);
        } catch (Exception e) {
            log.debug("No old phone found for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private String getOldAddressFromUser(Integer userId) {
        try {
            String sql = "SELECT address FROM user WHERE user_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, userId);
        } catch (Exception e) {
            log.debug("No old address found for user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    private String getOldEmailFromCustomer(Integer customerId) {
        try {
            String sql = "SELECT email FROM customer WHERE customer_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, customerId);
        } catch (Exception e) {
            log.debug("No old email found for customer {}: {}", customerId, e.getMessage());
            return null;
        }
    }

    private String getOldPhoneFromCustomer(Integer customerId) {
        try {
            String sql = "SELECT phone_number FROM customer WHERE customer_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, customerId);
        } catch (Exception e) {
            log.debug("No old phone found for customer {}: {}", customerId, e.getMessage());
            return null;
        }
    }

    private String getOldAddressFromCustomer(Integer customerId) {
        try {
            String sql = "SELECT address FROM customer WHERE customer_id = ?";
            return jdbcTemplate.queryForObject(sql, String.class, customerId);
        } catch (Exception e) {
            log.debug("No old address found for customer {}: {}", customerId, e.getMessage());
            return null;
        }
    }

    /**
     * Get backfill status - count records that still need migration
     */
    public Map<String, Object> getBackfillStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // Count users without encrypted data
            long totalUsers = userRepository.count();
            long usersWithEmailHash = userRepository.findAll().stream()
                    .mapToLong(user -> user.getEmailHash() != null ? 1 : 0)
                    .sum();
            long usersWithPhoneHash = userRepository.findAll().stream()
                    .mapToLong(user -> user.getPhoneHash() != null ? 1 : 0)
                    .sum();

            // Count customers without encrypted data
            long totalCustomers = customerRepository.count();
            long customersWithEmailHash = customerRepository.findAll().stream()
                    .mapToLong(customer -> customer.getEmailHash() != null ? 1 : 0)
                    .sum();
            long customersWithPhoneHash = customerRepository.findAll().stream()
                    .mapToLong(customer -> customer.getPhoneHash() != null ? 1 : 0)
                    .sum();

            status.put("users", Map.of(
                    "total", totalUsers,
                    "with_email_hash", usersWithEmailHash,
                    "with_phone_hash", usersWithPhoneHash,
                    "email_backfill_needed", totalUsers - usersWithEmailHash,
                    "phone_backfill_needed", totalUsers - usersWithPhoneHash,
                    "email_completion_percentage", totalUsers > 0 ? (usersWithEmailHash * 100.0 / totalUsers) : 0,
                    "phone_completion_percentage", totalUsers > 0 ? (usersWithPhoneHash * 100.0 / totalUsers) : 0
            ));

            status.put("customers", Map.of(
                    "total", totalCustomers,
                    "with_email_hash", customersWithEmailHash,
                    "with_phone_hash", customersWithPhoneHash,
                    "email_backfill_needed", totalCustomers - customersWithEmailHash,
                    "phone_backfill_needed", totalCustomers - customersWithPhoneHash,
                    "email_completion_percentage", totalCustomers > 0 ? (customersWithEmailHash * 100.0 / totalCustomers) : 0,
                    "phone_completion_percentage", totalCustomers > 0 ? (customersWithPhoneHash * 100.0 / totalCustomers) : 0
            ));

            // Overall status
            boolean usersComplete = usersWithEmailHash == totalUsers && usersWithPhoneHash == totalUsers;
            boolean customersComplete = customersWithEmailHash == totalCustomers && customersWithPhoneHash == totalCustomers;
            boolean allComplete = usersComplete && customersComplete;

            status.put("overall", Map.of(
                    "all_complete", allComplete,
                    "users_complete", usersComplete,
                    "customers_complete", customersComplete,
                    "timestamp", LocalDateTime.now()
            ));

            log.info("Backfill status: Users {}/{}, Customers {}/{}",
                    usersWithEmailHash, totalUsers, customersWithEmailHash, totalCustomers);

        } catch (Exception e) {
            log.error("Error getting backfill status", e);
            status.put("error", "Failed to get backfill status: " + e.getMessage());
        }

        return status;
    }
}

