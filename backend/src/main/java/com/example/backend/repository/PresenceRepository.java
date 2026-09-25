package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domaine.Presence;

public interface PresenceRepository extends JpaRepository<Presence, Long> {

    List<Presence> findBySessionId(Long sessionId);

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
