package com.gymflow.gym.infrastructure.persistence;

import java.util.Optional;

import com.gymflow.gym.domain.model.Gym;
import com.gymflow.gym.domain.port.GymRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class GymRepositoryAdapter implements GymRepository {

    private final GymJpaRepository jpa;
    private final GymPersistenceMapper mapper;

    @Override
    public Gym save(Gym gym) {
        return mapper.toDomain(jpa.save(mapper.toEntity(gym)));
    }

    @Override
    public Optional<Gym> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpa.existsBySlug(slug);
    }
}
