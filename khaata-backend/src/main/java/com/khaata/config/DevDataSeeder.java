package com.khaata.config;

import com.khaata.dto.CreateCustomerRequest;
import com.khaata.dto.CreateTransactionRequest;
import com.khaata.entity.AppUser;
import com.khaata.entity.CustomerType;
import com.khaata.entity.TransactionType;
import com.khaata.repository.AppUserRepository;
import com.khaata.repository.CustomerRepository;
import com.khaata.service.CustomerService;
import com.khaata.service.LedgerService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Optionally seeds sample data for the seeded owner. Disabled by default — enable
 * with {@code khaata.seed-demo-data=true}. Runs after the OwnerSeeder (@Order) and
 * authenticates as the owner so the owner-scoped services accept the writes.
 */
@Slf4j
@Component
@Profile({"dev", "devpg"})
@ConditionalOnProperty(name = "khaata.seed-demo-data", havingValue = "true")
@Order(1)
public class DevDataSeeder implements CommandLineRunner {

    private final CustomerService customerService;
    private final LedgerService ledgerService;
    private final CustomerRepository customerRepository;
    private final AppUserRepository userRepository;

    public DevDataSeeder(CustomerService customerService, LedgerService ledgerService,
                         CustomerRepository customerRepository, AppUserRepository userRepository) {
        this.customerService = customerService;
        this.ledgerService = ledgerService;
        this.customerRepository = customerRepository;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }
        AppUser owner = userRepository.findAll().stream().findFirst().orElse(null);
        if (owner == null) {
            log.warn("No owner account to seed demo data against; skipping.");
            return;
        }

        // Authenticate as the owner so owner-scoped services accept the writes.
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(owner.getUsername(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_OWNER"))));
        try {
            log.info("Seeding dev data for owner '{}'...", owner.getUsername());

            var guru = customerService.create(new CreateCustomerRequest(
                    "Guru", "+919000000001", "guru@example.com", CustomerType.REGULAR, 30, null));
            ledgerService.record(guru.id(), new CreateTransactionRequest(
                    TransactionType.CREDIT, new BigDecimal("1000.00"), "Rice and dal", LocalDate.now().minusDays(5)));
            ledgerService.record(guru.id(), new CreateTransactionRequest(
                    TransactionType.REPAYMENT, new BigDecimal("400.00"), "Part payment", null));

            var gopi = customerService.create(new CreateCustomerRequest(
                    "Gopi", "+919000000002", null, CustomerType.NORMAL, 7, null));
            ledgerService.record(gopi.id(), new CreateTransactionRequest(
                    TransactionType.CREDIT, new BigDecimal("500.00"), "Oil and sugar", LocalDate.now().plusDays(3)));

            var shitij = customerService.create(new CreateCustomerRequest(
                    "Shitij", "+919000000003", "shitij@example.com", CustomerType.NORMAL, 7, null));
            ledgerService.record(shitij.id(), new CreateTransactionRequest(
                    TransactionType.CREDIT, new BigDecimal("10000.00"), "Monthly groceries", LocalDate.now().minusDays(10)));
            ledgerService.record(shitij.id(), new CreateTransactionRequest(
                    TransactionType.REPAYMENT, new BigDecimal("5000.00"), "Half cleared", null));

            var karthik = customerService.create(new CreateCustomerRequest(
                    "Karthik", "+919000000004", null, CustomerType.REGULAR, 15, null));
            ledgerService.record(karthik.id(), new CreateTransactionRequest(
                    TransactionType.CREDIT, new BigDecimal("2000.00"), "Snacks", LocalDate.now().minusDays(2)));
            ledgerService.record(karthik.id(), new CreateTransactionRequest(
                    TransactionType.REPAYMENT, new BigDecimal("2000.00"), "Fully paid", null));

            log.info("Dev data seeded: {} customers", customerRepository.count());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
