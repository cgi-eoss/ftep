package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.WalletTransaction;
import com.cgi.eoss.ftep.model.projections.ShortWalletTransaction;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;

@RepositoryRestResource(path = "walletTransactions", itemResourceRel = "walletTransaction", collectionResourceRel = "walletTransactions", excerptProjection = ShortWalletTransaction.class)
public interface WalletTransactionsApi extends PagingAndSortingRepository<WalletTransaction, Long> {

    @Override
    @RestResource(exported = false)
    <S extends WalletTransaction> Iterable<S> save(Iterable<S> walletTransactions);

    @Override
    @RestResource(exported = false)
    <S extends WalletTransaction> S save(@P("walletTransaction") S walletTransaction);

    @Override
    @PostAuthorize("hasRole('ADMIN') or @ftepSecurityService.currentUser.equals(returnObject.owner)")
    WalletTransaction findOne(Long id);

    @Override
    @RestResource(exported = false)
    void delete(Iterable<? extends WalletTransaction> walletTransactions);

    @Override
    @RestResource(exported = false)
    void delete(WalletTransaction walletTransaction);

}
