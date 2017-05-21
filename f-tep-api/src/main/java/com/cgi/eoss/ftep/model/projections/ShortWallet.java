package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.model.Wallet;
import org.springframework.data.rest.core.config.Projection;
import org.springframework.hateoas.Identifiable;

/**
 * <p>Abbreviated representation of a Wallet entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortWallet", types = {Wallet.class})
public interface ShortWallet extends Identifiable<Long> {
    ShortUser getOwner();
    Integer getBalance();
}
