package com.texttosql.backend.controller;

import com.texttosql.backend.dto.search.ChatSearchResponse;
import com.texttosql.backend.dto.search.SchemaSearchResponse;
import com.texttosql.backend.security.CustomUserDetails;
import com.texttosql.backend.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping("/schema")
    public ResponseEntity<SchemaSearchResponse> searchSchema(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(searchService.searchSchema(userDetails, query, page, size, sort, direction));
    }

    @GetMapping("/chatbot")
    public ResponseEntity<ChatSearchResponse> searchChat(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "desc") String direction,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(searchService.searchChat(userDetails, query, page, size, sort, direction));
    }
}