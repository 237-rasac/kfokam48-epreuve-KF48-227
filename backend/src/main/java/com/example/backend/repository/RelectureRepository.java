package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domaine.Relecture;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    List<Relecture> findByRelecteurIdAndRendueAtIsNull(Long relecteurId);

    /** Relectures déjà rendues par un étudiant — écran relecteur, modification EF9/RG7. */
    List<Relecture> findByRelecteurIdAndRendueAtIsNotNull(Long relecteurId);

    boolean existsByExerciceId(Long exerciceId);

    boolean existsByExerciceIdAndRendueAtIsNotNull(Long exerciceId);

    java.util.Optional<Relecture> findByExerciceId(Long exerciceId);

    /** Relectures en cours (non rendues) d'une session, pour la répartition des relecteurs. */
    @org.springframework.data.jpa.repository.Query(
            "select r from Relecture r where r.exercice.session.id = :sessionId and r.rendueAt is null")
    List<Relecture> findBySessionIdAndRendueAtIsNull(@org.springframework.data.repository.query.Param("sessionId") Long sessionId);
}
