package com.example.backend.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.api.SessionDtos.SessionCreation;
import com.example.backend.api.SessionDtos.SessionDto;
import com.example.backend.domaine.SessionCours;
import com.example.backend.service.SessionService;

/**
 * API sessions (MODULE 2) : ouverture d'une session avec code de présence (EF1)
 * et consultation du détail.
 */
@RestController
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping("/api/sessions")
    public ResponseEntity<SessionDto> ouvrirSession(@RequestBody SessionCreation creation) {
        SessionCours session = sessionService.ouvrir(creation.titre(), creation.promotionId());
        return ResponseEntity.status(HttpStatus.CREATED).body(SessionDto.de(session));
    }

    @GetMapping("/api/sessions/{id}")
    public SessionDto getSession(@PathVariable Long id) {
        return SessionDto.de(sessionService.trouver(id));
    }

    /** MODULE 9 — EF8 : clôturer une session (200 / 404 SESSION_INCONNUE / 409 SESSION_DEJA_CLOTUREE). */
    @PostMapping("/api/sessions/{id}/cloture")
    public SessionDto cloturerSession(@PathVariable Long id) {
        return SessionDto.de(sessionService.cloturer(id));
    }
}
