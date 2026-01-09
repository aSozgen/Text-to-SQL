package com.texttosql.backend.mapper;

import com.texttosql.backend.dto.ChatDto;
import com.texttosql.backend.entity.ChatEntity;
import org.mapstruct.*;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ChatMapper {

    ChatDto toDto(ChatEntity entity);

    @Mapping(target = "user", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    ChatEntity toEntity(ChatDto dto);

     List<ChatDto> toDtoList(List<ChatEntity> entities);
}