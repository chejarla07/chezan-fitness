package com.example.zuora.repository;

import com.example.zuora.model.WaiverContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WaiverContentRepository extends JpaRepository<WaiverContent, Long> {

    Optional<WaiverContent> findByVersion(String version);

    Optional<WaiverContent> findByActiveTrue();

    List<WaiverContent> findAllByOrderByCreatedAtDesc();

    List<WaiverContent> findByActiveTrueOrderByCreatedAtDesc();

    boolean existsByVersion(String version);
}