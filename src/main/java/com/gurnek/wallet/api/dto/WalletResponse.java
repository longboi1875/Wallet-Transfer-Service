package com.gurnek.wallet.api.dto;

import java.math.BigDecimal;

public record WalletResponse(
        Long walletId,
        BigDecimal balance
) {
}
