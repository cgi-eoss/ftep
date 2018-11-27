package com.cgi.eoss.ftep.worker.metrics;

import java.io.Serializable;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface QueueMetricsRepository extends CrudRepository<QueueMetric, Serializable> {

    @Modifying
    @Transactional
    @Query("DELETE FROM QueueMetric qm WHERE :time - qm.epoch > :duration")
    int removeOlderThan(@Param("time") long time, @Param("duration") long duration);

    @Query("SELECT COALESCE(COUNT(1), 0) as cnt, COALESCE(AVG(queueLength), 0.0) as average from QueueMetric qm where :time - epoch <= :duration")
    List<Object[]> getMetrics(@Param("time") long time, @Param("duration") long duration);
}
