package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_costing_expressions",
        indexes = {@Index(name = "ftep_costing_expressions_type_associated_id_idx", columnList = "type, associated_id")},
        uniqueConstraints = {@UniqueConstraint(columnNames = {"type", "associated_id"})})
@NoArgsConstructor
@Entity
public class CostingExpression implements FtepEntity<CostingExpression> {

    /**
     * <p>Unique internal identifier of the costing expression.</p>
     */
    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * <p>Which type of entity is associated with the costing expression.</p>
     */
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    private Type type;

    /**
     * <p>The ID of the associated entity, e.g. the {@link FtepService#id} for {@link Type#SERVICE} or the {@link
     * DataSource#id} for {@link Type#DOWNLOAD}.</p>
     */
    @Column(name = "associated_id", nullable = false)
    private Long associatedId;

    /**
     * <p>Expression to be evaluated when calculating the cost of the associated entity.</p>
     */
    @Column(name = "cost_expression", nullable = false)
    private String costExpression;

    /**
     * <p>Expression to be evaluated when projecting the estimated cost of the associated entity. If null, the costing
     * service may fall back on the {@link #costExpression} or handle the case in an arbitrary way.</p>
     * <p>Note that the costing service may have (or require) additional information during evaluation of {@link
     * #costExpression} than is available in the context of this expression.</p>
     */
    @Column(name = "estimated_cost_expression")
    private String estimatedCostExpression;

    @Builder
    public CostingExpression(Type type, Long associatedId, String costExpression, String estimatedCostExpression) {
        this.type = type;
        this.associatedId = associatedId;
        this.costExpression = costExpression;
        this.estimatedCostExpression = estimatedCostExpression;
    }

    @Override
    public int compareTo(CostingExpression o) {
        return ComparisonChain.start().compare(id, o.id).result();
    }

    public enum Type {
        SERVICE, DOWNLOAD
    }

}
