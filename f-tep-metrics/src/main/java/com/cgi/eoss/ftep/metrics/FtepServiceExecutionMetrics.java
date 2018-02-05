package com.cgi.eoss.ftep.metrics;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.cgi.eoss.ftep.model.QJob.job;

@Log4j2
public class FtepServiceExecutionMetrics implements PublicMetrics {

    // Use a cache to allow simple throttling of the metrics lookups
    private final LoadingCache<Boolean, Collection<Metric<?>>> metricsCache;

    FtepServiceExecutionMetrics(EntityManager em) {
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
            LOG.debug("Updating F-TEP service execution metrics");
            Collection<Metric<?>> metrics = new HashSet<>();

            List<Tuple> totalCounts = new JPAQuery<>(em).from(job)
                    .select(Projections.tuple(job.config.service.type, job.status, Wildcard.count))
                    .groupBy(job.config.service.type, job.status)
                    .fetch();

            List<Tuple> d90Counts = new JPAQuery<>(em).from(job)
                    .select(Projections.tuple(job.config.service.type, job.status, Wildcard.count))
                    .where(job.startTime.gt(LocalDateTime.now(ZoneOffset.UTC).minusDays(90)))
                    .groupBy(job.config.service.type, job.status)
                    .fetch();

            List<Tuple> d30Counts = new JPAQuery<>(em).from(job)
                    .select(Projections.tuple(job.config.service.type, job.status, Wildcard.count))
                    .where(job.startTime.gt(LocalDateTime.now(ZoneOffset.UTC).minusDays(30)))
                    .groupBy(job.config.service.type, job.status)
                    .fetch();

            for (Job.Status status : Job.Status.values()) {
                Predicate<Tuple> statusPredicate = t -> t.get(job.status) == status;
                metrics.add(new Metric<>("ftep.jobs.total." + status.name().toLowerCase(), getTupleCount(totalCounts, statusPredicate, Wildcard.count)));
                metrics.add(new Metric<>("ftep.jobs.total.90d." + status.name().toLowerCase(), getTupleCount(d90Counts, statusPredicate, Wildcard.count)));
                metrics.add(new Metric<>("ftep.jobs.total.30d." + status.name().toLowerCase(), getTupleCount(d30Counts, statusPredicate, Wildcard.count)));

                for (FtepService.Type type : FtepService.Type.values()) {
                    Predicate<Tuple> serviceTypePredicate = statusPredicate.and(t -> t.get(job.config.service.type) == type);
                    metrics.add(new Metric<>("ftep.jobs." + type.name().toLowerCase() + "." + status.name().toLowerCase(), getTupleCount(totalCounts, serviceTypePredicate, Wildcard.count)));
                    metrics.add(new Metric<>("ftep.jobs." + type.name().toLowerCase() + ".90d." + status.name().toLowerCase(), getTupleCount(d90Counts, serviceTypePredicate, Wildcard.count)));
                    metrics.add(new Metric<>("ftep.jobs." + type.name().toLowerCase() + ".30d." + status.name().toLowerCase(), getTupleCount(d30Counts, serviceTypePredicate, Wildcard.count)));
                }
            }

            Long totalUserCounts = new JPAQuery<>(em).from(job)
                    .select(job.owner)
                    .distinct().fetchCount();

            Long d90UserCounts = new JPAQuery<>(em).from(job)
                    .select(job.owner)
                    .where(job.startTime.gt(LocalDateTime.now(ZoneOffset.UTC).minusDays(90)))
                    .distinct().fetchCount();

            Long d30UserCounts = new JPAQuery<>(em).from(job)
                    .select(job.owner)
                    .where(job.startTime.gt(LocalDateTime.now(ZoneOffset.UTC).minusDays(30)))
                    .distinct().fetchCount();

            metrics.add(new Metric<>("ftep.jobs.unique-users", totalUserCounts));
            metrics.add(new Metric<>("ftep.jobs.unique-users.90d", d90UserCounts));
            metrics.add(new Metric<>("ftep.jobs.unique-users.30d", d30UserCounts));

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
