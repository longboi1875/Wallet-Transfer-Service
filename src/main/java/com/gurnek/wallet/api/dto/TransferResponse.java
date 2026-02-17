package com.gurnek.wallet.api.dto;

import com.gurnek.wallet.domain.TransferStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransferResponse(
        Long transferId,
        Long fromWalletId,
        Long toWalletId,
        BigDecimal amount,
        TransferStatus status,
        Instant createdAt
) {
}
