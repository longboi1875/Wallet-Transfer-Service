package com.gurnek.wallet.repository;

import com.gurnek.wallet.domain.TransferTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TransferTransactionRepository extends JpaRepository<TransferTransaction, Long> {
    Optional<TransferTransaction> findByIdempotencyKey(String idempotencyKey);
}
