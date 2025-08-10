package com.aseubel.common.jpa.support;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.NoRepositoryBean;

/**
 * @author Aseubel
 * @date 2025/8/5 上午9:48
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>,
    JpaSpecificationExecutor<T>, QuerydslPredicateExecutor<T> {

}