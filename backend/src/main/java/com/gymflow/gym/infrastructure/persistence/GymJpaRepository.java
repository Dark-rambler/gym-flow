package com.gymflow.gym.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface GymJpaRepository extends JpaRepository<GymJpaEntity, Long> {

    boolean existsBySlug(String slug);
}
