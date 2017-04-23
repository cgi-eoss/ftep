package com.cgi.eoss.ftep.model.projections;

import com.cgi.eoss.ftep.model.WalletTransaction;
import org.springframework.data.rest.core.config.Projection;

import java.time.LocalDateTime;

/**
 * <p>Abbreviated representation of a WalletTransaction entity, for embedding in REST responses.</p>
 */
@Projection(name = "shortWalletTransaction", types = {WalletTransaction.class})
public interface ShortWalletTransaction extends EmbeddedId {
    ShortUser getOwner();
    Integer getBalanceChange();
    LocalDateTime getTransactionTime();
    WalletTransaction.Type getType();
}
