package com.khaata.service;

import com.khaata.dto.ProfileResponse;
import com.khaata.dto.UpdateProfileRequest;
import com.khaata.entity.AppUser;
import com.khaata.entity.Customer;
import com.khaata.exception.BusinessRuleException;
import com.khaata.repository.AppUserRepository;
import com.khaata.repository.CustomerRepository;
import com.khaata.repository.ReminderLogRepository;
import com.khaata.repository.TransactionRepository;
import com.khaata.security.CurrentUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Owner profile management: view, update (phone/email/UPI), and permanently delete
 * the account along with ALL of its data (customers, ledger, reminder logs).
 */
@Service
public class ProfileService {

    private final AppUserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final ReminderLogRepository reminderLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUser currentUser;

    public ProfileService(AppUserRepository userRepository,
                          CustomerRepository customerRepository,
                          TransactionRepository transactionRepository,
                          ReminderLogRepository reminderLogRepository,
                          PasswordEncoder passwordEncoder,
                          CurrentUser currentUser) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
        this.reminderLogRepository = reminderLogRepository;
        this.passwordEncoder = passwordEncoder;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public ProfileResponse get() {
        AppUser u = currentUser.get();
        return new ProfileResponse(u.getShopName(), u.getOwnerName(), u.getPhone(), u.getEmail(), u.getUpiId());
    }

    @Transactional
    public ProfileResponse update(UpdateProfileRequest req) {
        AppUser u = currentUser.get();
        u.setShopName(req.shopName().trim());
        u.setOwnerName(req.ownerName().trim());
        u.setDisplayName(req.shopName().trim());
        u.setPhone(blankToNull(req.phone()));
        u.setEmail(blankToNull(req.email()));
        u.setUpiId(blankToNull(req.upiId()));
        userRepository.save(u);
        return new ProfileResponse(u.getShopName(), u.getOwnerName(), u.getPhone(), u.getEmail(), u.getUpiId());
    }

    /**
     * Permanently delete the owner's account and everything they own. Requires the
     * owner's password. Removes dependent rows (reminder logs, transactions, customers)
     * before deleting the account itself.
     */
    @Transactional
    public void deleteAccount(String rawPassword) {
        AppUser u = currentUser.get();
        if (!passwordEncoder.matches(rawPassword, u.getPasswordHash())) {
            throw new BusinessRuleException("Incorrect password. Account was not deleted.");
        }
        for (Customer c : customerRepository.findByOwnerId(u.getId())) {
            reminderLogRepository.deleteByCustomerId(c.getId());
            transactionRepository.deleteByCustomerId(c.getId());
            customerRepository.delete(c);
        }
        userRepository.delete(u);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
