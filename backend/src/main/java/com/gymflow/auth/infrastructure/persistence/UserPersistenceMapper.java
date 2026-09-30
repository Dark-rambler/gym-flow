package com.gymflow.auth.infrastructure.persistence;

import com.gymflow.auth.domain.model.AppUser;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface UserPersistenceMapper {

    // MapStruct confunde los "withers" del record con setters fluidos; el record se construye por constructor.
    @Mapping(target = "withRole", ignore = true)
    @Mapping(target = "withActive", ignore = true)
    AppUser toDomain(AppUserJpaEntity entity);

    AppUserJpaEntity toEntity(AppUser user);
}
