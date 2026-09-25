package com.example.backend.api;

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
import com.example.backend.repository.PresenceRepository;
import com.example.backend.service.PresenceService;

/**
 * API présences (MODULE 3) : marquage par code (EF2, EF12) et consultation
 * des présences d'une session (utile au tableau, MODULE 8).
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
        Presence presence = presenceService.marquer(creation.code(), creation.etudiantId());
        return ResponseEntity.status(HttpStatus.CREATED).body(PresenceDto.de(presence));
    }

    @GetMapping("/api/sessions/{id}/presences")
    public java.util.List<PresenceDto> getPresences(@PathVariable Long id) {
        return presences.findBySessionId(id).stream()
                .map(PresenceDto::de)
                .toList();
    }
}
