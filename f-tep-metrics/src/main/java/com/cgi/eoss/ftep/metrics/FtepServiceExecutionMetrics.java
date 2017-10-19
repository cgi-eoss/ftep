package com.cgi.eoss.ftep.metrics;

import com.cgi.eoss.ftep.model.FtepService;
import com.cgi.eoss.ftep.model.Job;
import com.cgi.eoss.ftep.model.QJob;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQuery;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

public class FtepServiceExecutionMetrics implements PublicMetrics {

    private final EntityManager em;

    FtepServiceExecutionMetrics(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<Metric<?>> metrics() {
        Collection<Metric<?>> metrics = new HashSet<>();

        JPAQuery<Job> query = new JPAQuery<>(em);

        List<Tuple> totalCounts = query.from(QJob.job)
                .select(Projections.tuple(QJob.job.status, Wildcard.count))
                .groupBy(QJob.job.status)
                .fetch();
        for (Job.Status status : Job.Status.values()) {
            metrics.add(new Metric<>("ftep.jobs.total." + status.name().toLowerCase(), getTupleElement(totalCounts, QJob.job.status.eq(status), Wildcard.count, 0L)));
        }

        List<Tuple> serviceTypeCounts = query.from(QJob.job)
                .select(Projections.tuple(QJob.job.config.service.type, QJob.job.status, Wildcard.count))
                .groupBy(QJob.job.config.service.type, QJob.job.status)
                .fetch();
        for (FtepService.Type type : FtepService.Type.values()) {
            for (Job.Status status : Job.Status.values()) {
                metrics.add(new Metric<>("ftep.jobs." + type.name().toLowerCase() + "." + status.name().toLowerCase(),
                        getTupleElement(serviceTypeCounts, QJob.job.config.service.type.eq(type).and(QJob.job.status.eq(status)), Wildcard.count, 0L)));
            }
        }

        return metrics;
    }

    private <T> T getTupleElement(Collection<Tuple> tuple, BooleanExpression keySelector, Expression<T> valueSelector, T defaultValue) {
        return tuple.stream()
                .filter(t -> t.get(keySelector))
                .findFirst()
                .map(t -> t.get(valueSelector))
                .orElse(defaultValue);
    }

}
