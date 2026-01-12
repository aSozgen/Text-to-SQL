package com.texttosql.backend.mapper;

import com.texttosql.backend.dto.entity.ColumnDto;
import com.texttosql.backend.entity.ColumnEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ColumnMapper {

    ColumnDto toDto(ColumnEntity entity);

    @Mapping(target = "table", ignore = true)
    @Mapping(target = "active", ignore = true)
    ColumnEntity toEntity(ColumnDto dto);

    List<ColumnDto> toDtoList(List<ColumnEntity> entities);
}
