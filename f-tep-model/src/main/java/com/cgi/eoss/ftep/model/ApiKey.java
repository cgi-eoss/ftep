package com.cgi.eoss.ftep.model;

import com.google.common.collect.ComparisonChain;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * <p>
 * F-TEP API Keys. Stores user API keys
 * </p>
 */
@Data
@EqualsAndHashCode(exclude = {"id"})
@Table(name = "ftep_api_keys",
        indexes = {@Index(name = "ftep_api_keys_owner_idx", columnList = "owner")})
@NoArgsConstructor
@Entity
public class ApiKey implements FtepEntityWithOwner<ApiKey> {
    /**
     * <p>
     * Internal unique identifier of the api key
     * </p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * <p>
     * The user owning the endpoint.
     * </p>
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner")
    private User owner;

    /**
     * <p>
     * The API key
     * </p>
     */
    @Column(name = "api_key")
    private String apiKeyString;

    public ApiKey(String apiKeyString) {
        this.apiKeyString = apiKeyString;
    }

    // @Override
    public int compareTo(ApiKey o) {
        return ComparisonChain.start().compare(owner.getId(), o.owner.getId()).result();
    }

}
