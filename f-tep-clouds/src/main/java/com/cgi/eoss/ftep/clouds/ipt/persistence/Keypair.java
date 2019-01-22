package com.cgi.eoss.ftep.clouds.ipt.persistence;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.io.Serializable;

@Data
@Entity
@Table(name = "keypairs")
@NoArgsConstructor
@AllArgsConstructor
public class Keypair implements Serializable {
    @Id
    private String serverId;

    @Lob
    @Column(length = 10000)
    private String privateKey;

    @Lob
    @Column(length = 1000)
    private String publicKey;
}