package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.persistence.service.WalletDataService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.data.rest.webmvc.RepositoryRestController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>A {@link RepositoryRestController} for interacting with {@link Wallet}s. Offers additional functionality over
 * the standard CRUD-style {@link WalletsApi}.</p>
 */
@RestController
@BasePathAwareController
@RequestMapping("/wallets")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Log4j2
public class WalletsApiExtension {

    private final WalletDataService walletDataService;

    @PostMapping("/{walletId}/credit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity credit(@ModelAttribute("walletId") Wallet wallet, @RequestBody CreditParam credit) {
        walletDataService.creditBalance(wallet, credit.getAmount());
        return ResponseEntity.noContent().build();
    }

    @Data
    private static final class CreditParam {
        private int amount;
    }

}
