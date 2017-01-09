package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepEntity;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

@Transactional(readOnly = true)
abstract class AbstractJpaDataService<T extends FtepEntity<T>> implements FtepEntityDataService<T> {
    @Override
    @Transactional
    public void delete(T entity) {
        getDao().delete(entity);
    }

    @Override
    @Transactional
    public void deleteAll() {
        getDao().deleteAll();
    }

    @Override
    public List<T> getAll() {
        return getDao().findAll();
    }

    @Override
    public List<T> getAllFull() {
        return getDao().findAll();
    }

    @Override
    @Transactional
    public T save(T entity) {
        resyncId(entity);
        return getDao().save(entity);
    }

    @Override
    @Transactional
    public Collection<T> save(Collection<T> entities) {
        entities.forEach(this::resyncId);
        return getDao().save(entities);
    }

    @Override
    public T getById(Long id) {
        return getDao().findOne(id);
    }

    @Override
    public List<T> getByIds(Collection<Long> ids) {
        return getDao().findAll(ids);
    }

    @Override
    public boolean isUnique(T entity) {
        return !getDao().exists(Example.of(entity, getUniqueMatcher()));
    }

    @Override
    public boolean isUniqueAndValid(T obj) {
        return isUnique(obj);
    }

    @Override
    public T refresh(T obj) {
        return null;
    }

    @Override
    public T refreshFull(T obj) {
        return null;
    }

    /**
     * @param entity The potentially detached entity
     */
    private void resyncId(T entity) {
        T tmp = getDao().findOne(Example.of(entity, getUniqueMatcher()));
        if (tmp != null) {
            entity.setId(tmp.getId());
        }
    }

    abstract FtepEntityDao<T> getDao();

    abstract ExampleMatcher getUniqueMatcher();
}
