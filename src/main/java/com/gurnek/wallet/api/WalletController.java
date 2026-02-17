package com.gurnek.wallet.api;

import com.gurnek.wallet.api.dto.CreateUserRequest;
import com.gurnek.wallet.api.dto.CreateUserResponse;
import com.gurnek.wallet.api.dto.DepositRequest;
import com.gurnek.wallet.api.dto.WalletResponse;
import com.gurnek.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return walletService.createUserWithWallet(request);
    }

    @PostMapping("/wallets/{walletId}/deposit")
    public WalletResponse deposit(@PathVariable Long walletId, @Valid @RequestBody DepositRequest request) {
        return walletService.deposit(walletId, request.amount());
    }

    @GetMapping("/wallets/{walletId}")
    public WalletResponse getWallet(@PathVariable Long walletId) {
        return walletService.getWallet(walletId);
    }
}
