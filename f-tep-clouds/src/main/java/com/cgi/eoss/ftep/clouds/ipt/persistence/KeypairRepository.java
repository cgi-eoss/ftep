package com.cgi.eoss.ftep.clouds.ipt.persistence;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KeypairRepository extends CrudRepository<Keypair, String> {
}
