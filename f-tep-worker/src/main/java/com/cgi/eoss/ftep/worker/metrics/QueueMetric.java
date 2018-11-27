package com.cgi.eoss.ftep.worker.metrics;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "queue_metrics")
@NoArgsConstructor
public class QueueMetric implements Serializable {

    @Id
    private long epoch;
    private long queueLength;

    public QueueMetric(long epoch, long queueLength) {
        this.epoch = epoch;
        this.queueLength = queueLength;
    }
}
