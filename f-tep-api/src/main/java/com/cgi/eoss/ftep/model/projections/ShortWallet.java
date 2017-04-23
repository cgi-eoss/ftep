package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.model.Wallet;
import org.springframework.data.rest.core.config.Projection;

/**
 * <p>Abbreviated representation of a Wallet entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortWallet", types = {Wallet.class})
public interface ShortWallet extends EmbeddedId {
    ShortUser getOwner();
    Integer getBalance();
}
