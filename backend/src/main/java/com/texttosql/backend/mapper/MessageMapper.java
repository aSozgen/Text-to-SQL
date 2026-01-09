package com.texttosql.backend.mapper;

import com.texttosql.backend.dto.MessageDto;
import com.texttosql.backend.entity.MessageEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MessageMapper {

    MessageDto toDto(MessageEntity entity);

    @Mapping(target = "chat", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    MessageEntity toEntity(MessageDto dto);

    List<MessageDto> toDtoList(List<MessageEntity> entities);
}
