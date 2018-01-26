package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.FtepEntity;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.querydsl.core.types.Predicate;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
        return getDao().saveAndFlush(entity);
    }

    @Override
    @Transactional
    public Collection<T> save(Collection<T> entities) {
        entities.forEach(this::resyncId);
        try {
            return getDao().save(entities);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        } finally {
            getDao().flush();
        }
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
        return !getDao().exists(getUniquePredicate(entity));
    }

    @Override
    public boolean isUniqueAndValid(T obj) {
        return isUnique(obj);
    }

    @Override
    public T findOneByExample(T example) {
        return getDao().findOne(getUniquePredicate(example));
    }

    @Override
    public T refresh(T obj) {
        return Optional.ofNullable(getDao().findOne(getUniquePredicate(obj))).orElseThrow(() -> new EmptyResultDataAccessException(1));
    }

    @Override
    public T refreshFull(T obj) {
        return refresh(obj);
    }

    @Override
    public T convert(Long source) {
        return getById(source);
    }

    /**
     * @param entity The potentially detached entity
     */
    private void resyncId(T entity) {
        T tmp = getDao().findOne(getUniquePredicate(entity));
        if (tmp != null) {
            entity.setId(tmp.getId());
        }
    }

    abstract FtepEntityDao<T> getDao();

    abstract Predicate getUniquePredicate(T entity);

}
