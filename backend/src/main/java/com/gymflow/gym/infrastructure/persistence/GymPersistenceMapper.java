package com.gymflow.gym.infrastructure.persistence;

import com.gymflow.gym.domain.model.Gym;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
interface GymPersistenceMapper {

    Gym toDomain(GymJpaEntity entity);

    GymJpaEntity toEntity(Gym gym);
}
