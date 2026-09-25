package com.example.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.backend.domaine.Promotion;
import com.example.backend.domaine.SessionCours;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.PromotionRepository;
import com.example.backend.repository.SessionCoursRepository;
import com.example.backend.service.GenerateurCode;
import com.example.backend.service.HorlogeMetier;
import com.example.backend.service.SessionService;

/**
 * Test unitaire imposé sur RG1 (cahier des charges §8) : sans Spring ni base,
 * avec une horloge figée, expirationAt vaut exactement ouvertureAt + 15 minutes.
 */
class SessionServiceTest {

    private static final LocalDateTime OUVERTURE = LocalDateTime.of(2026, 3, 12, 8, 0, 0);

    private SessionCoursRepository sessions;
    private SessionService service;

    @BeforeEach
    void preparer() {
        sessions = mock(SessionCoursRepository.class);
        PromotionRepository promotions = mock(PromotionRepository.class);
        when(promotions.findById(1L)).thenReturn(Optional.of(new Promotion("KFOKAM48")));
        when(promotions.findById(999L)).thenReturn(Optional.empty());
        when(sessions.findAll()).thenReturn(List.of());
        when(sessions.save(any(SessionCours.class))).thenAnswer(appel -> appel.getArgument(0));

        service = new SessionService(sessions, promotions, HorlogeMetier.figee(OUVERTURE), new GenerateurCode());
    }

    @Test
    void rg1LeCodeExpireExactementQuinzeMinutesApresOuverture() {
        SessionCours session = service.ouvrir("Cours Java", 1L);

        assertThat(session.getOuvertureAt()).isEqualTo(OUVERTURE);
        assertThat(session.getExpirationAt()).isEqualTo(LocalDateTime.of(2026, 3, 12, 8, 15, 0));
        assertThat(session.getClotureAt()).isNull();
        assertThat(session.getCode()).hasSize(6);
    }

    @Test
    void leTitreEstNettoyeEtObligatoire() {
        assertThat(service.ouvrir("  Cours Java  ", 1L).getTitre()).isEqualTo("Cours Java");

        assertThatThrownBy(() -> service.ouvrir("   ", 1L))
                .isInstanceOf(ErreurMetierException.class)
                .extracting("code").isEqualTo("CHAMP_MANQUANT");
    }

    @Test
    void promotionInconnueLeve404() {
        assertThatThrownBy(() -> service.ouvrir("Cours Java", 999L))
                .isInstanceOf(ErreurMetierException.class)
                .extracting("status").isEqualTo(404);
    }
}
