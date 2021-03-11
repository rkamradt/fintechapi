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

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import net.kamradtfamily.fintech.fakeapi.TransferAuditReactiveRepository;
import net.kamradtfamily.fintech.fakeapi.UserReactiveRepository;
import net.kamradtfamily.fintech.fakeapi.data.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author randalkamradt
 */
public class FinTechServiceTest {
    FinTechService sut;
    UserReactiveRepository userRepository = new UserRepositoryDouble();
    TransferAuditReactiveRepository transferAuditRepository = new TransferAuditRepositoryDouble();
    List<Customer> customers;
    
    
    public FinTechServiceTest() {
    }
    
    @BeforeEach
    public void setUp() throws IOException {
        userRepository.deleteAll();
        transferAuditRepository.deleteAll();
        sut = new FinTechService(userRepository,
                            transferAuditRepository);
        sut.initializeUsers();
        customers = userRepository.findAll()
                .collectList()
                .block();
    }

    @Test
    public void testCreateAccount() {
        System.out.println("createAccount");
        AccountPayload account = AccountPayload.builder()
                .currAmount(BigDecimal.valueOf(100, 2))
                .type("checking")
                .build();
        String userId = customers.get(0).getId();
        AccountPayload result = sut.createAccount(account, userId)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of save not found"));
        assertEquals(account.getCurrAmount(),result.getCurrAmount());
        assertEquals(account.getType(),result.getType());
        assertNotNull(result.getId());
        try { // negative values not allowed
            account = AccountPayload.builder()
                    .currAmount(BigDecimal.valueOf(-100, 2))
                    .type("checking")
                    .build();
            userId = customers.get(0).getId();
            sut.createAccount(account, userId)
                    .blockOptional(Duration.ofSeconds(1))
                    .orElseThrow(() -> new RuntimeException("result of save not found"));
            fail("expected exception not thrown");
        } catch(NegativeValueNotAllowedException ex) {
            assertEquals("Negative value -1.00 not allowed here", ex.getMessage());
        }
        try { // test for invalid user id (authentication should have caught this)
            account = AccountPayload.builder()
                    .currAmount(BigDecimal.valueOf(100, 2))
                    .type("checking")
                    .build();
            userId = "0";
            sut.createAccount(account, userId)
                    .blockOptional(Duration.ofSeconds(1))
                    .orElseThrow(() -> new RuntimeException("result of save not found"));
            fail("expected exception not thrown");
        } catch(UserNotFoundException ex) {
            assertEquals("User 0 not found", ex.getMessage());
        }
    }

    @Test
    public void testGetAccount() {
        System.out.println("getAccount");
        AccountPayload account = AccountPayload.builder()
                .currAmount(BigDecimal.valueOf(100, 2))
                .type("checking")
                .build();
        String userId = customers.get(0).getId();
        account = sut.createAccount(account, userId)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of save not found"));
        AccountPayload result = sut.getAccount(account.getId(), userId)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of get not found"));
        assertEquals(account.getCurrAmount(),result.getCurrAmount());
        assertEquals(account.getType(),result.getType());
        assertNotNull(result.getId());
        try { // user can only get their own accounts
            sut.getAccount(account.getId(), customers.get(1).getId())
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of get not found"));
            fail("expected exception not thrown");
        } catch (AccountNotFoundException ex) {
            assertEquals("Account " + account.getId() + " not found for user " + customers.get(1).getId(), ex.getMessage());
        }
        try { // unknown account returns reasonable error
            sut.getAccount("0", customers.get(1).getId())
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of get not found"));
            fail("expected exception not thrown");
        } catch (AccountNotFoundException ex) {
            assertEquals("Account 0 not found for user " + customers.get(1).getId(), ex.getMessage());
        }
        try { // unknown user
            sut.getAccount("", "0")
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of get not found"));
            fail("expected exception not thrown");
        } catch (UserNotFoundException ex) {
            assertEquals("User 0 not found", ex.getMessage());
        }
    }

