package com.meydan.meydan.service;

import com.meydan.meydan.request.Auth.Turnuva.AddTurnuvaRequestBody;
import com.meydan.meydan.request.Auth.Turnuva.UpdateTurnuvaRequestBody;
import com.meydan.meydan.exception.ErrorCode;
import com.meydan.meydan.exception.BaseException;
import com.meydan.meydan.models.entities.*;
import com.meydan.meydan.repository.*;
import com.meydan.meydan.request.Auth.Turnuva.ApplyToTournamentRequestBody;
import com.meydan.meydan.request.Auth.Turnuva.UpdateApplicationStatusRequestBody;
import com.meydan.meydan.util.SocialMediaValidator;
import com.meydan.meydan.util.XssSanitizer;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TurnuvaService {

    private static final Logger logger = LoggerFactory.getLogger(TurnuvaService.class);

    private final TurnuvaRepository turnuvaRepository;
    private final CategoryRepository categoryRepository;
    private final ClanRepository clanRepository;
    private final ClanMemberRepository clanMemberRepository;
    private final TournamentApplicationRepository tournamentApplicationRepository;
    private final TournamentApplicationPlayerRepository tournamentApplicationPlayerRepository;
    private final ModelMapper modelMapper;
    private final XssSanitizer xssSanitizer;
    private final SocialMediaValidator socialMediaValidator;


    public Turnuva createTurnuva(AddTurnuvaRequestBody addTurnuvaRequestBody, Long organizationId) {
        try {
            // Validation kontrolleri
            if (addTurnuvaRequestBody.getStart_date() != null && addTurnuvaRequestBody.getFinish_date() != null) {
                if (addTurnuvaRequestBody.getStart_date().after(addTurnuvaRequestBody.getFinish_date())) {
                    logger.warn("Tarih hatası: Başlangıç tarihi bitiş tarihinden sonra");
                    throw new BaseException(
                            ErrorCode.VAL_003,
                            "Başlangıç tarihi bitiş tarihinden sonra olamaz",
                            HttpStatus.BAD_REQUEST,
                            "start_date: " + addTurnuvaRequestBody.getStart_date() +
                                    " > finish_date: " + addTurnuvaRequestBody.getFinish_date()
                    );
                }
            }

            Turnuva turnuva = modelMapper.map(addTurnuvaRequestBody, Turnuva.class);
            
            // Category'yi bul ve set et
            Category category = categoryRepository.findById(addTurnuvaRequestBody.getCategoryId())
                    .orElseThrow(() -> new BaseException(
                            ErrorCode.VAL_001,
                            "Geçersiz kategori ID",
                            HttpStatus.BAD_REQUEST,
                            "CategoryId: " + addTurnuvaRequestBody.getCategoryId()
                    ));
            turnuva.setCategory(category);
            
            // XSS koruması - HTML taglarını temizle
            if (turnuva.getTitle() != null) {
                String originalTitle = turnuva.getTitle();
                turnuva.setTitle(xssSanitizer.sanitizeAndLimit(turnuva.getTitle(), 200));
                if (xssSanitizer.containsXss(originalTitle)) {
                    logger.warn("XSS saldırısı tespit edildi ve temizlendi. Title alanında HTML tagları kaldırıldı. OrganizationId: {}", organizationId);
                }
            }
            
            if (turnuva.getDescription() != null) {
                String originalDescription = turnuva.getDescription();
                turnuva.setDescription(xssSanitizer.sanitizeAndLimit(turnuva.getDescription(), 1000));
                if (xssSanitizer.containsXss(originalDescription)) {
                    logger.warn("XSS saldırısı tespit edildi ve temizlendi. Description alanında HTML tagları kaldırıldı. OrganizationId: {}", organizationId);
                }
            }
            
            if (turnuva.getLink() != null) {
                // Link alanında sadece temel URL formatına izin ver
                String originalLink = turnuva.getLink();
                turnuva.setLink(xssSanitizer.sanitizeBasic(turnuva.getLink().trim()));
                
                // Link'in geçerli bir sosyal ağ URL'si olup olmadığını kontrol et
                if (!turnuva.getLink().isEmpty()) {
                    if (!socialMediaValidator.isValidSocialMediaUrl(turnuva.getLink())) {
                        logger.warn("Geçersiz sosyal ağ URL'si. OrganizationId: {}, Link: {}", organizationId, originalLink);
                        throw new BaseException(
                                ErrorCode.LINK_001,
                                "Geçersiz sosyal ağ URL'si - Sadece Instagram, WhatsApp, Discord veya Telegram URL'leri desteklenir",
                                HttpStatus.BAD_REQUEST,
                                "Link: " + originalLink
                        );
                    }
                    
                    // Sosyal ağ türünü otomatik belirle
                    SocialMediaValidator.SocialMediaType linkType = socialMediaValidator.detectSocialMediaType(turnuva.getLink());
                    if (linkType != null) {
                        turnuva.setLink_type(linkType.getType());
                        logger.info("Link type otomatik belirlemeleri. OrganizationId: {}, Type: {}", organizationId, linkType.getType());
                    }
                }
            }
            
            // organizationId'yi token'dan gelen user ID'si olarak set et
            turnuva.setOrganizationId(organizationId);
            
            // isActive default olarak true
            if (turnuva.getIsActive() == null) {
                turnuva.setIsActive(true);
            }

            Turnuva saved = turnuvaRepository.save(turnuva);

            logger.info("Turnuva başarıyla oluşturuldu. ID: {}, OrganizationId: {}",
                    saved.getId(), saved.getOrganizationId());

            return saved;
        } catch (BaseException ex) {
            logger.error("Turnuva oluşturma hatası: {}, Details: {}", ex.getMessage(), ex.getDetails());
            throw ex;
        } catch (Exception e) {
            logger.error("Turnuva oluşturulurken beklenmeyen hata: {}", e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.TRN_005,
                    "Turnuva oluşturulurken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public List<Turnuva> getAllTurnuvas() {
        try {
            return turnuvaRepository.findAll();
        } catch (Exception e) {
            logger.error("Turnuvalar listelenirken hata oluştu: {}", e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.TRN_005,
                    "Turnuvalar listelenirken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public Page<Turnuva> getAllTurnuvasWithPagination(Pageable pageable) {
        try {
            return turnuvaRepository.findAll(pageable);
        } catch (Exception e) {
            logger.error("Turnuvalar listelenirken hata oluştu: {}", e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.TRN_005,
                    "Turnuvalar listelenirken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public Turnuva getTurnuvaById(Long id) {
        return turnuvaRepository.findById(id)
                .orElseThrow(() -> new BaseException(
                        ErrorCode.TRN_001,
                        "Turnuva bulunamadı",
                        HttpStatus.NOT_FOUND,
                        "ID: " + id
                ));
    }

    public List<Turnuva> getTurnuvasByOrganizationId(Long organizationId) {
        try {
            return turnuvaRepository.findByOrganizationId(organizationId);
        } catch (Exception e) {
            logger.error("OrganizationId {} için turnuvalar listelenirken hata: {}",
                    organizationId, e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.TRN_005,
                    "Turnuvalar listelenirken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public Page<Turnuva> getTurnuvasByOrganizationIdWithPagination(Long organizationId, Pageable pageable) {
        try {
            return turnuvaRepository.findByOrganizationId(organizationId, pageable);
        } catch (Exception e) {
            logger.error("OrganizationId {} için turnuvalar listelenirken hata: {}",
                    organizationId, e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.TRN_005,
                    "Turnuvalar listelenirken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public Turnuva updateTurnuva(UpdateTurnuvaRequestBody updateTurnuvaRequestBody, Long organizationId) {
        try {
            Turnuva turnuva = turnuvaRepository.findById(updateTurnuvaRequestBody.getId())
                    .orElseThrow(() -> new BaseException(
                            ErrorCode.TRN_001,
                            "Turnuva bulunamadı",
                            HttpStatus.NOT_FOUND,
                            "ID: " + updateTurnuvaRequestBody.getId()
                    ));

            // IDOR koruması: Kullanıcı sadece kendi turnuvalarını güncelleyebilir
            if (!turnuva.getOrganizationId().equals(organizationId)) {
                logger.warn("IDOR saldırısı tespit edildi. OrganizationId: {}, Turnuva OrganizationId: {}",
                        organizationId, turnuva.getOrganizationId());
                throw new BaseException(
                        ErrorCode.TRN_002,
                        "Bu turnuvayı güncellemek için yetkiniz yok",
                        HttpStatus.FORBIDDEN,
                        "OrganizationId: " + organizationId
                );
            }

            if (Boolean.FALSE.equals(turnuva.getIsActive())) {
                throw new BaseException(
                        ErrorCode.TRN_004,
                        "Silinmiş turnuva güncellenemez",
                        HttpStatus.BAD_REQUEST,
                        "ID: " + updateTurnuvaRequestBody.getId()
                );
            }

            // Tarih kontrolü
            if (updateTurnuvaRequestBody.getStart_date() != null && updateTurnuvaRequestBody.getFinish_date() != null) {
                if (updateTurnuvaRequestBody.getStart_date().after(updateTurnuvaRequestBody.getFinish_date())) {
                    logger.warn("Tarih hatası: Başlangıç tarihi bitiş tarihinden sonra");
                    throw new BaseException(
                            ErrorCode.VAL_003,
                            "Başlangıç tarihi bitiş tarihinden sonra olamaz",
                            HttpStatus.BAD_REQUEST,
                            "start_date: " + updateTurnuvaRequestBody.getStart_date() +
                                    " > finish_date: " + updateTurnuvaRequestBody.getFinish_date()
                    );
                }
            }

            // Category'yi bul ve set et
            if (updateTurnuvaRequestBody.getCategoryId() != null) {
                Category category = categoryRepository.findById(updateTurnuvaRequestBody.getCategoryId())
                        .orElseThrow(() -> new BaseException(
                                ErrorCode.VAL_001,
                                "Geçersiz kategori ID",
                                HttpStatus.BAD_REQUEST,
                                "CategoryId: " + updateTurnuvaRequestBody.getCategoryId()
                        ));
                turnuva.setCategory(category);
            }

            // Alanları güncelle
            turnuva.setTitle(updateTurnuvaRequestBody.getTitle());
            turnuva.setDescription(updateTurnuvaRequestBody.getDescription());
            turnuva.setStart_date(updateTurnuvaRequestBody.getStart_date());
            turnuva.setFinish_date(updateTurnuvaRequestBody.getFinish_date());
            if (updateTurnuvaRequestBody.getIsActive() != null) {
                turnuva.setIsActive(updateTurnuvaRequestBody.getIsActive());
            }
            if (updateTurnuvaRequestBody.getImageUrl() != null) {
                turnuva.setImageUrl(updateTurnuvaRequestBody.getImageUrl());
            }
            if (updateTurnuvaRequestBody.getReward_amount() != null) {
                turnuva.setReward_amount(updateTurnuvaRequestBody.getReward_amount());
            }
            if (updateTurnuvaRequestBody.getReward_currency() != null) {
                turnuva.setReward_currency(updateTurnuvaRequestBody.getReward_currency());
            }
            if (updateTurnuvaRequestBody.getPlayer_format() != null) {
                turnuva.setPlayer_format(updateTurnuvaRequestBody.getPlayer_format());
            }

            // XSS koruması
            if (turnuva.getTitle() != null) {
                String originalTitle = turnuva.getTitle();
                turnuva.setTitle(xssSanitizer.sanitizeAndLimit(turnuva.getTitle(), 200));
                if (xssSanitizer.containsXss(originalTitle)) {
                    logger.warn("XSS saldırısı tespit edildi ve temizlendi. Title alanında HTML tagları kaldırıldı. OrganizationId: {}", organizationId);
                }
            }

            if (turnuva.getDescription() != null) {
                String originalDescription = turnuva.getDescription();
                turnuva.setDescription(xssSanitizer.sanitizeAndLimit(turnuva.getDescription(), 1000));
                if (xssSanitizer.containsXss(originalDescription)) {
                    logger.warn("XSS saldırısı tespit edildi ve temizlendi. Description alanında HTML tagları kaldırıldı. OrganizationId: {}", organizationId);
                }
            }

            // Link güncelleme
            if (updateTurnuvaRequestBody.getLink() != null && !updateTurnuvaRequestBody.getLink().isEmpty()) {
                String originalLink = updateTurnuvaRequestBody.getLink();
                turnuva.setLink(xssSanitizer.sanitizeBasic(updateTurnuvaRequestBody.getLink().trim()));

                if (!socialMediaValidator.isValidSocialMediaUrl(turnuva.getLink())) {
                    logger.warn("Geçersiz sosyal ağ URL'si. OrganizationId: {}, Link: {}", organizationId, originalLink);
                    throw new BaseException(
                            ErrorCode.LINK_001,
                            "Geçersiz sosyal ağ URL'si - Sadece Instagram, WhatsApp, Discord veya Telegram URL'leri desteklenir",
                            HttpStatus.BAD_REQUEST,
                            "Link: " + originalLink
                    );
                }

                SocialMediaValidator.SocialMediaType linkType = socialMediaValidator.detectSocialMediaType(turnuva.getLink());
                if (linkType != null) {
                    turnuva.setLink_type(linkType.getType());
                }
            }

            Turnuva updated = turnuvaRepository.save(turnuva);
            logger.info("Turnuva başarıyla güncellendi. ID: {}, OrganizationId: {}", updated.getId(), organizationId);

            return updated;
        } catch (BaseException ex) {
            logger.error("Turnuva güncelleme hatası: {}, Details: {}", ex.getMessage(), ex.getDetails());
            throw ex;
        } catch (Exception e) {
            logger.error("Turnuva güncellenirken beklenmeyen hata: {}", e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.TRN_005,
                    "Turnuva güncellenirken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public Turnuva deleteTurnuva(Long id, Long organizationId) {
        try {
            Turnuva turnuva = turnuvaRepository.findById(id)
                    .orElseThrow(() -> new BaseException(
                            ErrorCode.TRN_001,
                            "Turnuva bulunamadı",
                            HttpStatus.NOT_FOUND,
                            "ID: " + id
                    ));

            // IDOR koruması
            if (!turnuva.getOrganizationId().equals(organizationId)) {
                logger.warn("IDOR saldırısı tespit edildi. OrganizationId: {}, Turnuva OrganizationId: {}",
                        organizationId, turnuva.getOrganizationId());
                throw new BaseException(
                        ErrorCode.TRN_002,
                        "Bu turnuvayı silmek için yetkiniz yok",
                        HttpStatus.FORBIDDEN,
                        "OrganizationId: " + organizationId
                );
            }

            if (Boolean.FALSE.equals(turnuva.getIsActive())) {
                throw new BaseException(
                        ErrorCode.TRN_003,
                        "Turnuva zaten silinmiş",
                        HttpStatus.BAD_REQUEST,
                        "ID: " + id
                );
            }

            turnuva.setIsActive(false);
            Turnuva deleted = turnuvaRepository.save(turnuva);
            logger.info("Turnuva başarıyla silindi. ID: {}, OrganizationId: {}", id, organizationId);

            return deleted;
        } catch (BaseException ex) {
            logger.error("Turnuva silme hatası: {}, Details: {}", ex.getMessage(), ex.getDetails());
            throw ex;
        } catch (Exception e) {
            logger.error("Turnuva silinirken beklenmeyen hata: {}", e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.TRN_005,
                    "Turnuva silinirken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public Turnuva restoreTurnuva(Long id, Long organizationId) {
        try {
            Turnuva turnuva = turnuvaRepository.findById(id)
                    .orElseThrow(() -> new BaseException(
                            ErrorCode.TRN_001,
                            "Turnuva bulunamadı",
                            HttpStatus.NOT_FOUND,
                            "ID: " + id
                    ));

            // IDOR koruması
            if (!turnuva.getOrganizationId().equals(organizationId)) {
                logger.warn("IDOR saldırısı tespit edildi. OrganizationId: {}, Turnuva OrganizationId: {}",
                        organizationId, turnuva.getOrganizationId());
                throw new BaseException(
                        ErrorCode.TRN_002,
                        "Bu turnuvayı geri yüklemek için yetkiniz yok",
                        HttpStatus.FORBIDDEN,
                        "OrganizationId: " + organizationId
                );
            }

            if (Boolean.TRUE.equals(turnuva.getIsActive())) {
                throw new BaseException(
                        ErrorCode.TRN_004,
                        "Turnuva zaten aktif",
                        HttpStatus.BAD_REQUEST,
                        "ID: " + id
                );
            }

            turnuva.setIsActive(true);
            Turnuva restored = turnuvaRepository.save(turnuva);
            logger.info("Turnuva başarıyla geri yüklendi. ID: {}, OrganizationId: {}", id, organizationId);

            return restored;
        } catch (BaseException ex) {
            logger.error("Turnuva geri yükleme hatası: {}, Details: {}", ex.getMessage(), ex.getDetails());
            throw ex;
        } catch (Exception e) {
            logger.error("Turnuva geri yüklenirken beklenmeyen hata: {}", e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.TRN_005,
                    "Turnuva geri yüklenirken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public Turnuva permanentlyDeleteTurnuva(Long id, Long organizationId) {
        try {
            Turnuva turnuva = turnuvaRepository.findById(id)
                    .orElseThrow(() -> new BaseException(
                            ErrorCode.TRN_001,
                            "Turnuva bulunamadı",
                            HttpStatus.NOT_FOUND,
                            "ID: " + id
                    ));

            // IDOR koruması
            if (!turnuva.getOrganizationId().equals(organizationId)) {
                logger.warn("IDOR saldırısı tespit edildi. OrganizationId: {}, Turnuva OrganizationId: {}",
                        organizationId, turnuva.getOrganizationId());
                throw new BaseException(
                        ErrorCode.TRN_002,
                        "Bu turnuvayı kalıcı olarak silmek için yetkiniz yok",
                        HttpStatus.FORBIDDEN,
                        "OrganizationId: " + organizationId
                );
            }

            turnuvaRepository.delete(turnuva);
            logger.info("Turnuva kalıcı olarak silindi. ID: {}, OrganizationId: {}", id, organizationId);

            return turnuva;
        } catch (BaseException ex) {
            logger.error("Turnuva kalıcı silme hatası: {}, Details: {}", ex.getMessage(), ex.getDetails());
            throw ex;
        } catch (Exception e) {
            logger.error("Turnuva kalıcı silinirken beklenmeyen hata: {}", e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.TRN_005,
                    "Turnuva kalıcı silinirken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public TournamentApplication applyToTournament(ApplyToTournamentRequestBody request, Long applicantUserId) {
        try {
            // Turnuva kontrolü
            Turnuva tournament = turnuvaRepository.findById(request.getTournamentId())
                    .orElseThrow(() -> new BaseException(
                            ErrorCode.TRN_001,
                            "Turnuva bulunamadı",
                            HttpStatus.NOT_FOUND,
                            "ID: " + request.getTournamentId()
                    ));

            if (!tournament.getIsActive()) {
                throw new BaseException(
                        ErrorCode.TRN_004,
                        "Bu turnuva aktif değil",
                        HttpStatus.BAD_REQUEST,
                        "TournamentId: " + request.getTournamentId()
                );
            }

            // Kullanıcının zaten başvurup başvurmadığını kontrol et
            if (tournamentApplicationRepository.existsByTournamentIdAndUserId(request.getTournamentId(), applicantUserId)) {
                throw new BaseException(
                        ErrorCode.APP_001,
                        "Bu turnuvaya zaten başvurdunuz",
                        HttpStatus.BAD_REQUEST,
                        "TournamentId: " + request.getTournamentId() + ", UserId: " + applicantUserId
                );
            }

            TournamentApplication application = new TournamentApplication();
            application.setTournament(tournament);
            application.setUserId(applicantUserId);
            application.setStatus(TournamentApplicationStatus.PENDING);

            // ParticipantType'a göre validasyon
            if (tournament.getParticipantType() == ParticipantType.SOLO) {
                // SOLO turnuvalar için clan opsiyonel (representation için)
                if (request.getClanId() != null) {
                    Clan clan = clanRepository.findById(request.getClanId())
                            .orElseThrow(() -> new RuntimeException("Clan bulunamadı"));

                    // Kullanıcının bu clan'da olup olmadığını kontrol et
                    clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), applicantUserId)
                            .orElseThrow(() -> new RuntimeException("Bu clan'ın üyesi değilsiniz"));

                    application.setClan(clan);
                }
            } else if (tournament.getParticipantType() == ParticipantType.CLAN) {
                // CLAN turnuvalar için clan zorunlu
                if (request.getClanId() == null) {
                    throw new BaseException(
                            ErrorCode.APP_002,
                            "Takım turnuvalarında clan seçimi zorunludur",
                            HttpStatus.BAD_REQUEST,
                            "TournamentId: " + request.getTournamentId()
                    );
                }

                Clan clan = clanRepository.findById(request.getClanId())
                        .orElseThrow(() -> new RuntimeException("Clan bulunamadı"));

                // Clan'ın turnuva kategorisi ile aynı olup olmadığını kontrol et
                if (!clan.getCategory().getId().equals(tournament.getCategory().getId())) {
                    throw new BaseException(
                            ErrorCode.APP_003,
                            "Clan'ın kategorisi turnuva kategorisi ile uyuşmuyor",
                            HttpStatus.BAD_REQUEST,
                            "Clan Category: " + clan.getCategory().getId() + ", Tournament Category: " + tournament.getCategory().getId()
                    );
                }

                // Kullanıcının bu clan'da OWNER veya TEAM_CAPTAIN olup olmadığını kontrol et
                ClanMember member = clanMemberRepository.findByClanIdAndUserIdAndIsActiveTrue(request.getClanId(), applicantUserId)
                        .orElseThrow(() -> new RuntimeException("Bu clan'ın üyesi değilsiniz"));

                if (member.getRole() != ClanMemberRole.OWNER && member.getRole() != ClanMemberRole.TEAM_CAPTAIN) {
                    throw new BaseException(
                            ErrorCode.APP_004,
                            "Sadece clan sahibi veya takım kaptanı turnuvaya başvurabilir",
                            HttpStatus.FORBIDDEN,
                            "User Role: " + member.getRole()
                    );
                }

                application.setClan(clan);

                // Seçilen oyuncuları doğrula ve kaydet
                if (request.getSelectedClanMemberIds() != null && !request.getSelectedClanMemberIds().isEmpty()) {
                    for (Long memberId : request.getSelectedClanMemberIds()) {
                        ClanMember selectedMember = clanMemberRepository.findByIdAndIsActiveTrue(memberId)
                                .orElseThrow(() -> new RuntimeException("Seçilen oyuncu bulunamadı: " + memberId));

                        // Seçilen oyuncunun aynı clan'da olup olmadığını kontrol et
                        if (!selectedMember.getClan().getId().equals(request.getClanId())) {
                            throw new RuntimeException("Seçilen oyuncu bu clan'da değil: " + memberId);
                        }

                        TournamentApplicationPlayer player = new TournamentApplicationPlayer();
                        player.setTournamentApplication(application);
                        player.setClanMemberId(memberId);
                        player.setUserId(selectedMember.getUserId());
                        application.getSelectedPlayers().add(player);
                    }
                }
            }

            TournamentApplication saved = tournamentApplicationRepository.save(application);
            logger.info("Turnuva başvurusu başarıyla oluşturuldu. TournamentId: {}, UserId: {}, ClanId: {}",
                    request.getTournamentId(), applicantUserId, request.getClanId());

            return saved;
        } catch (BaseException ex) {
            logger.error("Turnuva başvurusu hatası: {}, Details: {}", ex.getMessage(), ex.getDetails());
            throw ex;
        } catch (Exception e) {
            logger.error("Turnuva başvurusu oluşturulurken beklenmeyen hata: {}", e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.APP_005,
                    "Turnuva başvurusu oluşturulurken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public TournamentApplication updateApplicationStatus(Long applicationId, UpdateApplicationStatusRequestBody request, Long adminUserId) {
        try {
            TournamentApplication application = tournamentApplicationRepository.findById(applicationId)
                    .orElseThrow(() -> new BaseException(
                            ErrorCode.APP_006,
                            "Başvuru bulunamadı",
                            HttpStatus.NOT_FOUND,
                            "ApplicationId: " + applicationId
                    ));

            // Burada admin kontrolü yapılabilir - şimdilik basit tutuyorum
            application.setStatus(request.getStatus());
            application.setRejectionReason(request.getRejectionReason());
            application.setReviewedAt(java.time.LocalDateTime.now());

            TournamentApplication updated = tournamentApplicationRepository.save(application);
            logger.info("Başvuru durumu güncellendi. ApplicationId: {}, NewStatus: {}, AdminId: {}",
                    applicationId, request.getStatus(), adminUserId);

            return updated;
        } catch (BaseException ex) {
            logger.error("Başvuru durumu güncelleme hatası: {}, Details: {}", ex.getMessage(), ex.getDetails());
            throw ex;
        } catch (Exception e) {
            logger.error("Başvuru durumu güncellenirken beklenmeyen hata: {}", e.getMessage(), e);
            throw new BaseException(
                    ErrorCode.APP_005,
                    "Başvuru durumu güncellenirken bir hata oluştu.",
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    e.getMessage()
            );
        }
    }

    public List<TournamentApplication> getTournamentApplications(Long tournamentId) {
        return tournamentApplicationRepository.findByTournamentId(tournamentId);
    }

    public List<TournamentApplication> getUserApplications(Long userId) {
        return tournamentApplicationRepository.findByUserId(userId);
    }
}
