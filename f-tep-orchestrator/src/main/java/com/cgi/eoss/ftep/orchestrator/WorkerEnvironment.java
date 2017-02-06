package com.cgi.eoss.ftep.orchestrator;

/**
 * <p>Provisioning environments for F-TEP Workers.</p>
 */
public enum WorkerEnvironment {

    /**
     * <p>Provides access to Workers using the default local environment.</p>
     */
    LOCAL,

    /**
     * <p>Provides access to Workers provisioned in the CEMS cloud.</p>
     */
    CEMS,

    /**
     * <p>Provides access to Workers provisioned in the IPT cloud.</p>
     */
    IPT

}
