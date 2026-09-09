package com.bankapp.banking.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private boolean success;
    private String message;
    private LocalDateTime timestamp = LocalDateTime.now();

    public ApiResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}
