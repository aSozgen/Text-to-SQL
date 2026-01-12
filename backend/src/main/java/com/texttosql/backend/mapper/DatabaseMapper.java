package com.texttosql.backend.mapper;

import com.texttosql.backend.dto.entity.DatabaseDto;
import com.texttosql.backend.entity.DatabaseEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DatabaseMapper {

    DatabaseDto toDto(DatabaseEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "currentVersion", ignore = true)
    DatabaseEntity toEntity(DatabaseDto dto);

    List<DatabaseDto> toDtoList(List<DatabaseEntity> entities);
}
