package com.meydan.meydan.controller;

import com.meydan.meydan.dto.ApiResponse;
import com.meydan.meydan.models.entities.User;
import com.meydan.meydan.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "Kullanıcı API endpoint'leri")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/available-for-clan")
    @Operation(summary = "Klansız kullanıcıları listele", description = "Belirli bir oyun kategorisinde herhangi bir klanı olmayan kullanıcıları sayfalı olarak getirir.")
    public ResponseEntity<ApiResponse<Page<User>>> getAvailableUsersForClan(
            @RequestParam Long categoryId,
            Pageable pageable) {
        Page<User> users = userService.findAvailableUsersForClan(categoryId, pageable);
        return ResponseEntity.ok(new ApiResponse<>(true, "Klana eklenebilir kullanıcılar başarıyla getirildi.", users));
    }
}
