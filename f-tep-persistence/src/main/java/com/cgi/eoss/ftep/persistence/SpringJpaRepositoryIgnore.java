package com.cgi.eoss.ftep.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Marker interface to exclude a class from Spring's {@link org.springframework.data.jpa.repository.config.EnableJpaRepositories}
 * scanning.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SpringJpaRepositoryIgnore {
}
