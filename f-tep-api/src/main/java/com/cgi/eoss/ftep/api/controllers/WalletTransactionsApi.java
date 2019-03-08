package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.WalletTransaction;
import com.cgi.eoss.ftep.model.projections.ShortWalletTransaction;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;

import java.util.Optional;

@RepositoryRestResource(path = "walletTransactions", itemResourceRel = "walletTransaction", collectionResourceRel = "walletTransactions", excerptProjection = ShortWalletTransaction.class)
public interface WalletTransactionsApi extends WalletTransactionsApiCustom, PagingAndSortingRepository<WalletTransaction, Long> {

    @Override
    @RestResource(exported = false)
    <S extends WalletTransaction> Iterable<S> saveAll(Iterable<S> walletTransactions);

    @Override
    @RestResource(exported = false)
    <S extends WalletTransaction> S save(@Param("walletTransaction") S walletTransaction);

    @Override
    @PostAuthorize("hasRole('ADMIN') or @ftepSecurityService.currentUser.equals(returnObject.get().owner)")
    Optional<WalletTransaction> findById(Long id);

    @Override
    @RestResource(exported = false)
    void deleteAll(Iterable<? extends WalletTransaction> walletTransactions);

    @Override
    @RestResource(exported = false)
    void delete(WalletTransaction walletTransaction);

}
