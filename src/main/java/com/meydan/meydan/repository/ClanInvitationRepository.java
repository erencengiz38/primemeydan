package com.meydan.meydan.repository;

import com.meydan.meydan.models.entities.ClanInvitation;
import com.meydan.meydan.models.entities.ClanInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClanInvitationRepository extends JpaRepository<ClanInvitation, Long> {

    Optional<ClanInvitation> findByClanIdAndUserIdAndStatus(Long clanId, Long userId, ClanInvitationStatus status);

    List<ClanInvitation> findByClanIdAndStatus(Long clanId, ClanInvitationStatus status);

    List<ClanInvitation> findByUserIdAndStatus(Long userId, ClanInvitationStatus status);
}
