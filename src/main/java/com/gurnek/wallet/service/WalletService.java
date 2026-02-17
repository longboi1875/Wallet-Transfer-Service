package com.gurnek.wallet.service;

import com.gurnek.wallet.api.dto.CreateUserRequest;
import com.gurnek.wallet.api.dto.CreateUserResponse;
import com.gurnek.wallet.api.dto.TransferRequest;
import com.gurnek.wallet.api.dto.TransferResponse;
import com.gurnek.wallet.api.dto.WalletResponse;
import com.gurnek.wallet.domain.TransferStatus;
import com.gurnek.wallet.domain.TransferTransaction;
import com.gurnek.wallet.domain.UserAccount;
import com.gurnek.wallet.domain.Wallet;
import com.gurnek.wallet.repository.TransferTransactionRepository;
import com.gurnek.wallet.repository.UserAccountRepository;
import com.gurnek.wallet.repository.WalletRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class WalletService {

    private static final Logger log = LoggerFactory.getLogger(WalletService.class);
    private final UserAccountRepository userAccountRepository;
    private final WalletRepository walletRepository;
    private final TransferTransactionRepository transferTransactionRepository;

    public WalletService(UserAccountRepository userAccountRepository,
                         WalletRepository walletRepository,
                         TransferTransactionRepository transferTransactionRepository) {
        this.userAccountRepository = userAccountRepository;
        this.walletRepository = walletRepository;
        this.transferTransactionRepository = transferTransactionRepository;
    }

    @Transactional
    public CreateUserResponse createUserWithWallet(CreateUserRequest request) {
        log.info("Creating user and wallet for email={}", request.email());
        UserAccount user = new UserAccount();
        user.setFullName(request.fullName());
        user.setEmail(request.email());
        UserAccount savedUser = userAccountRepository.save(user);

        Wallet wallet = new Wallet();
        wallet.setUserAccount(savedUser);
        wallet.setBalance(BigDecimal.ZERO);
        Wallet savedWallet = walletRepository.save(wallet);

        log.info("Created userId={} with walletId={}", savedUser.getId(), savedWallet.getId());
        return new CreateUserResponse(savedUser.getId(), savedWallet.getId());
    }

    @Transactional
    public WalletResponse deposit(Long walletId, BigDecimal amount) {
        log.info("Deposit request walletId={} amount={}", walletId, amount);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("wallet not found"));
        wallet.setBalance(wallet.getBalance().add(amount));
        Wallet saved = walletRepository.save(wallet);
        log.info("Deposit success walletId={} newBalance={}", saved.getId(), saved.getBalance());
        return new WalletResponse(saved.getId(), saved.getBalance());
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request, String idempotencyKey) {
        log.info("Transfer request fromWalletId={} toWalletId={} amount={} idempotencyKey={}",
                request.fromWalletId(), request.toWalletId(), request.amount(), idempotencyKey);
        if (request.fromWalletId().equals(request.toWalletId())) {
            log.warn("Transfer rejected because source and destination wallets are equal: walletId={}", request.fromWalletId());
            throw new BusinessException("fromWalletId and toWalletId cannot be the same");
        }

        TransferTransaction existing = transferTransactionRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existing != null) {
            log.info("Idempotent replay detected for key={} transferId={}", idempotencyKey, existing.getId());
            return toResponse(existing);
        }

        Wallet fromWallet = walletRepository.findById(request.fromWalletId())
                .orElseThrow(() -> new NotFoundException("source wallet not found"));
        Wallet toWallet = walletRepository.findById(request.toWalletId())
                .orElseThrow(() -> new NotFoundException("destination wallet not found"));

        if (fromWallet.getBalance().compareTo(request.amount()) < 0) {
            log.warn("Transfer rejected for insufficient balance walletId={} balance={} requested={}",
                    request.fromWalletId(), fromWallet.getBalance(), request.amount());
            throw new BusinessException("insufficient balance");
        }

        fromWallet.setBalance(fromWallet.getBalance().subtract(request.amount()));
        toWallet.setBalance(toWallet.getBalance().add(request.amount()));
        walletRepository.save(fromWallet);
        walletRepository.save(toWallet);

        TransferTransaction tx = new TransferTransaction();
        tx.setFromWalletId(request.fromWalletId());
        tx.setToWalletId(request.toWalletId());
        tx.setAmount(request.amount());
        tx.setStatus(TransferStatus.SUCCESS);
        tx.setIdempotencyKey(idempotencyKey);
        TransferTransaction saved = transferTransactionRepository.save(tx);
        log.info("Transfer success transferId={} fromWalletId={} toWalletId={} amount={}",
                saved.getId(), saved.getFromWalletId(), saved.getToWalletId(), saved.getAmount());
        return toResponse(saved);
    }

    public TransferResponse getTransfer(Long transferId) {
        log.info("Fetching transfer transferId={}", transferId);
        TransferTransaction tx = transferTransactionRepository.findById(transferId)
                .orElseThrow(() -> new NotFoundException("transfer not found"));
        return toResponse(tx);
    }

    public WalletResponse getWallet(Long walletId) {
        log.info("Fetching wallet walletId={}", walletId);
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new NotFoundException("wallet not found"));
        return new WalletResponse(wallet.getId(), wallet.getBalance());
    }

    private TransferResponse toResponse(TransferTransaction tx) {
        return new TransferResponse(
                tx.getId(),
                tx.getFromWalletId(),
                tx.getToWalletId(),
                tx.getAmount(),
                tx.getStatus(),
                tx.getCreatedAt()
        );
    }
}
