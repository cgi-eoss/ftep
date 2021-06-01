package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Subscription;
import com.cgi.eoss.ftep.persistence.service.SubscriptionDataService;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@BasePathAwareController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SubscriptionsApiExtension {

    private final FtepSecurityService ftepSecurityService;
    private final SubscriptionDataService subscriptionDataService;

    @PostMapping("/{subscriptionId}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> cancelSubscription(@ModelAttribute("subscriptionId") Subscription subscription) {
        subscription.setCanceller(ftepSecurityService.getCurrentUser());
        subscription.setCancellationTime(LocalDateTime.now(ZoneOffset.UTC));
        subscriptionDataService.save(subscription);
        return ResponseEntity.noContent().build();
    }
}
