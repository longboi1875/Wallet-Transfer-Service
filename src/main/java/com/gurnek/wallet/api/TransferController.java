package com.gurnek.wallet.api;

import com.gurnek.wallet.api.dto.TransferRequest;
import com.gurnek.wallet.api.dto.TransferResponse;
import com.gurnek.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private static final String IDEMPOTENCY_HEADER = "X-Idempotency-Key";
    private final WalletService walletService;

    public TransferController(WalletService walletService) {
        this.walletService = walletService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferResponse transfer(@Valid @RequestBody TransferRequest request,
                                     @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey) {
        return walletService.transfer(request, idempotencyKey);
    }

    @GetMapping("/{transferId}")
    public TransferResponse getTransfer(@PathVariable Long transferId) {
        return walletService.getTransfer(transferId);
    }
}
