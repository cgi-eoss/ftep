package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.model.projections.ShortWallet;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.method.P;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

@RepositoryRestResource(path = "wallets", itemResourceRel = "wallet", collectionResourceRel = "wallets", excerptProjection = ShortWallet.class)
public interface WalletsApi extends PagingAndSortingRepository<Wallet, Long> {

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    <S extends Wallet> Iterable<S> save(Iterable<S> wallets);

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    <S extends Wallet> S save(@P("wallet") S wallet);

    @Override
    @PostAuthorize("hasRole('ADMIN') or @ftepSecurityService.currentUser.equals(returnObject.owner)")
    Wallet findOne(Long id);

    @Override
    @RestResource(exported = false)
    void delete(Iterable<? extends Wallet> wallets);

    @Override
    @RestResource(exported = false)
    void delete(Wallet wallet);

}
