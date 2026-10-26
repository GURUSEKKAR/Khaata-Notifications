package com.khaata.repository.projection;

import java.math.BigDecimal;

/**
 * Spring Data projection for the per-customer balance aggregation.
 * Lets a single GROUP BY query return outstanding + paid per customer,
 * avoiding an N+1 query storm when building the shop dashboard.
 */
public interface CustomerBalanceView {
    Long getCustomerId();
    BigDecimal getOutstanding();
    BigDecimal getPaid();
}
