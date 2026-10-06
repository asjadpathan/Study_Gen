package com.asjad.studygen.repository;

import com.asjad.studygen.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResourceRepository extends JpaRepository<Resource, Long> {
    List<Resource> findByCategoryIgnoreCase(String category);
    List<Resource> findAllByOrderByCreatedAtDesc();
}
