package com.moodcafe.store.abstraction.repository;

import com.moodcafe.store.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, UUID> {

    Optional<Amenity> findByName(String name);

    boolean existsByName(String name);
}
