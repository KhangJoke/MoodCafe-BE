package com.moodcafe.tag.abstraction.repository;

import com.moodcafe.tag.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, UUID> {

    List<Tag> findAllByActiveTrue();

    List<Tag> findAllByCategoryTagCategoryIdAndActiveTrue(UUID tagCategoryId);

    List<Tag> findAllByCategoryCodeAndActiveTrue(String categoryCode);

    Optional<Tag> findByName(String name);

    Optional<Tag> findByCategoryTagCategoryIdAndScaleValue(UUID tagCategoryId, Integer scaleValue);

    boolean existsByName(String name);
}
