package com.gymflow.gym.domain.port;

import java.util.Optional;

import com.gymflow.gym.domain.model.Gym;

public interface GymRepository {

    Gym save(Gym gym);

    Optional<Gym> findById(Long id);

    boolean existsBySlug(String slug);
}
