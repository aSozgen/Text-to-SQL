package com.texttosql.backend.mapper;

import com.texttosql.backend.dto.entity.TableDto;
import com.texttosql.backend.entity.TableEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TableMapper {

    @Mapping(source = "database.databaseId", target = "databaseId")
    TableDto toDto(TableEntity entity);

    @Mapping(target = "database", ignore = true)
    @Mapping(target = "active", ignore = true)
    TableEntity toEntity(TableDto dto);

    List<TableDto> toDtoList(List<TableEntity> entities);
}
