package com.khaata.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.khaata.dto.CreateCustomerRequest;
import com.khaata.dto.CreateTransactionRequest;
import com.khaata.dto.CustomerResponse;
import com.khaata.entity.CustomerType;
import com.khaata.entity.TransactionType;
import com.khaata.exception.BusinessRuleException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration tests for the ledger math — the most important thing to get right.
 * Runs against the in-memory H2 DB with the dev profile disabled-seeder path
 * (we create our own customers here).
 */
@SpringBootTest
@ActiveProfiles("test")
class LedgerServiceTest {

    @Autowired
    CustomerService customerService;
    @Autowired
    LedgerService ledgerService;

    private CustomerResponse newCustomer(String phone) {
        return customerService.create(new CreateCustomerRequest(
                "Test " + phone, phone, null, CustomerType.NORMAL, 7, null));
    }

    @Test
    void balanceIsCreditMinusRepayment() {
        CustomerResponse c = newCustomer("+919111100001");
        ledgerService.record(c.id(), new CreateTransactionRequest(
                TransactionType.CREDIT, new BigDecimal("1000.00"), "goods", null));
        ledgerService.record(c.id(), new CreateTransactionRequest(
                TransactionType.REPAYMENT, new BigDecimal("300.00"), "part", null));

        assertThat(ledgerService.outstanding(c.id())).isEqualByComparingTo("700.00");
        assertThat(ledgerService.paid(c.id())).isEqualByComparingTo("300.00");
    }

    @Test
    void repaymentCannotExceedOutstanding() {
        CustomerResponse c = newCustomer("+919111100002");
        ledgerService.record(c.id(), new CreateTransactionRequest(
                TransactionType.CREDIT, new BigDecimal("500.00"), "goods", null));

        assertThatThrownBy(() -> ledgerService.record(c.id(), new CreateTransactionRequest(
                TransactionType.REPAYMENT, new BigDecimal("600.00"), "too much", null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void overdueReflectsPastDueCredit() {
        CustomerResponse c = newCustomer("+919111100003");
        ledgerService.record(c.id(), new CreateTransactionRequest(
                TransactionType.CREDIT, new BigDecimal("800.00"), "old debt",
                LocalDate.now().minusDays(3)));

        assertThat(ledgerService.overdue(c.id())).isEqualByComparingTo("800.00");
    }

    @Test
    void futureDueCreditIsNotOverdue() {
        CustomerResponse c = newCustomer("+919111100004");
        ledgerService.record(c.id(), new CreateTransactionRequest(
                TransactionType.CREDIT, new BigDecimal("800.00"), "fresh debt",
                LocalDate.now().plusDays(3)));

        assertThat(ledgerService.overdue(c.id())).isEqualByComparingTo("0");
    }
}
