package com.asjad.studygen.repository;

import com.asjad.studygen.entity.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findTop10ByOrderByCreatedAtDesc();
}
