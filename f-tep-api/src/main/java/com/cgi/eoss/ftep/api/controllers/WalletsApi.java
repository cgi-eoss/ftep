package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.model.projections.ShortWallet;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.rest.core.annotation.RestResource;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.Optional;

@RepositoryRestResource(path = "wallets", itemResourceRel = "wallet", collectionResourceRel = "wallets", excerptProjection = ShortWallet.class)
public interface WalletsApi extends WalletsApiCustom, PagingAndSortingRepository<Wallet, Long> {

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    <S extends Wallet> Iterable<S> saveAll(Iterable<S> wallets);

    @Override
    @PreAuthorize("hasRole('ADMIN')")
    <S extends Wallet> S save(@Param("wallet") S wallet);

    @Override
    @PostAuthorize("hasRole('ADMIN') or @ftepSecurityService.currentUser.equals(returnObject.get().owner)")
    Optional<Wallet> findById(Long id);

    @Override
    @RestResource(exported = false)
    void deleteAll(Iterable<? extends Wallet> wallets);

    @Override
    @RestResource(exported = false)
    void delete(Wallet wallet);

}
