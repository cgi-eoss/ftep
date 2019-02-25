package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.FtepEntity;
import com.cgi.eoss.ftep.persistence.SpringJpaRepositoryIgnore;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;

import java.util.List;
import java.util.stream.Stream;

/**
 * <p>A generic DAO interface for F-TEP entity objects.</p>
 *
 * @param <T> The type of entity managed by this DAO. It is expected that the type has a Long-type primary identifier.
 */
@SpringJpaRepositoryIgnore
public interface FtepEntityDao<T extends FtepEntity<T>> extends JpaRepository<T, Long>, QuerydslPredicateExecutor<T> {

    @Override
    List<T> findAll(Predicate predicate);

    @Override
    List<T> findAll(Predicate predicate, Sort sort);

    @Override
    List<T> findAll(Predicate predicate, OrderSpecifier<?>... orders);

    @Override
    List<T> findAll(OrderSpecifier<?>... orders);

    @Query("select distinct t.id from #{#entityName} t")
    List<Long> findAllIds();

    @Query("select t from #{#entityName} t")
    Stream<T> streamAll();

}
