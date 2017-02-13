package com.cgi.eoss.ftep.api.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.acls.domain.AuditLogger;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.AuditableAccessControlEntry;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

/**
 * <p>Clone of Spring's {@link org.springframework.security.acls.domain.ConsoleAuditLogger}, logging to slf4j.</p>
 */
@Component
final class Slf4jAuditLogger implements AuditLogger {
    private static Logger LOG = LoggerFactory.getLogger(Slf4jAuditLogger.class);

    @Override
    public void logIfNeeded(boolean granted, AccessControlEntry ace) {
        Assert.notNull(ace, "AccessControlEntry required");

        if (ace instanceof AuditableAccessControlEntry) {
            AuditableAccessControlEntry auditableAce = (AuditableAccessControlEntry) ace;

            if (granted && auditableAce.isAuditSuccess()) {
                LOG.info("GRANTED due to ACE: {}", ace);
            } else if (!granted && auditableAce.isAuditFailure()) {
                LOG.info("DENIED due to ACE: {}", ace);
            }
        }
    }
}