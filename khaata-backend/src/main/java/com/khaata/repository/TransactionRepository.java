package com.khaata.repository;

import com.khaata.entity.LedgerTransaction;
import com.khaata.repository.projection.CustomerBalanceView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Data access for the append-only ledger. The aggregation queries here are the
 * "read model" — they derive balances from the raw event log (a light-touch
 * CQRS: the write model is the event rows, the read model is these aggregates).
 */
public interface TransactionRepository extends JpaRepository<LedgerTransaction, Long> {

    List<LedgerTransaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    void deleteByCustomerId(Long customerId);

    /** Outstanding for one customer: SUM(CREDIT) - SUM(REPAYMENT). */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN t.type = com.khaata.entity.TransactionType.CREDIT
                                     THEN t.amount ELSE -t.amount END), 0)
            FROM LedgerTransaction t
            WHERE t.customerId = :customerId
            """)
    BigDecimal outstandingForCustomer(@Param("customerId") Long customerId);

    /** Total repaid by one customer. */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM LedgerTransaction t
            WHERE t.customerId = :customerId
              AND t.type = com.khaata.entity.TransactionType.REPAYMENT
            """)
    BigDecimal paidForCustomer(@Param("customerId") Long customerId);

    /** Shop-wide total still outstanding across every customer. */
    @Query("""
            SELECT COALESCE(SUM(CASE WHEN t.type = com.khaata.entity.TransactionType.CREDIT
                                     THEN t.amount ELSE -t.amount END), 0)
            FROM LedgerTransaction t
            """)
    BigDecimal totalOutstanding();

    /** Shop-wide total ever repaid. */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM LedgerTransaction t
            WHERE t.type = com.khaata.entity.TransactionType.REPAYMENT
            """)
    BigDecimal totalPaid();

    /** Shop-wide total credit ever given. */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM LedgerTransaction t
            WHERE t.type = com.khaata.entity.TransactionType.CREDIT
            """)
    BigDecimal totalCredit();

    /**
     * Per-customer outstanding, computed in one query (avoids N+1 when building
     * the customer list / shop summary). Returns only customers who have activity.
     */
    @Query("""
            SELECT t.customerId AS customerId,
                   COALESCE(SUM(CASE WHEN t.type = com.khaata.entity.TransactionType.CREDIT
                                     THEN t.amount ELSE -t.amount END), 0) AS outstanding,
                   COALESCE(SUM(CASE WHEN t.type = com.khaata.entity.TransactionType.REPAYMENT
                                     THEN t.amount ELSE 0 END), 0) AS paid
            FROM LedgerTransaction t
            GROUP BY t.customerId
            """)
    List<CustomerBalanceView> balancesPerCustomer();

    /**
     * Overdue amount for a customer: the sum of CREDIT whose due_date has passed,
     * capped at the current outstanding (you can't be overdue for more than you owe).
     * The capping is applied in the service layer; here we return the raw overdue credit.
     */
    @Query("""
            SELECT COALESCE(SUM(t.amount), 0)
            FROM LedgerTransaction t
            WHERE t.customerId = :customerId
              AND t.type = com.khaata.entity.TransactionType.CREDIT
              AND t.dueDate IS NOT NULL
              AND t.dueDate < :today
            """)
    BigDecimal overdueCreditForCustomer(@Param("customerId") Long customerId,
                                        @Param("today") LocalDate today);
}
