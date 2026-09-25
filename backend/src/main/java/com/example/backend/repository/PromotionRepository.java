package com.example.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.domaine.Promotion;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    Optional<Promotion> findByNom(String nom);
}