    @Test
    public void testTransfer() {
        System.out.println("transfer");
        AccountPayload account1 = AccountPayload.builder()
                .currAmount(BigDecimal.valueOf(1000, 2))
                .type("checking")
                .build();
        String userId1 = customers.get(0).getId();
        account1 = sut.createAccount(account1, userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of save not found"));
        AccountPayload account2 = AccountPayload.builder()
                .currAmount(BigDecimal.valueOf(1000, 2))
                .type("checking")
                .build();
        String userId2 = customers.get(1).getId();
        account2 = sut.createAccount(account2, userId2)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of save not found"));
        TransferPayload transfer = TransferPayload.builder()
                .amount(BigDecimal.TEN)
                .fromAccount(account1.getId())
                .toAccount(account2.getId())
                .userId(userId2)
                .build();
        TransferPayload result = sut.transfer(transfer, userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of transfer not found"));
        // check transfer:
        assertNotNull(result.getTransferId());
        assertEquals(account1.getId(), result.getFromAccount());
        assertEquals(account2.getId(), result.getToAccount());
        assertEquals(userId2, result.getUserId());
        // check reduction in user 1
        assertEquals(BigDecimal.valueOf(0,2), sut.getAccount(account1.getId(), userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("account 1 disappeared!"))
                .getCurrAmount());
        // check addition in user 2
        assertEquals(BigDecimal.valueOf(2000,2), sut.getAccount(account2.getId(), userId2)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("account 2 disappeared!"))
                .getCurrAmount());
        // check with accounts from same user
        account1 = AccountPayload.builder()
                .currAmount(BigDecimal.valueOf(1000, 2))
                .type("checking")
                .build();
        userId1 = customers.get(0).getId();
        account1 = sut.createAccount(account1, userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of save not found"));
        account2 = AccountPayload.builder()
                .currAmount(BigDecimal.valueOf(1000, 2))
                .type("savings")
                .build();
        account2 = sut.createAccount(account2, userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of save not found"));
        transfer = TransferPayload.builder()
                .amount(BigDecimal.TEN)
                .fromAccount(account1.getId())
                .toAccount(account2.getId())
                .userId(userId1)
                .build();
        result = sut.transfer(transfer, userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of transfer not found"));
        // check transfer:
        assertNotNull(result.getTransferId());
        assertEquals(account1.getId(), result.getFromAccount());
        assertEquals(account2.getId(), result.getToAccount());
        assertEquals(userId1, result.getUserId());
        // check reduction in user 1
        assertEquals(BigDecimal.valueOf(0,2), sut.getAccount(account1.getId(), userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("account 1 disappeared!"))
                .getCurrAmount());
        // check addition in user 2
        assertEquals(BigDecimal.valueOf(2000,2), sut.getAccount(account2.getId(), userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("account 2 disappeared!"))
                .getCurrAmount());
        try {
            transfer = TransferPayload.builder()
                    .amount(BigDecimal.TEN)
                    .fromAccount(account1.getId())
                    .toAccount(account2.getId())
                    .userId(userId1)
                    .build();
            sut.transfer(transfer, userId1)
                    .blockOptional(Duration.ofSeconds(1))
                    .orElseThrow(() -> new RuntimeException("result of transfer not found"));
            fail("unexpected exception not thrown");
        } catch(NegativeValueNotAllowedException ex) {
            assertEquals("Negative value transfer result not allowed here", ex.getMessage());
        }
    }

    @Test
    public void testGetTransfers() {
        System.out.println("getTransfers");
        AccountPayload account1 = AccountPayload.builder()
                .currAmount(BigDecimal.valueOf(1000, 2))
                .type("checking")
                .build();
        String userId1 = customers.get(0).getId();
        account1 = sut.createAccount(account1, userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of save not found"));
        AccountPayload account2 = AccountPayload.builder()
                .currAmount(BigDecimal.valueOf(1000, 2))
                .type("checking")
                .build();
        String userId2 = customers.get(1).getId();
        account2 = sut.createAccount(account2, userId2)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of save not found"));
        TransferPayload transfer = TransferPayload.builder()
                .amount(BigDecimal.TEN)
                .fromAccount(account1.getId())
                .toAccount(account2.getId())
                .userId(userId2)
                .build();
        sut.transfer(transfer, userId1)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of transfer not found"));
        transfer = TransferPayload.builder()
                .amount(BigDecimal.TEN)
                .fromAccount(account2.getId())
                .toAccount(account1.getId())
                .userId(userId1)
                .build();
        sut.transfer(transfer, userId2)
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of transfer not found"));
        List<TransferPayload> result = sut.getTransfers(account1.getId(), userId1)
                .collectList()
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of get transfers not found"));

        assertEquals(2, result.size());
        try { // can't get transfers not of another user
            sut.getTransfers(account2.getId(), userId1)
                .collectList()
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of get transfers not found"));
            fail("expected exception not thrown"); 
        } catch (AccountNotFoundException ex) {
            assertEquals("Account " + account2.getId() + " not found for user " + userId1, ex.getMessage());
        }
        try { // invalid user
            sut.getTransfers(account2.getId(), "0")
                .collectList()
                .blockOptional(Duration.ofSeconds(1))
                .orElseThrow(() -> new RuntimeException("result of get transfers not found"));
            fail("expected exception not thrown"); 
        } catch (UserNotFoundException ex) {
            assertEquals("User 0 not found", ex.getMessage());
        }
    }
    
}
