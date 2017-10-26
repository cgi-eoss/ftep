package com.cgi.eoss.ftep.metrics;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQuery;
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
import java.util.function.Predicate;

import static com.cgi.eoss.ftep.model.QJob.job;

public class FtepServiceExecutionMetrics implements PublicMetrics {

    private final EntityManager em;

    FtepServiceExecutionMetrics(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Metric<?>> metrics() {
        Collection<Metric<?>> metrics = new HashSet<>();

        addJobStatusMetrics(metrics);

        return metrics;
    }

    private void addJobStatusMetrics(Collection<Metric<?>> metrics) {
        JPAQuery<Job> query = new JPAQuery<>(em);

        List<Tuple> totalCounts = query.from(job)
                .select(Projections.tuple(job.config.service.type, job.status, Wildcard.count))
                .groupBy(job.config.service.type, job.status)
                .fetch();

        List<Tuple> d90Counts = query.from(job)
                .select(Projections.tuple(job.config.service.type, job.status, Wildcard.count))
                .where(job.startTime.gt(LocalDateTime.now(ZoneOffset.UTC).minusDays(90)))
                .groupBy(job.config.service.type, job.status)
                .fetch();

        List<Tuple> d30Counts = query.from(job)
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
    }

    private long getTupleCount(Collection<Tuple> tuple, Predicate<? super Tuple> keyPredicate, NumberExpression<Long> valueSelector) {
        return tuple.stream()
                .filter(keyPredicate)
                .mapToLong(t -> Optional.ofNullable(t.get(valueSelector)).orElse(0L))
                .sum();
    }

}
