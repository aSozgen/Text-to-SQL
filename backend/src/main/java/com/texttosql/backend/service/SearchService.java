package com.texttosql.backend.service;

import com.texttosql.backend.dto.search.ChatSearchResponse;
import com.texttosql.backend.dto.search.SchemaSearchResponse;
import com.texttosql.backend.entity.UserEntity;
import com.texttosql.backend.mapper.*;
import com.texttosql.backend.repository.*;
import com.texttosql.backend.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final DatabaseRepository databaseRepository;
    private final TableRepository tableRepository;
    private final ColumnRepository columnRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    private final DatabaseMapper databaseMapper;
    private final TableMapper tableMapper;
    private final ColumnMapper columnMapper;
    private final ChatMapper chatMapper;
    private final MessageMapper messageMapper;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public SchemaSearchResponse searchSchema(CustomUserDetails userDetails, String query, int page, int size, String sort, String direction) {
        UserEntity user = userMapper.toEntity(userDetails);
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        return SchemaSearchResponse.builder()
                .databases(databaseRepository.searchDatabases(user, query, pageRequest)
                        .map(databaseMapper::toDto).getContent())
                .tables(tableRepository.searchTables(user, query, pageRequest)
                        .map(tableMapper::toDto).getContent())
                .columns(columnRepository.searchColumns(user, query, pageRequest)
                        .map(columnMapper::toDto).getContent())
                .build();
    }

    @Transactional(readOnly = true)
    public ChatSearchResponse searchChat(CustomUserDetails userDetails, String query, int page, int size, String sort, String direction) {
        UserEntity user = userMapper.toEntity(userDetails);
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        return ChatSearchResponse.builder()
                .chats(chatRepository.findByUserAndActiveTrueAndNameContainingIgnoreCase(user, query, pageRequest)
                        .map(chatMapper::toDto).getContent())
                .messages(messageRepository.searchMessages(user, query, pageRequest)
                        .map(messageMapper::toDto).getContent())
                .build();
    }
}