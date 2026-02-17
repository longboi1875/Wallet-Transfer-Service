package com.gurnek.wallet.api.dto;

public record CreateUserResponse(
        Long userId,
        Long walletId
) {
}
