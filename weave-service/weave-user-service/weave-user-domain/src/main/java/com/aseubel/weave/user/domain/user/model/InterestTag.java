package com.aseubel.weave.user.domain.user.model;

import com.aseubel.common.jpa.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Aseubel
 * @date 2025/6/27 下午6:38
 */
@Getter
@Setter
@Entity
@Table(name = "interest_tag")
public class InterestTag extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50)
    private String name; // e.g., "手工艺爱好者", "戏曲学习者"

    @Column(nullable = true, unique = false)
    private String color;
}
