package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.Subscription;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.security.access.prepost.PreAuthorize;

@PreAuthorize("hasRole('ADMIN')")
@RepositoryRestResource(
        path = "subscriptions",
        itemResourceRel = "subscription",
        collectionResourceRel = "subscriptions"
)
public interface SubscriptionsApi extends PagingAndSortingRepository<Subscription, Long> {

}
