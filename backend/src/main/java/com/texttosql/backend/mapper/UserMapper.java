package com.texttosql.backend.mapper;

import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.security.CustomUserDetails;
import org.mapstruct.Mapper;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface UserMapper {

    CustomUserDetails toDto(UserEntity entity);

    UserEntity toEntity(CustomUserDetails dto);
}
