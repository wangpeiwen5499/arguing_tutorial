package com.arguing.controller;

import com.arguing.entity.Scene;
import com.arguing.exception.ApiException;
import com.arguing.service.SceneService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scenes")
public class SceneController {

    private final SceneService sceneService;

    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    @GetMapping
    public ResponseEntity<List<Scene>> listScenes(
            @RequestParam(required = false) String category) {
        List<Scene> scenes = sceneService.listScenes(category);
        return ResponseEntity.ok(scenes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Scene> getScene(@PathVariable Long id) {
        Scene scene = sceneService.getScene(id);
        return ResponseEntity.ok(scene);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getStatus().value());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }
}
