package com.cgi.eoss.ftep.api.security;

import lombok.extern.log4j.Log4j2;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.AuditableAccessControlEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * <p>Clone of Spring's {@link org.springframework.security.acls.domain.ConsoleAuditLogger}, logging to log4j2 API.</p>
 */
@Component
@Log4j2
final class Log4j2AuditLogger implements AuditLogger {
    @Override
    public void logIfNeeded(boolean granted, AccessControlEntry ace) {
        Assert.notNull(ace, "AccessControlEntry required");

        if (ace instanceof AuditableAccessControlEntry) {
            AuditableAccessControlEntry auditableAce = (AuditableAccessControlEntry) ace;

            if (granted && auditableAce.isAuditSuccess()) {
                LOG.debug("GRANTED due to ACE: {}", ace);
            } else if (!granted && auditableAce.isAuditFailure()) {
                LOG.debug("DENIED due to ACE: {}", ace);
            }
        }
    }
}