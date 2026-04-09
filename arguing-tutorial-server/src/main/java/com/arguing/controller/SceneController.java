package com.arguing.controller;

import com.arguing.entity.Scene;
import com.arguing.exception.ApiException;
import com.arguing.service.SceneService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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

    /**
     * 创建自定义场景。
     * 用户传入 name + description + opponentDescription，由 LLM 生成场景设定。
     */
    @PostMapping("/custom")
    public ResponseEntity<Scene> createCustomScene(
            @RequestBody CustomSceneRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "场景名称不能为空");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "场景描述不能为空");
        }
        if (request.getOpponentDescription() == null || request.getOpponentDescription().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "对手描述不能为空");
        }

        Scene scene = sceneService.createCustomScene(
                request.getName(),
                request.getDescription(),
                request.getOpponentDescription()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(scene);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> handleApiException(ApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", ex.getStatus().value());
        body.put("message", ex.getMessage());
        return ResponseEntity.status(ex.getStatus()).body(body);
    }

    /**
     * 自定义场景创建请求体。
     */
    public static class CustomSceneRequest {
        @NotBlank(message = "场景名称不能为空")
        private String name;

        @NotBlank(message = "场景描述不能为空")
        private String description;

        @NotBlank(message = "对手描述不能为空")
        private String opponentDescription;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getOpponentDescription() {
            return opponentDescription;
        }

        public void setOpponentDescription(String opponentDescription) {
            this.opponentDescription = opponentDescription;
        }
    }
}
