package com.texttosql.backend.controller;

import com.texttosql.backend.dto.support.SupportRequest;
import com.texttosql.backend.service.SupportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/support")
@RequiredArgsConstructor
@Tag(name = "5. Support", description = "Operations for user support and feedback.")
public class SupportController {

    private final SupportService supportService;

    @PostMapping("/contact")
    @Operation(summary = "Send a support contact message", description = "Sends an email to the administrator with the user's message.")
    public ResponseEntity<Void> contactSupport(@Valid @RequestBody SupportRequest request) {
        supportService.sendSupportEmail(request);
        return ResponseEntity.ok().build();
    }
}
