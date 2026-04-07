package com.meydan.meydan.dto.Error;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private int status;
    private String errorCode;
    private String message;
    private String error;
    private String details;
    private long timestamp;
}

