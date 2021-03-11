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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.kamradtfamily.fintech.fakeapi.TransferAuditReactiveRepository;
import net.kamradtfamily.fintech.fakeapi.data.TransferAudit;
import org.reactivestreams.Publisher;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Sort;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 *
 * @author randalkamradt
 */
public class TransferAuditRepositoryDouble implements TransferAuditReactiveRepository {
    private final Map<String, TransferAudit> data = new HashMap<>();
    @Override
    public  Mono<TransferAudit> insert(TransferAudit s) {
        TransferAudit s1 = addId(s);
        data.put(s1.getId(),s1);
        return Mono.just(s1);
    }

    @Override
    public Flux<TransferAudit> findAll(Sort sort) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mono<TransferAudit> save(TransferAudit s) {
        TransferAudit s1 = addId(s);
        data.put(s1.getId(),s1);
        return Mono.just(s1);
    }

    @Override
    public Mono<TransferAudit> findById(String id) {
        return Mono.just(data.get(id));
    }

    @Override
    public Mono<TransferAudit> findById(Publisher<String> pblshr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mono<Boolean> existsById(String id) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mono<Boolean> existsById(Publisher<String> pblshr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Flux<TransferAudit> findAll() {
        return Flux.fromStream(data.values().stream());
    }

    @Override
    public Flux<TransferAudit> findAllById(Iterable<String> itrbl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Flux<TransferAudit> findAllById(Publisher<String> pblshr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mono<Long> count() {
        return Mono.just(Long.valueOf(data.size()));
    }

    @Override
    public Mono<Void> deleteById(String id) {
        data.remove(id);
        return Mono.empty();
    }

    @Override
    public Mono<Void> deleteById(Publisher<String> pblshr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mono<Void> delete(TransferAudit t) {
        data.remove(t.getId());
        return Mono.empty();
    }

    @Override
    public Mono<Void> deleteAll(Iterable<? extends TransferAudit> itrbl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mono<Void> deleteAll(Publisher<? extends TransferAudit> pblshr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Mono<Void> deleteAll() {
        data.clear();
        return Mono.empty();
    }

    @Override
    public <S extends TransferAudit> Flux<S> insert(Iterable<S> itrbl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <S extends TransferAudit> Flux<S> insert(Publisher<S> pblshr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <S extends TransferAudit> Flux<S> findAll(Example<S> exmpl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <S extends TransferAudit> Flux<S> findAll(Example<S> exmpl, Sort sort) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <S extends TransferAudit> Flux<S> saveAll(Iterable<S> itrbl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <S extends TransferAudit> Flux<S> saveAll(Publisher<S> pblshr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <S extends TransferAudit> Mono<S> findOne(Example<S> exmpl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <S extends TransferAudit> Mono<Long> count(Example<S> exmpl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public <S extends TransferAudit> Mono<Boolean> exists(Example<S> exmpl) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    private static TransferAudit addId(TransferAudit transferAudit) {
        return TransferAudit.builder()
                .amount(transferAudit.getAmount())
                .fromAccount(transferAudit.getFromAccount())
                .toAccount(transferAudit.getToAccount())
                .toUserId(transferAudit.getToUserId())
                .id(UUID.randomUUID().toString())
                .build();
    }

}
