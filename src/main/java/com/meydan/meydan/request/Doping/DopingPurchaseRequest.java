package com.meydan.meydan.request.Doping;

import com.meydan.meydan.models.enums.TargetType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DopingPurchaseRequest {
    @NotNull(message = "Hedef ID (Klan, Organizasyon vb.) boş olamaz")
    private Long targetId;
    
    @NotNull(message = "Hedef tipi boş olamaz")
    private TargetType targetType;
    
    @NotNull(message = "Paket ID boş olamaz")
    private Long packageId;
}
