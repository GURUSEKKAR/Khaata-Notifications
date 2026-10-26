package com.khaata.repository;

import com.khaata.entity.ReminderLog;
import com.khaata.entity.ReminderStatus;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link ReminderLog}. */
public interface ReminderLogRepository extends JpaRepository<ReminderLog, Long> {

    /**
     * Idempotency guard: has this customer already been successfully reminded
     * (or deliberately skipped) on the given day?
     */
    boolean existsByCustomerIdAndSentOnAndStatusIn(
            Long customerId, LocalDate sentOn, List<ReminderStatus> statuses);

    List<ReminderLog> findTop100ByOrderByCreatedAtDesc();

    List<ReminderLog> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    void deleteByCustomerId(Long customerId);
}
