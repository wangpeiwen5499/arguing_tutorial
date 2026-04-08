package com.arguing.repository;

import com.arguing.entity.Scene;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SceneRepository extends JpaRepository<Scene, Long> {

    List<Scene> findByCategory(String category);
}
