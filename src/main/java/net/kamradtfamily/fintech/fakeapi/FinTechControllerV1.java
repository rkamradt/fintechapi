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
package net.kamradtfamily.fintech.fakeapi;

import net.kamradtfamily.fintech.fakeapi.service.TransferPayload;
import net.kamradtfamily.fintech.fakeapi.service.FinTechService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.io.IOException;
import net.kamradtfamily.fintech.fakeapi.service.AccountPayload;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author randalkamradt
 */
@Slf4j
@RestController
@RequestMapping("/v1/fintech")
public class FinTechControllerV1 {
    private final static String USER_HEADER = "X-user-id";
    
    private final FinTechService service;
    
    FinTechControllerV1(final FinTechService service,
            final UserReactiveRepository newsReactiveRepository) throws IOException {
        this.service = service;
        this.service.initializeUsers();
    }
    
    @Operation(summary = "Create an account by user with initial amount")
    @ApiResponses(value = { 
      @ApiResponse(responseCode = "201", description = "Found the account", 
        content = { @Content(mediaType = "application/json", 
          schema = @Schema(implementation = AccountPayload.class)) }),
      @ApiResponse(responseCode = "400", description = "Invalid user supplied", 
        content = @Content), 
      @ApiResponse(responseCode = "404", description = "Account not found", 
        content = @Content) })
    @PostMapping(path="account")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<AccountPayload> createAccount(@RequestBody AccountPayload account, @RequestHeader(value=USER_HEADER) String userId)
    {
        return service.createAccount(account, userId);
    }
    
    @Operation(summary = "Transfer between accounts")
    @ApiResponses(value = { 
      @ApiResponse(responseCode = "201", description = "Transfer Complete", 
        content = { @Content(mediaType = "application/json", 
          schema = @Schema(implementation = TransferPayload.class)) }),
      @ApiResponse(responseCode = "400", description = "Invalid user supplied", 
        content = @Content), 
      @ApiResponse(responseCode = "404", description = "Account not found", 
        content = @Content) })
    @PostMapping(path="transfer")
    @ResponseStatus(HttpStatus.CREATED)
    Mono<TransferPayload> transfer(@RequestBody TransferPayload account, @RequestHeader(value=USER_HEADER) String userId)
    {
        return service.transfer(account, userId);
    }
    
    @Operation(summary = "Get an account information by user")
    @ApiResponses(value = { 
      @ApiResponse(responseCode = "200", description = "Found the account", 
        content = { @Content(mediaType = "application/json", 
          schema = @Schema(implementation = AccountPayload.class)) }),
      @ApiResponse(responseCode = "400", description = "Invalid user supplied", 
        content = @Content), 
      @ApiResponse(responseCode = "404", description = "Account not found", 
        content = @Content) })
    @GetMapping("/account/{accountId}")
    @ResponseStatus(HttpStatus.OK)
    Mono<AccountPayload> getAccount(@PathVariable("accountId") final String id, @RequestHeader(value=USER_HEADER) String userId) {
        return service.getAccount(id, userId);
    }
    
    @Operation(summary = "Get an account transfer information by user")
    @ApiResponses(value = { 
      @ApiResponse(responseCode = "200", description = "Found the account", 
        content = { @Content(mediaType = "application/json", 
          schema = @Schema(implementation = AccountPayload.class)) }),
      @ApiResponse(responseCode = "400", description = "Invalid user supplied", 
        content = @Content), 
      @ApiResponse(responseCode = "404", description = "Account not found", 
        content = @Content) })
    @GetMapping("/transfers/{accountId}")
    @ResponseStatus(HttpStatus.OK)
    Flux<TransferPayload> getTransfers(@PathVariable("accountId") final String accountId, @RequestHeader(value=USER_HEADER) String userId) {
        return service.getTransfers(accountId, userId);
    }


}
