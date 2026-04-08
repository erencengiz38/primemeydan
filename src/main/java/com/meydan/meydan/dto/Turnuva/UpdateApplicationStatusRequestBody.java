package com.meydan.meydan.dto.Turnuva;

import com.meydan.meydan.models.enums.ApplicationStatus;
import lombok.Data;

@Data
public class UpdateApplicationStatusRequestBody {
    private ApplicationStatus status;
}