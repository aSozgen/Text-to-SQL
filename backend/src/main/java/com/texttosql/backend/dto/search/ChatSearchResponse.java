package com.texttosql.backend.dto.search;

import com.texttosql.backend.dto.ChatDto;
import com.texttosql.backend.dto.MessageDto;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ChatSearchResponse {
    private List<ChatDto> chats;
    private List<MessageDto> messages;
}