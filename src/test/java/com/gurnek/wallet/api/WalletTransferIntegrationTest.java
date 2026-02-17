package com.gurnek.wallet.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class WalletTransferIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateUsersDepositAndTransferWithIdempotency() throws Exception {
        Long walletA = createUser("Alex Doe", uniqueEmail("alex"));
        Long walletB = createUser("Jamie Doe", uniqueEmail("jamie"));

        mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", walletA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 200.00}
                                """))
                .andExpect(status().isOk());

        String firstTransfer = mockMvc.perform(post("/api/v1/transfers")
                        .header("X-Idempotency-Key", "tx-abc-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromWalletId": %d, "toWalletId": %d, "amount": 50.00}
                                """.formatted(walletA, walletB)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String duplicateTransfer = mockMvc.perform(post("/api/v1/transfers")
                        .header("X-Idempotency-Key", "tx-abc-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromWalletId": %d, "toWalletId": %d, "amount": 50.00}
                                """.formatted(walletA, walletB)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode first = objectMapper.readTree(firstTransfer);
        JsonNode duplicate = objectMapper.readTree(duplicateTransfer);

        assertThat(duplicate.get("transferId").asLong()).isEqualTo(first.get("transferId").asLong());

        String walletAResponse = mockMvc.perform(get("/api/v1/wallets/{walletId}", walletA))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String walletBResponse = mockMvc.perform(get("/api/v1/wallets/{walletId}", walletB))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(objectMapper.readTree(walletAResponse).get("balance").decimalValue()).isEqualByComparingTo("150.00");
        assertThat(objectMapper.readTree(walletBResponse).get("balance").decimalValue()).isEqualByComparingTo("50.00");
    }

    @Test
    void shouldRejectTransferWhenIdempotencyHeaderMissing() throws Exception {
        Long walletA = createUser("No Header A", uniqueEmail("no-header-a"));
        Long walletB = createUser("No Header B", uniqueEmail("no-header-b"));

        mockMvc.perform(post("/api/v1/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromWalletId": %d, "toWalletId": %d, "amount": 10.00}
                                """.formatted(walletA, walletB)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransferWhenInsufficientBalance() throws Exception {
        Long walletA = createUser("Low Balance A", uniqueEmail("low-balance-a"));
        Long walletB = createUser("Low Balance B", uniqueEmail("low-balance-b"));

        mockMvc.perform(post("/api/v1/transfers")
                        .header("X-Idempotency-Key", "insufficient-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromWalletId": %d, "toWalletId": %d, "amount": 10.00}
                                """.formatted(walletA, walletB)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectTransferToSameWallet() throws Exception {
        Long walletA = createUser("Same Wallet", uniqueEmail("same-wallet"));

        mockMvc.perform(post("/api/v1/transfers")
                        .header("X-Idempotency-Key", "same-wallet-" + UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromWalletId": %d, "toWalletId": %d, "amount": 10.00}
                                """.formatted(walletA, walletA)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRejectDepositWithInvalidAmount() throws Exception {
        Long wallet = createUser("Deposit User", uniqueEmail("deposit-user"));

        mockMvc.perform(post("/api/v1/wallets/{walletId}/deposit", wallet)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount": 0}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnNotFoundForUnknownWallet() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/{walletId}", 999999L))
                .andExpect(status().isNotFound());
    }

    private Long createUser(String fullName, String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName":"%s", "email":"%s"}
                                """.formatted(fullName, email)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("walletId").asLong();
    }

    private String uniqueEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }
}
