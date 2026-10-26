package com.khaata.controller;

import com.khaata.dto.CreateCustomerRequest;
import com.khaata.dto.CreateTransactionRequest;
import com.khaata.dto.CustomerResponse;
import com.khaata.dto.DeleteCustomerRequest;
import com.khaata.dto.TransactionResponse;
import com.khaata.dto.UpdateCustomerRequest;
import com.khaata.exception.BusinessRuleException;
import com.khaata.service.AuthService;
import com.khaata.service.CustomerService;
import com.khaata.service.LedgerService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for customers and their ledger entries. Controllers are intentionally
 * thin: they validate input, delegate to services, and map to HTTP responses.
 */
@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;
    private final LedgerService ledgerService;
    private final AuthService authService;

    public CustomerController(CustomerService customerService, LedgerService ledgerService,
                              AuthService authService) {
        this.customerService = customerService;
        this.ledgerService = ledgerService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(req));
    }

    @GetMapping
    public List<CustomerResponse> list() {
        return customerService.listWithBalances();
    }

    @GetMapping("/{id}")
    public CustomerResponse get(@PathVariable Long id) {
        return customerService.get(id);
    }

    /** Update a customer's editable details. */
    @PutMapping("/{id}")
    public CustomerResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateCustomerRequest req) {
        return customerService.update(id, req);
    }

    /**
     * Delete a customer. Requires the owner to re-enter their own login password,
     * which is verified here before the service performs the delete.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @Valid @RequestBody DeleteCustomerRequest req,
                                       Authentication authentication) {
        String username = authentication.getName();
        if (!authService.verifyPassword(username, req.password())) {
            throw new BusinessRuleException("Incorrect password. Customer was not deleted.");
        }
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Record a CREDIT or REPAYMENT for this customer (the Add / Clear actions). */
    @PostMapping("/{id}/transactions")
    public ResponseEntity<TransactionResponse> addTransaction(
            @PathVariable Long id,
            @Valid @RequestBody CreateTransactionRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ledgerService.record(id, req));
    }

    @GetMapping("/{id}/transactions")
    public List<TransactionResponse> history(@PathVariable Long id) {
        return ledgerService.history(id);
    }

    /** Full ledger history with running balance per row — for the history panel. */
    @GetMapping("/{id}/history")
    public List<com.khaata.dto.LedgerEntryResponse> historyWithBalance(@PathVariable Long id) {
        return ledgerService.historyWithBalance(id);
    }
}
