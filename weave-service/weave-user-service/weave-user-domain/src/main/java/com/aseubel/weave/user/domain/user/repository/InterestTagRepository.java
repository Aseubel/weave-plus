package com.aseubel.weave.user.domain.user.repository;


import com.aseubel.common.jpa.support.BaseRepository;
import com.aseubel.weave.user.domain.user.model.InterestTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 兴趣标签Repository
 * @author Aseubel
 * @date 2025/6/27
 */
public interface InterestTagRepository extends BaseRepository<InterestTag, Long> {
    
    /**
     * 根据ID列表查找兴趣标签
     */
    List<InterestTag> findAllById(Iterable<Long> ids);
    
    /**
     * 根据名称查找兴趣标签
     */
    InterestTag findByName(String name);
    
    /**
     * 检查名称是否存在
     */
    boolean existsByName(String name);
}