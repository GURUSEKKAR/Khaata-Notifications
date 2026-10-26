package com.khaata.repository;

import com.khaata.entity.Customer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Data access for {@link Customer}. All reads are scoped to one owner. */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    List<Customer> findByOwnerId(Long ownerId);

    Optional<Customer> findByIdAndOwnerId(Long id, Long ownerId);

    boolean existsByOwnerIdAndPhone(Long ownerId, String phone);

    long countByOwnerId(Long ownerId);
}
