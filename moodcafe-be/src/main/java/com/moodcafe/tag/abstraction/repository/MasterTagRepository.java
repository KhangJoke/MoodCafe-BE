package com.moodcafe.tag.abstraction.repository;

import com.moodcafe.tag.entity.MasterTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MasterTagRepository extends JpaRepository<MasterTag, UUID> {

    List<MasterTag> findAllByActiveTrue();

    List<MasterTag> findAllByCategoryAndActiveTrue(String category);

    Optional<MasterTag> findByName(String name);

    boolean existsByName(String name);
}
