/*
 * The MIT License
 *
 * Copyright 2021 randalkamradt.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.kamradtfamily.fintech.fakeapi.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import net.kamradtfamily.fintech.fakeapi.TransferAuditReactiveRepository;
import net.kamradtfamily.fintech.fakeapi.data.Customer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import net.kamradtfamily.fintech.fakeapi.UserReactiveRepository;
import net.kamradtfamily.fintech.fakeapi.data.Customer.Account;
import net.kamradtfamily.fintech.fakeapi.data.TransferAudit;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 *
 * Service to allow creating new accounts, transferring, getting account
 * balances and transfer lists. Assumes pre-loaded users
 * 
 * @author randalkamradt
 */
@Component
public class FinTechService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UserReactiveRepository userRepository;
    private final TransferAuditReactiveRepository transferAuditRepository;
    
    /**
     *
     * Create a new FinTechService with auto wired objects
     * 
     * @param userRepository database interface for the user repository
     * @param transferAuditRepository database interface for transfer audit repository
     */
    public FinTechService(
            final UserReactiveRepository userRepository,
            final TransferAuditReactiveRepository transferAuditRepository
    ) {
        this.userRepository = userRepository;
        this.transferAuditRepository = transferAuditRepository;
    }

    /**
     *
     * Create a new account for a user with an initial value. The initial
     * value cannot be negative
     * 
     * @param account the values to create the account with
     * @param userId the user id
     * @return the account payload with the new account id filled in
     */
    public Mono<AccountPayload> createAccount(AccountPayload account, String userId) {
        if(account.getCurrAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeValueNotAllowedException(account.getCurrAmount().toPlainString());
        }
        String accountId = UUID.randomUUID().toString();
        return userRepository.saveAll(userRepository.findById(userId)
                    .switchIfEmpty(Mono.error(() -> new UserNotFoundException(userId)))
                    .map(c -> {
                        c.getAccounts().add(Customer.Account.builder()
                            .currAmount(account.getCurrAmount())
                            .id(accountId)
                            .type(account.getType())
                            .build());
                        return c;
                    }))
                .singleOrEmpty()
                .switchIfEmpty(Mono.error(() -> new RuntimeException("empty return from save")))
                .map(c -> c.getAccounts()
                        .stream()
                        .filter(a -> a.getId().equals(accountId))
                        .findAny()
                        .orElseThrow(() -> new RuntimeException("account not added")))
                .map(a -> AccountPayload.builder()
                        .currAmount(a.getCurrAmount())
                        .id(a.getId())
                        .type(a.getType())
                        .build());
                
    }

    /**
     *
     * Get account details based on the account id. Can only be used on 
     * accounts owned by the user specified
     * 
     * @param accountId the account id to look up
     * @param userId the user the account belongs to
     * @return the account details including the current balance
     */
    public Mono<AccountPayload> getAccount(String accountId, String userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(() -> new UserNotFoundException(userId)))
                .map(c -> c.getAccounts()
                        .stream()
                        .filter(a -> a.getId().equals(accountId))
                        .findAny()
                        .orElseThrow(() -> new AccountNotFoundException(accountId, userId)))
                .map(a -> AccountPayload.builder()
                        .currAmount(a.getCurrAmount())
                        .id(a.getId())
                        .type(a.getType())
                        .build());
    }

    /**
     * 
     * read in a list of bootstrapped users
     * 
     * @throws IOException
     */
    public void initializeUsers() throws IOException {
        userRepository.saveAll(Flux.fromArray(
            objectMapper.readValue(FinTechService.class.getResourceAsStream("/initcust.json"), Customer[].class)))
                .collectList()
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("Unable to read initcust.json"));
    }

    /**
     *
     * Transfer money from one account to another. The amount to transfer cannot
     * be negative, the from account must belong to the user and the result 
     * cannot cause a negative balance
     * 
     * @param transferPayload The information about this transfer
     * @param userId the user initiating the transfer
     * @return the original transfer payload
     */
    public Mono<TransferPayload> transfer(TransferPayload transferPayload, String userId) {
        if(transferPayload.getAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeValueNotAllowedException(transferPayload.getAmount().toPlainString());
        }
        return Flux.combineLatest(findAccount(transferPayload.getFromAccount(),userId),
                 findAccount(transferPayload.getToAccount(), transferPayload.getUserId()),
                (accountFrom, accountTo) -> 
                        updateAccounts(accountFrom, 
                                accountTo, transferPayload))
                .flatMap(a -> a)
                .singleOrEmpty()
                .switchIfEmpty(Mono.error(() -> new RuntimeException("unable to complete transfer")));
    }
    
    private Mono<Tuple2<Customer, Account>> findAccount(String accountId, String userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(() -> new UserNotFoundException(userId)))
                .map(c -> Tuples.of(c, c.getAccounts()
                        .stream()
                        .filter(a -> a.getId().equals(accountId))
                        .findAny()
                        .orElseThrow(() -> new AccountNotFoundException(accountId, userId))));       
    }
    
    private Mono<TransferPayload> updateAccounts(Tuple2<Customer, Account> accountFrom, 
            Tuple2<Customer, Account> accountTo,
            TransferPayload transferPayload) {
        Account newAccountFrom = Account.builder()
                .currAmount(accountFrom.getT2().getCurrAmount().subtract(transferPayload.getAmount()))
                .id(accountFrom.getT2().getId())
                .type(accountFrom.getT2().getType())
                .build();
        if(newAccountFrom.getCurrAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new NegativeValueNotAllowedException("transfer result");
        }
        Account newAccountTo = Account.builder()
                .currAmount(accountTo.getT2().getCurrAmount().add(transferPayload.getAmount()))
                .id(accountTo.getT2().getId())
                .type(accountTo.getT2().getType())
                .build();
        accountFrom.getT1().getAccounts().removeIf(a -> a.getId().equals(newAccountFrom.getId()));
        accountFrom.getT1().getAccounts().add(newAccountFrom);
        accountTo.getT1().getAccounts().removeIf(a -> a.getId().equals(newAccountTo.getId()));
        accountTo.getT1().getAccounts().add(newAccountTo);
        return userRepository.saveAll(Flux.just(accountTo.getT1(), accountFrom.getT1()))
                .all(c -> !c.getId().isEmpty())
                .flatMap(c -> transferAuditRepository.save(TransferAudit.builder()
                        .amount(transferPayload.getAmount())
                        .fromAccount(transferPayload.getFromAccount())
                        .toAccount(transferPayload.getToAccount())
                        .toUserId(transferPayload.getUserId())
                        .build()))
                .map(s -> TransferPayload.builder()
                        .amount(s.getAmount())
                        .fromAccount(s.getFromAccount())
                        .toAccount(s.getToAccount())
                        .userId(s.getToUserId())
                        .transferId(s.getId())
                        .build());
    }

    /**
     *
     * Get a list of transfer to/from the account. the account must belong
     * to the user
     * 
     * @param accountId the account to list
     * @param userId the user the account belongs to
     * @return a list of transfer payloads
     */
    public Flux<TransferPayload> getTransfers(String accountId, String userId) {
        return userRepository.findById(userId)
                .switchIfEmpty(Mono.error(() -> new UserNotFoundException(userId)))
                .map(c -> c.getAccounts()
                        .stream()
                        .filter(a -> a.getId().equals(accountId))
                        .findAny()
                        .orElseThrow(() -> new AccountNotFoundException(accountId, userId)))
                .thenMany(transferAuditRepository.findAll())
                .filter(s -> s.getFromAccount().equals(accountId) || s.getToAccount().equals(accountId))
                .map(s -> TransferPayload.builder()
                        .amount(s.getAmount())
                        .fromAccount(s.getFromAccount())
                        .toAccount(s.getToAccount())
                        .userId(s.getToUserId())
                        .transferId(s.getId())
                        .build());

    }
    
}
