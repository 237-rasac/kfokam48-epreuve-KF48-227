package com.example.backend.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.backend.api.PresenceDtos.PresenceCreation;
import com.example.backend.api.PresenceDtos.PresenceDto;
import com.example.backend.domaine.Presence;
import com.example.backend.domaine.SourcePresence;
import com.example.backend.erreur.ErreurMetierException;
import com.example.backend.repository.PresenceRepository;
import com.example.backend.service.PresenceService;

/**
 * API présences (MODULES 3 et 4) : marquage par code (EF2, EF12) et ajout
 * manuel par le formateur (EF7/RG11), consultation des présences d'une session.
 */
@RestController
public class PresenceController {

    private final PresenceService presenceService;
    private final PresenceRepository presences;

    public PresenceController(PresenceService presenceService, PresenceRepository presences) {
        this.presenceService = presenceService;
        this.presences = presences;
    }

    @PostMapping("/api/presences")
    public ResponseEntity<PresenceDto> marquerPresence(@RequestBody PresenceCreation creation) {
        SourcePresence source = convertirSource(creation.source());
        Presence presence = presenceService.marquer(creation.code(), creation.etudiantId(), source);
        return ResponseEntity.status(HttpStatus.CREATED).body(PresenceDto.de(presence));
    }

    @GetMapping("/api/sessions/{id}/presences")
    public List<PresenceDto> getPresences(@PathVariable Long id) {
        return presences.findBySessionId(id).stream()
                .map(PresenceDto::de)
                .toList();
    }

    /** source est optionnel : absent ou ETUDIANT → voie étudiant, FORMATEUR → ajout manuel (EF7). */
    private SourcePresence convertirSource(String source) {
        if (source == null || source.isBlank() || "ETUDIANT".equals(source)) {
            return SourcePresence.ETUDIANT;
        }
        if ("FORMATEUR".equals(source)) {
            return SourcePresence.FORMATEUR;
        }
        throw new ErreurMetierException("CHAMP_MANQUANT", 400,
                "source doit valoir ETUDIANT ou FORMATEUR.");
    }
}
