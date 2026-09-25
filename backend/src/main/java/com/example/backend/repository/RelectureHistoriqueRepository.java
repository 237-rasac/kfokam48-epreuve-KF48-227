package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domaine.RelectureHistorique;

public interface RelectureHistoriqueRepository extends JpaRepository<RelectureHistorique, Long> {

    List<RelectureHistorique> findByRelectureIdOrderByModifieAtAsc(Long relectureId);
}
