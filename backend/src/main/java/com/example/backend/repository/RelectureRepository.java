package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domaine.Relecture;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    List<Relecture> findByRelecteurIdAndRendueAtIsNull(Long relecteurId);

    boolean existsByExerciceId(Long exerciceId);
}
