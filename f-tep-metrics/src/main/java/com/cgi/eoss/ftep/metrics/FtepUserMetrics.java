package com.cgi.eoss.ftep.metrics;

import com.cgi.eoss.ftep.model.Role;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.MoreExecutors;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQuery;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.cgi.eoss.ftep.model.QUser.user;

@Log4j2
public class FtepUserMetrics implements PublicMetrics {

    // Use a cache to allow simple throttling of the metrics lookups
    private final LoadingCache<Boolean, Collection<Metric<?>>> metricsCache;

    FtepUserMetrics(EntityManager em) {
        this.metricsCache = CacheBuilder.newBuilder()
                .initialCapacity(1)
                .maximumSize(1)
                .refreshAfterWrite(15, TimeUnit.MINUTES)
                .build(new MetricsCacheLoader(em));
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Metric<?>> metrics() {
        return metricsCache.getUnchecked(Boolean.TRUE);
    }

    private static final class MetricsCacheLoader extends CacheLoader<Boolean, Collection<Metric<?>>> {
        private final EntityManager em;

        private MetricsCacheLoader(EntityManager em) {
            this.em = em;
        }

        @Override
        public ListenableFuture<Collection<Metric<?>>> reload(Boolean key, Collection<Metric<?>> oldValue) throws Exception {
            ListenableFutureTask<Collection<Metric<?>>> task = ListenableFutureTask.create(this::loadMetricsFromDatabase);
            MoreExecutors.directExecutor().execute(task);
            return task;
        }

        @Override
        public Collection<Metric<?>> load(Boolean key) throws Exception {
            return loadMetricsFromDatabase();
        }

        private Collection<Metric<?>> loadMetricsFromDatabase() {
            LOG.debug("Updating F-TEP user metrics");
            Collection<Metric<?>> metrics = new HashSet<>();

            List<Tuple> totalCounts = new JPAQuery<>(em).from(user)
                    .select(Projections.tuple(user.role, Wildcard.count))
                    .groupBy(user.role)
                    .fetch();

            for (Role role : Role.values()) {
                Predicate<Tuple> rolePredicate = t -> t.get(user.role) == role;
                metrics.add(new Metric<>("ftep.users." + role.name().toLowerCase(), getTupleCount(totalCounts, rolePredicate, Wildcard.count)));
            }

            return metrics;
        }

        private long getTupleCount(Collection<Tuple> tuple, Predicate<? super Tuple> keyPredicate, NumberExpression<Long> valueSelector) {
            return tuple.stream()
                    .filter(keyPredicate)
                    .mapToLong(t -> Optional.ofNullable(t.get(valueSelector)).orElse(0L))
                    .sum();
        }
    }
}
