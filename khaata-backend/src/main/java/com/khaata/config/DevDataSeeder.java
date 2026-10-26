package com.khaata.config;

import com.khaata.dto.CreateCustomerRequest;
import com.khaata.dto.CreateTransactionRequest;
import com.khaata.entity.CustomerType;
import com.khaata.entity.TransactionType;
import com.khaata.repository.CustomerRepository;
import com.khaata.service.CustomerService;
import com.khaata.service.LedgerService;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Optionally seeds a few sample customers/ledger entries (Guru, Gopi, Shitij,
 * Karthik). Disabled by default — enable with {@code khaata.seed-demo-data=true}
 * if you want the sample data for a demo. Runs only when the DB is empty.
 */
@Slf4j
@Component
@Profile({"dev", "devpg"})
@ConditionalOnProperty(name = "khaata.seed-demo-data", havingValue = "true")
public class DevDataSeeder implements CommandLineRunner {

    private final CustomerService customerService;
    private final LedgerService ledgerService;
    private final CustomerRepository customerRepository;

    public DevDataSeeder(CustomerService customerService, LedgerService ledgerService,
                         CustomerRepository customerRepository) {
        this.customerService = customerService;
        this.ledgerService = ledgerService;
        this.customerRepository = customerRepository;
    }

    @Override
    public void run(String... args) {
        if (customerRepository.count() > 0) {
            return;
        }
        log.info("Seeding dev data...");

        // Guru: regular customer, overdue credit (due date in the past).
        var guru = customerService.create(new CreateCustomerRequest(
                "Guru", "+919000000001", "guru@example.com", CustomerType.REGULAR, 30, null));
        ledgerService.record(guru.id(), new CreateTransactionRequest(
                TransactionType.CREDIT, new BigDecimal("1000.00"), "Rice and dal",
                LocalDate.now().minusDays(5)));
        ledgerService.record(guru.id(), new CreateTransactionRequest(
                TransactionType.REPAYMENT, new BigDecimal("400.00"), "Part payment", null));

        // Gopi: normal customer, current (not yet overdue).
        var gopi = customerService.create(new CreateCustomerRequest(
                "Gopi", "+919000000002", null, CustomerType.NORMAL, 7, null));
        ledgerService.record(gopi.id(), new CreateTransactionRequest(
                TransactionType.CREDIT, new BigDecimal("500.00"), "Oil and sugar",
                LocalDate.now().plusDays(3)));

        // Shitij: large overdue balance.
        var shitij = customerService.create(new CreateCustomerRequest(
                "Shitij", "+919000000003", "shitij@example.com", CustomerType.NORMAL, 7, null));
        ledgerService.record(shitij.id(), new CreateTransactionRequest(
                TransactionType.CREDIT, new BigDecimal("10000.00"), "Monthly groceries",
                LocalDate.now().minusDays(10)));
        ledgerService.record(shitij.id(), new CreateTransactionRequest(
                TransactionType.REPAYMENT, new BigDecimal("5000.00"), "Half cleared", null));

        // Karthik: fully paid off (zero balance).
        var karthik = customerService.create(new CreateCustomerRequest(
                "Karthik", "+919000000004", null, CustomerType.REGULAR, 15, null));
        ledgerService.record(karthik.id(), new CreateTransactionRequest(
                TransactionType.CREDIT, new BigDecimal("2000.00"), "Snacks", LocalDate.now().minusDays(2)));
        ledgerService.record(karthik.id(), new CreateTransactionRequest(
                TransactionType.REPAYMENT, new BigDecimal("2000.00"), "Fully paid", null));

        log.info("Dev data seeded: {} customers", customerRepository.count());
    }
}
