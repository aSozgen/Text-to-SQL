package com.texttosql.backend.dto.search;

import com.texttosql.backend.dto.entity.ChatDto;
import com.texttosql.backend.dto.entity.MessageDto;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ChatSearchResponse {
    private List<ChatDto> chats;
    private List<MessageDto> messages;
}