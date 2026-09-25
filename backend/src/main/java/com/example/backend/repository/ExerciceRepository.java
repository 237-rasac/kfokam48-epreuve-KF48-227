package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domaine.Exercice;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    List<Exercice> findBySessionId(Long sessionId);

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
