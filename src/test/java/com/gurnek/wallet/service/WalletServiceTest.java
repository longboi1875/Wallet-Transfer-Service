package com.gurnek.wallet.service;

import com.gurnek.wallet.api.dto.TransferRequest;
import com.gurnek.wallet.api.dto.WalletResponse;
import com.gurnek.wallet.domain.TransferStatus;
import com.gurnek.wallet.domain.TransferTransaction;
import com.gurnek.wallet.domain.Wallet;
import com.gurnek.wallet.repository.TransferTransactionRepository;
import com.gurnek.wallet.repository.UserAccountRepository;
import com.gurnek.wallet.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransferTransactionRepository transferTransactionRepository;

    @InjectMocks
    private WalletService walletService;

    @Test
    void shouldReturnExistingTransferForDuplicateIdempotencyKey() {
        TransferTransaction tx = new TransferTransaction();
        tx.setFromWalletId(1L);
        tx.setToWalletId(2L);
        tx.setAmount(new BigDecimal("20.00"));
        tx.setStatus(TransferStatus.SUCCESS);
        tx.setIdempotencyKey("key-1");

        when(transferTransactionRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.of(tx));

        var response = walletService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("20.00")),
                "key-1"
        );

        assertThat(response.fromWalletId()).isEqualTo(1L);
        assertThat(response.toWalletId()).isEqualTo(2L);
        assertThat(response.amount()).isEqualByComparingTo("20.00");
        assertThat(response.status()).isEqualTo(TransferStatus.SUCCESS);
        assertThat(response.createdAt()).isNull();

        verify(transferTransactionRepository).findByIdempotencyKey("key-1");
        verifyNoInteractions(walletRepository);
    }

    @Test
    void shouldFailWhenTransferUsesSameWallet() {
        assertThatThrownBy(() -> walletService.transfer(
                new TransferRequest(10L, 10L, new BigDecimal("5.00")),
                "same-wallet-key"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("fromWalletId and toWalletId cannot be the same");

        verifyNoInteractions(walletRepository);
    }

    @Test
    void shouldFailWhenSourceWalletMissing() {
        when(transferTransactionRepository.findByIdempotencyKey("missing-source")).thenReturn(Optional.empty());
        when(walletRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("5.00")),
                "missing-source"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("source wallet not found");
    }

    @Test
    void shouldFailWhenInsufficientBalance() {
        Wallet source = new Wallet();
        source.setBalance(new BigDecimal("1.00"));
        Wallet destination = new Wallet();
        destination.setBalance(new BigDecimal("0.00"));

        when(transferTransactionRepository.findByIdempotencyKey("insufficient")).thenReturn(Optional.empty());
        when(walletRepository.findById(1L)).thenReturn(Optional.of(source));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(destination));

        assertThatThrownBy(() -> walletService.transfer(
                new TransferRequest(1L, 2L, new BigDecimal("2.00")),
                "insufficient"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("insufficient balance");
    }

    @Test
    void shouldTransferAndPersistWalletBalances() {
        Wallet source = new Wallet();
        source.setBalance(new BigDecimal("100.00"));
        Wallet destination = new Wallet();
        destination.setBalance(new BigDecimal("15.00"));

        TransferTransaction savedTransaction = new TransferTransaction();
        savedTransaction.setFromWalletId(1L);
        savedTransaction.setToWalletId(2L);
        savedTransaction.setAmount(new BigDecimal("20.00"));
        savedTransaction.setStatus(TransferStatus.SUCCESS);
        savedTransaction.setIdempotencyKey("ok-key");

        when(transferTransactionRepository.findByIdempotencyKey("ok-key")).thenReturn(Optional.empty());
        when(walletRepository.findById(1L)).thenReturn(Optional.of(source));
        when(walletRepository.findById(2L)).thenReturn(Optional.of(destination));
        when(transferTransactionRepository.save(any(TransferTransaction.class))).thenReturn(savedTransaction);

        var response = walletService.transfer(new TransferRequest(1L, 2L, new BigDecimal("20.00")), "ok-key");

        assertThat(source.getBalance()).isEqualByComparingTo("80.00");
        assertThat(destination.getBalance()).isEqualByComparingTo("35.00");
        assertThat(response.status()).isEqualTo(TransferStatus.SUCCESS);

        verify(walletRepository).save(source);
        verify(walletRepository).save(destination);
        verify(transferTransactionRepository).save(any(TransferTransaction.class));
    }

    @Test
    void shouldGetWalletBalance() {
        Wallet wallet = new Wallet();
        wallet.setBalance(new BigDecimal("77.00"));
        when(walletRepository.findById(25L)).thenReturn(Optional.of(wallet));

        WalletResponse response = walletService.getWallet(25L);

        assertThat(response.balance()).isEqualByComparingTo("77.00");
    }

    @Test
    void shouldFailWhenWalletNotFound() {
        when(walletRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> walletService.getWallet(404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("wallet not found");
    }
}
