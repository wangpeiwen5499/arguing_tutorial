package com.arguing.service;

import com.arguing.entity.Scene;
import com.arguing.exception.ApiException;
import com.arguing.repository.SceneRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SceneService {

    private final SceneRepository sceneRepository;

    public SceneService(SceneRepository sceneRepository) {
        this.sceneRepository = sceneRepository;
    }

    public List<Scene> listScenes(String category) {
        if (category != null && !category.isEmpty()) {
            return sceneRepository.findByCategory(category);
        }
        return sceneRepository.findAll();
    }

    public Scene getScene(Long id) {
        return sceneRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "场景不存在"));
    }
}
