package com.moodcafe.tag.abstraction.repository;

import com.moodcafe.tag.entity.TagCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagCategoryRepository extends JpaRepository<TagCategory, UUID> {

    Optional<TagCategory> findByCode(String code);

    List<TagCategory> findAllByActiveTrueOrderByDisplayOrderAsc();

    List<TagCategory> findAllByOrderByDisplayOrderAsc();
}
