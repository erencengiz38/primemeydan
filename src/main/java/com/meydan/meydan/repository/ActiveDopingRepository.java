package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.ActiveDoping;
import com.meydan.meydan.models.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActiveDopingRepository extends JpaRepository<ActiveDoping, Long> {
    
    // Belirli bir hedefin aktif dopinglerini getir
    List<ActiveDoping> findByTargetTypeAndTargetIdAndIsActiveTrue(TargetType targetType, Long targetId);
    
    // Süresi dolan aktif dopingleri bulmak için
    @Query("SELECT ad FROM ActiveDoping ad WHERE ad.isActive = true AND ad.endDate <= :currentTime")
    List<ActiveDoping> findExpiredDopings(@Param("currentTime") LocalDateTime currentTime);

    // Toplu pasife çekme işlemi (Performans için)
    @Modifying
    @Query("UPDATE ActiveDoping ad SET ad.isActive = false WHERE ad.isActive = true AND ad.endDate <= :currentTime")
    int deactivateExpiredDopings(@Param("currentTime") LocalDateTime currentTime);
    
    // Kullanıcının aldığı aktif dopingleri listelemek için
    List<ActiveDoping> findByUserIdAndIsActiveTrue(Long userId);
}
