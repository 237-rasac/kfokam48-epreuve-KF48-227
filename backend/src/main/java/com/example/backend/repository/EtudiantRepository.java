package com.example.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domaine.Etudiant;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    List<Etudiant> findByPromotionId(Long promotionId);
}
