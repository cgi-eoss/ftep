package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepEntityWithOwner;
import com.cgi.eoss.ftep.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class JpaFtepEntityOwnerDataService implements FtepEntityOwnerDataService {

    private final EntityManager em;

    public JpaFtepEntityOwnerDataService(EntityManager em) {
        this.em = em;
    }

    @Override
    public User getOwner(Class<? extends FtepEntityWithOwner> entityClass, Long id) {
        return Optional.ofNullable(em.find(entityClass, id))
                .map(FtepEntityWithOwner::getOwner)
                .orElseThrow(() -> new EntityNotFoundException("No " + entityClass.getSimpleName() + " found for Id: " + id));
    }

}
