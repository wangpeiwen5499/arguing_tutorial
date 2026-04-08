# 程序员吵架修炼场 MVP 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个微信小程序 MVP，让用户可以选择预设场景与 AI 数字人进行语音对练，结束后获得复盘分析报告。

**Architecture:** 前后端分离。uni-app 小程序前端通过 REST API 调用 Spring Boot 后端。后端统一管理 AI（LLM + ASR + TTS）、数据库、数字人驱动数据生成。前端负责录音/播放、Canvas 2D 数字人渲染、页面交互。

**Tech Stack:** uni-app (Vue 3) + Spring Boot 3 + MySQL + Redis + 阿里云/腾讯云 ASR&TTS + 大模型 API

---

## 项目结构

### 后端 (arguing-tutorial-server/)

```
arguing-tutorial-server/
├── pom.xml
├── src/main/java/com/arguing/
│   ├── ArguingTutorialApplication.java
│   ├── config/
│   │   ├── WebConfig.java              # CORS、拦截器配置
│   │   ├── RedisConfig.java            # Redis 序列化配置
│   │   └── AiConfig.java               # AI 服务连接配置
│   ├── controller/
│   │   ├── SceneController.java        # 场景相关 API
│   │   ├── SessionController.java      # 对练相关 API
│   │   ├── ReportController.java       # 复盘报告 API
│   │   ├── AuthController.java         # 登录认证 API
│   │   └── UserController.java         # 用户信息 API
│   ├── entity/
│   │   ├── Scene.java
│   │   ├── Session.java
│   │   ├── Round.java
│   │   ├── Report.java
│   │   └── User.java
│   ├── dto/
│   │   ├── ChatRequest.java            # 对练请求（含音频）
│   │   ├── ChatResponse.java           # AI 回复（含语音+表情指令）
│   │   ├── ReportView.java             # 复盘报告视图
│   │   └── ShareCardResponse.java      # 分享卡片
│   ├── repository/
│   │   ├── SceneRepository.java
│   │   ├── SessionRepository.java
│   │   ├── RoundRepository.java
│   │   ├── ReportRepository.java
│   │   └── UserRepository.java
│   ├── service/
│   │   ├── SceneService.java
│   │   ├── SessionService.java
│   │   ├── SpeechService.java          # ASR + TTS 封装
│   │   ├── AiService.java              # 大模型调用封装
│   │   ├── AvatarService.java          # 数字人驱动数据生成
│   │   ├── AnalysisService.java        # 复盘分析
│   │   ├── AuthService.java            # 认证服务
│   │   ├── ShareService.java           # 分享卡片生成
│   │   └── ContentSafetyService.java   # 内容安全
│   ├── service/prompt/
│   │   ├── RolePlayPromptBuilder.java   # 角色扮演 Prompt 构建
│   │   ├── AnalysisPromptBuilder.java   # 复盘分析 Prompt 构建
│   │   └── HintPromptBuilder.java       # 策略提示 Prompt 构建
│   └── common/
│       ├── GuestInterceptor.java       # 游客 token 拦截器
│       ├── RateLimitService.java       # 频率限制
│       └── ApiException.java           # 统一异常
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   └── db/migration/
│       └── V1__init_schema.sql
└── src/test/java/com/arguing/
    ├── service/
    │   ├── SceneServiceTest.java
    │   ├── SessionServiceTest.java
    │   ├── AnalysisServiceTest.java
    │   └── SpeechServiceTest.java
    └── controller/
        ├── SceneControllerTest.java
        └── SessionControllerTest.java
```

### 前端 (arguing-tutorial-client/)

```
arguing-tutorial-client/
├── package.json
├── vite.config.js
├── src/
│   ├── App.vue
│   ├── main.js
│   ├── manifest.json
│   ├── pages.json                    # 页面路由配置
│   ├── api/
│   │   ├── index.js                  # axios 实例 + 拦截器
│   │   ├── scene.js                  # 场景 API
│   │   ├── session.js                # 对练 API
│   │   ├── report.js                 # 复盘 API
│   │   └── user.js                   # 用户 API
│   ├── pages/
│   │   ├── index/
│   │   │   └── index.vue             # 首页（场景列表）
│   │   ├── practice/
│   │   │   └── index.vue             # 沉浸式对练页
│   │   ├── report/
│   │   │   └── index.vue             # 复盘报告页
│   │   └── profile/
│   │       └── index.vue             # 个人中心
│   ├── components/
│   │   ├── SceneCard.vue             # 场景卡片
│   │   ├── AvatarRenderer.vue        # 数字人渲染组件
│   │   ├── VoiceRecorder.vue         # 语音录制组件
│   │   ├── AudioPlayer.vue           # 音频播放组件
│   │   ├── ScoreRadar.vue            # 评分雷达图
│   │   └── ShareCard.vue             # 分享卡片
│   ├── composables/
│   │   ├── useRecording.js           # 录音逻辑
│   │   ├── useAvatar.js              # 数字人驱动逻辑
│   │   └── useGuest.js               # 游客 token 管理
│   ├── store/
│   │   └── user.js                   # 用户状态（Pinia）
│   ├── utils/
│   │   └── audio.js                  # 音频工具函数
│   └── static/
│       └── images/                   # 静态图片资源
└── tsconfig.json
```

---

## Task 1: 后端项目初始化与数据库 Schema

**Files:**
- Create: `arguing-tutorial-server/pom.xml`
- Create: `arguing-tutorial-server/src/main/resources/application.yml`
- Create: `arguing-tutorial-server/src/main/resources/application-dev.yml`
- Create: `arguing-tutorial-server/src/main/resources/db/migration/V1__init_schema.sql`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/ArguingTutorialApplication.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/config/WebConfig.java`

- [ ] **Step 1: 使用 Spring Initializr 创建项目骨架**

```bash
cd D:/code/arguing_tutorial
mkdir -p arguing-tutorial-server
```

创建 `pom.xml`，引入依赖：Spring Boot 3.2, Spring Web, Spring Data JPA, MySQL Driver, Lombok, Jackson。

- [ ] **Step 2: 编写 application.yml 配置**

数据库连接、服务端口（8080）、JPA 配置（ddl-auto: validate）。

- [ ] **Step 3: 编写数据库初始化 SQL**

```sql
-- V1__init_schema.sql
CREATE TABLE scene (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(20) NOT NULL COMMENT '职场/技术/日常',
    difficulty INT NOT NULL DEFAULT 3 COMMENT '1-5星',
    background_config JSON COMMENT '背景图/氛围配置',
    avatar_config JSON COMMENT '数字人模型ID+外观参数',
    personality VARCHAR(500) COMMENT '对手性格设定',
    opening_line VARCHAR(500) COMMENT '开场白',
    evaluation_criteria JSON COMMENT '评分权重覆盖',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    wx_openid VARCHAR(64) UNIQUE,
    wx_unionid VARCHAR(64),
    nickname VARCHAR(50),
    avatar_url VARCHAR(500),
    guest_token VARCHAR(64) UNIQUE COMMENT '游客临时token',
    is_guest BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    scene_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/COMPLETED/ABANDONED',
    total_rounds INT NOT NULL DEFAULT 10,
    current_round INT NOT NULL DEFAULT 0,
    hint_used_count INT NOT NULL DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME,
    FOREIGN KEY (user_id) REFERENCES user(id),
    FOREIGN KEY (scene_id) REFERENCES scene(id)
);

CREATE TABLE round (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    round_number INT NOT NULL,
    user_audio_url VARCHAR(500),
    user_text TEXT,
    ai_text TEXT,
    ai_audio_url VARCHAR(500),
    ai_emotion VARCHAR(20) COMMENT 'angry/sarcastic/hesitant/compromising/confident',
    ai_expression JSON COMMENT '表情/动作指令',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES session(id)
);

CREATE TABLE report (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL UNIQUE,
    total_score INT NOT NULL COMMENT '0-100',
    logic_score INT NOT NULL,
    emotion_score INT NOT NULL,
    persuasion_score INT NOT NULL,
    strategy_score INT NOT NULL,
    clarity_score INT NOT NULL,
    strengths JSON COMMENT '做得好的点',
    improvements JSON COMMENT '可改进的点',
    round_reviews JSON COMMENT '每轮点评',
    share_card_url VARCHAR(500),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (session_id) REFERENCES session(id)
);

-- 预置场景种子数据
INSERT INTO scene (name, description, category, difficulty, background_config, avatar_config, personality, opening_line, evaluation_criteria) VALUES
('谈加薪', '你决定找老板谈涨工资，需要说服对方认可你的价值', '职场', 3,
 '{"type":"office","bgColor":"#1a1a2e","elements":["bookshelf","window"]}',
 '{"modelId":"boss_01","clothing":"suit","accessories":"glasses"}',
 '强硬，精于算计，不会轻易让步，善于用公司利益压人',
 '你来了？坐吧。最近工作怎么样？', NULL),
('怼不合理需求', 'PM 提了一个明显不合理的需求，你需要在会议上说服大家', '技术', 2,
 '{"type":"open_office","bgColor":"#2d3436","elements":["monitor","whiteboard"]}',
 '{"modelId":"pm_01","clothing":"casual","accessories":"none"}',
 '固执，只关心 KPI 和排期，不懂技术细节但自信满满',
 '这个需求很简单吧，就加个按钮的事，下周五能上吗？', NULL),
('Code Review 辩论', '同事对你的代码提出了质疑，你认为自己的方案更好', '技术', 4,
 '{"type":"meeting_room","bgColor":"#2c3e50","elements":["projector","whiteboard"]}',
 '{"modelId":"dev_01","clothing":"tshirt","accessories":"macbook"}',
 '技术能力强但固执，喜欢用反问和质疑施压，不会轻易认错',
 '这段代码的可读性不太好，为什么要用这种实现方式？', NULL),
('跨部门资源争夺', '你需要从其他部门争取更多资源支持你的项目', '职场', 4,
 '{"type":"meeting_room","bgColor":"#1e3a5f","elements":["projector","coffee"]}',
 '{"modelId":"manager_01","clothing":"shirt","accessories":"none"}',
 '老练，深谙公司政治，善于转移话题和踢皮球',
 '你们项目的重要性我们理解，但 Q4 我们的资源也紧张', NULL),
('日常技术选型争论', '团队在讨论技术方案，你支持的技术栈被质疑', '技术', 3,
 '{"type":"open_office","bgColor":"#2d3436","elements":["monitor","coffee"]}',
 '{"modelId":"dev_02","clothing":"hoodie","accessories":"headphones"}',
 '有主见但不恶意，喜欢用案例和最佳实践来论证',
 '说实话，这个方案在大型项目里踩过不少坑', NULL);
```

- [ ] **Step 4: 创建 Application 启动类和 CORS 配置**

`ArguingTutorialApplication.java` + `WebConfig.java`（允许小程序跨域）。

- [ ] **Step 5: 启动项目验证数据库连接**

Run: `cd arguing-tutorial-server && mvn spring-boot:run`
Expected: 应用启动成功，日志显示数据库连接正常。

- [ ] **Step 6: Commit**

```bash
git add arguing-tutorial-server/
git commit -m "feat(server): init Spring Boot project with DB schema and seed data"
```

---

## Task 2: 实体类与 Repository 层

**Files:**
- Create: `arguing-tutorial-server/src/main/java/com/arguing/entity/Scene.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/entity/User.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/entity/Session.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/entity/Round.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/entity/Report.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/repository/*.java`

- [ ] **Step 1: 编写 Scene 实体**

```java
@Entity
@Table(name = "scene")
@Data
public class Scene {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    private String category;
    private Integer difficulty;
    @Column(columnDefinition = "JSON")
    private String backgroundConfig;
    @Column(columnDefinition = "JSON")
    private String avatarConfig;
    private String personality;
    private String openingLine;
    @Column(columnDefinition = "JSON")
    private String evaluationCriteria;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 编写 User、Session、Round、Report 实体**

按 spec 数据模型定义字段，使用 JPA 注解映射。Session 的 status 用枚举类型。

- [ ] **Step 3: 编写 Repository 接口**

```java
public interface SceneRepository extends JpaRepository<Scene, Long> {
    List<Scene> findByCategory(String category);
}
public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByUserIdOrderByCreatedAtDesc(Long userId);
}
// ... 其余类似
```

- [ ] **Step 4: 编写 Repository 测试**

使用 `@DataJpaTest` 测试 Scene 的基本 CRUD 和按分类查询。

- [ ] **Step 5: Run tests**

Run: `mvn test -pl arguing-tutorial-server`
Expected: 全部通过。

- [ ] **Step 6: Commit**

```bash
git add arguing-tutorial-server/src/main/java/com/arguing/entity/
git add arguing-tutorial-server/src/main/java/com/arguing/repository/
git commit -m "feat(server): add entity classes and repositories"
```

---

## Task 3: 场景服务 — API 层

**Files:**
- Create: `arguing-tutorial-server/src/main/java/com/arguing/controller/SceneController.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/SceneService.java`
- Create: `arguing-tutorial-server/src/test/java/com/arguing/controller/SceneControllerTest.java`

- [ ] **Step 1: 编写 SceneService**

```java
@Service
public class SceneService {
    private final SceneRepository sceneRepository;
    public List<Scene> listScenes(String category) { ... }
    public Scene getScene(Long id) { ... }
}
```

- [ ] **Step 2: 编写 SceneController**

```
GET /api/scenes?category=职场       → 场景列表
GET /api/scenes/{id}               → 场景详情
```

- [ ] **Step 3: 编写 SceneController 测试**

使用 `@WebMvcTest` 测试场景列表和详情接口，Mock SceneService。

- [ ] **Step 4: Run tests**

Run: `mvn test -pl arguing-tutorial-server -Dtest=SceneControllerTest`
Expected: 通过。

- [ ] **Step 5: 启动服务，手动验证 API**

Run: `mvn spring-boot:run`
Test: `curl http://localhost:8080/api/scenes`
Expected: 返回 5 条预置场景数据。

- [ ] **Step 6: Commit**

```bash
git add arguing-tutorial-server/
git commit -m "feat(server): add scene list and detail API"
```

---

## Task 3.5: 自定义场景创建 API

**Files:**
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/prompt/CustomScenePromptBuilder.java`
- Modify: `arguing-tutorial-server/src/main/java/com/arguing/controller/SceneController.java`
- Modify: `arguing-tutorial-server/src/main/java/com/arguing/service/SceneService.java`

- [ ] **Step 1: 编写 CustomScenePromptBuilder**

构建自定义场景生成 Prompt：用户传入 `name` + `description` + `opponent_description`，由 LLM 生成：
```json
{
  "personality": "强硬但讲道理，善于用数据反驳...",
  "opening_line": "你就是那个后端开发？我听说你们接口写得一塌糊涂",
  "difficulty": 3
}
```

- [ ] **Step 2: 在 SceneService 添加 createCustomScene 方法**

1. 调用 LLM 生成 personality、opening_line、difficulty
2. 根据 `opponent_description` 关键词匹配预制模型标签选择 avatar_config
3. 使用默认 evaluation_criteria 权重
4. 保存并返回 Scene

- [ ] **Step 3: 在 SceneController 添加自定义场景端点**

```
POST /api/scenes/custom
Body: { "name": "...", "description": "...", "opponentDescription": "..." }
```

- [ ] **Step 4: 编写测试**

测试关键词匹配逻辑（"强势"→boss_01, "年轻"→pm_01 等）。Mock LLM 调用。

- [ ] **Step 5: Run tests**

Run: `mvn test -Dtest=SceneServiceTest`
Expected: 通过。

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat(server): add custom scene creation with LLM generation"
```

---

## Task 4: 游客认证与会话管理

**Files:**
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/AuthService.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/controller/AuthController.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/common/GuestInterceptor.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/common/RateLimitService.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/config/RedisConfig.java`
- Modify: `arguing-tutorial-server/pom.xml` (添加 Redis 依赖)

> **依赖：** Task 1 (项目骨架) + Task 2 (User 实体)

- [ ] **Step 1: 添加 Redis 依赖到 pom.xml**

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

- [ ] **Step 2: 编写 AuthService**

核心逻辑：
- `ensureGuest(String guestToken)` → 若 token 无对应用户则创建游客用户（UUID token），返回用户
- `loginByWx(String code)` → 调用微信 API 换取 openid，创建/查找正式用户
- `upgradeGuest(Long guestUserId, String wxCode)` → 游客转正式用户，迁移会话

- [ ] **Step 3: 编写 GuestInterceptor**

从请求头 `X-Guest-Token` 获取游客 token，通过 `AuthService.ensureGuest` 确保用户存在，将 userId 注入请求属性。

- [ ] **Step 4: 编写 RateLimitService**

使用 Redis 计数器实现游客每日 5 次限制。key: `rate:{userId}:{date}`，TTL 到当天结束。

- [ ] **Step 5: 编写 AuthController**

```
POST /api/auth/guest       → 获取/刷新游客 token
POST /api/auth/wx-login    → 微信登录（code 换 token）
```

- [ ] **Step 6: 编写测试**

- Create: `arguing-tutorial-server/src/test/java/com/arguing/service/AuthServiceTest.java`

```java
// testEnsureGuest_newToken_createsUser()
// testEnsureGuest_existingToken_returnsExistingUser()
// testUpgradeGuest_migratesSessions()
```

- Create: `arguing-tutorial-server/src/test/java/com/arguing/service/RateLimitServiceTest.java`

```java
// testGuestRateLimit_underLimit_allows()
// testGuestRateLimit_overLimit_rejects()
// testGuestRateLimit_resetsNextDay()
```

- [ ] **Step 7: Run tests**

Run: `mvn test -Dtest=AuthServiceTest,RateLimitServiceTest`
Expected: 通过。

- [ ] **Step 8: Commit**

```bash
git add .
git commit -m "feat(server): add guest auth, WeChat login, rate limiting"
```

---

## Task 5: 对练核心 — 会话与轮次管理

**Files:**
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/SessionService.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/controller/SessionController.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/dto/ChatRequest.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/dto/ChatResponse.java`
- Test: `arguing-tutorial-server/src/test/java/com/arguing/service/SessionServiceTest.java`

- [ ] **Step 1: 编写 SessionService 核心逻辑**

```java
@Service
public class SessionService {
    // startSession(userId, sceneId) → 创建 Session，返回 AI 开场白
    // chat(userId, sessionId, audioFile) → 核心对练流程
    // requestHint(userId, sessionId) → 策略提示
    // endSession(userId, sessionId) → 结束对练，触发分析
}
```

`startSession`: 创建 Session 记录，用 scene 的 `openingLine` 作为第一轮 AI 消息。

`chat` 方法先留桩（Task 6 接入 AI 后补全）：
1. 校验 Session 状态和轮次
2. 保存音频文件
3. 调用 ASR（先 mock）
4. 调用 AI（先 mock）
5. 调用 TTS（先 mock）
6. 保存 Round 记录
7. 返回 ChatResponse

- [ ] **Step 2: 编写 SessionController**

```
POST /api/sessions              → 开始对练
POST /api/sessions/{id}/chat    → 发送语音，返回 AI 回复
POST /api/sessions/{id}/hint    → 请求策略提示
POST /api/sessions/{id}/end     → 结束对练
```

- [ ] **Step 3: 编写 DTO**

ChatRequest: `MultipartFile audio`（sessionId 来自 URL 路径参数）
ChatResponse: `String text`, `String audioUrl`, `String emotion`, `Object expression`, `Integer currentRound`, `Integer totalRounds`

- [ ] **Step 4: 编写 SessionService 测试**

测试：
- 开始会话 → 返回开场白
- 超过 10 轮 → 抛异常
- 重复结束 → 幂等
- 提示超过 3 次 → 拒绝

- [ ] **Step 5: Run tests**

Run: `mvn test -Dtest=SessionServiceTest`
Expected: 通过。

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat(server): add session and round management with mock AI"
```

---

## Task 6: AI 服务集成 — LLM + ASR + TTS

**Files:**
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/SpeechService.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/AiService.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/ContentSafetyService.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/prompt/RolePlayPromptBuilder.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/prompt/HintPromptBuilder.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/config/AiConfig.java`
- Modify: `application.yml` (添加 AI 服务配置)

- [ ] **Step 1: 编写 AiConfig**

配置大模型 API 的 endpoint、api-key、model-name（从环境变量读取）。

- [ ] **Step 2: 编写 AiService**

```java
@Service
public class AiService {
    // chat(messages) → 调用大模型 API，返回回复文本
    // 分析调用：analysisChat(messages) → 结构化输出
}
```

使用 RestTemplate 或 WebClient 调用大模型 HTTP API。

- [ ] **Step 3: 编写 RolePlayPromptBuilder**

构建角色扮演对话的 Prompt 模板：
- System Prompt：角色设定 + 规则（不生成有害内容、在指定轮次推动结论）+ 输出格式要求（回复内容 + emotion 标记）
- 将历史轮次转为 messages 数组
- 附加当前轮次信息（第 N/10 轮）

输出格式要求 AI 返回 JSON：
```json
{"reply": "...", "emotion": "angry|sarcastic|hesitant|compromising|confident"}
```

- [ ] **Step 4: 编写 SpeechService**

```java
@Service
public class SpeechService {
    // recognize(audioFile) → ASR 转文字
    // synthesize(text) → TTS 生成语音文件，返回 URL
    // getPhonemeTimestamps(text) → 返回音素时间戳（用于口型驱动）
}
```

先实现接口定义 + Mock 实现（返回固定文本/预录音频），后续接入真实 ASR/TTS 服务。

- [ ] **Step 5: 编写 ContentSafetyService**

```java
@Service
public class ContentSafetyService {
    // filter(text) → 敏感词过滤，返回清洗后文本
    // audit(text) → 审核 AI 输出是否安全
}
```

初期可用简单敏感词库，后续接入云服务内容安全 API。

- [ ] **Step 6: 编写 HintPromptBuilder**

基于当前对话上下文，构建策略提示 Prompt。要求 AI 返回一个建议策略名称 + 一句话说明。

- [ ] **Step 7: 将 AI 服务接入 SessionService.chat()**

替换之前的 mock 实现，完整接入 ASR → 内容安全 → AI 对话 → 内容审核 → TTS 流程。

- [ ] **Step 8: 编写集成测试**

- Create: `arguing-tutorial-server/src/test/java/com/arguing/service/SpeechServiceTest.java`

```java
// testRecognize_returnsText (mock ASR)
// testSynthesize_returnsAudioUrl (mock TTS)
// testRecognize_emptyAudio_throwsException()
```

- Create: `arguing-tutorial-server/src/test/java/com/arguing/service/SessionServiceIntegrationTest.java`

```java
// testFullChatFlow_mockedServices() — 完整对练流程，mock ASR/TTS/LLM
// testChatFlow_llmTimeout_retries() — LLM 超时重试
// testChatFlow_ttsFails_degradesToText() — TTS 失败降级
```

测试完整对练流程（使用 mock 的 ASR/TTS，LLM 调用 mock）。

- [ ] **Step 9: Commit**

```bash
git add .
git commit -m "feat(server): integrate AI, ASR, TTS services with prompt engineering"
```

---

## Task 7: 复盘分析服务

**Files:**
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/AnalysisService.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/prompt/AnalysisPromptBuilder.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/controller/ReportController.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/dto/ReportView.java`
- Test: `arguing-tutorial-server/src/test/java/com/arguing/service/AnalysisServiceTest.java`

- [ ] **Step 1: 编写 AnalysisPromptBuilder**

构建复盘分析 Prompt：将完整对话历史 + 场景信息 + 评分标准喂给 LLM，要求返回结构化 JSON：
```json
{
  "logic_score": 75, "emotion_score": 60, "persuasion_score": 80,
  "strategy_score": 65, "clarity_score": 85,
  "strengths": ["用数据说话", "逻辑清晰"],
  "improvements": ["第5轮情绪激动", "缺少共情"],
  "round_reviews": [{"round": 1, "comment": "...", "score": 70}, ...]
}
```

- [ ] **Step 2: 编写 AnalysisService**

```java
@Service
public class AnalysisService {
    // analyze(session) → 调用 AI 生成复盘报告，计算总分，保存 Report
    // getReport(sessionId) → 返回 ReportView
}
```

总分计算：各维度 × 权重 - 提示扣分（每次 5 分），最低 0 分。

- [ ] **Step 3: 编写 ReportController**

```
GET /api/reports/{sessionId}           → 复盘报告
GET /api/reports/{sessionId}/share-card → 分享卡片
```

- [ ] **Step 4: 编写 ReportView DTO**

包含所有评分维度、strengths、improvements、roundReviews、与上次对比的 diff。

- [ ] **Step 5: 编写测试**

测试总分计算逻辑（含边界：扣分后低于 0、满分 100、各权重正确）。

- [ ] **Step 6: Run tests**

Run: `mvn test -Dtest=AnalysisServiceTest`
Expected: 通过。

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat(server): add analysis service with scoring and report generation"
```

---

## Task 8: 分享卡片与用户服务

**Files:**
- Create: `arguing-tutorial-server/src/main/java/com/arguing/service/ShareService.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/controller/UserController.java`
- Create: `arguing-tutorial-server/src/main/java/com/arguing/dto/ShareCardResponse.java`
- Create: `arguing-tutorial-server/src/test/java/com/arguing/service/ShareServiceTest.java`
- Create: `arguing-tutorial-server/src/test/java/com/arguing/controller/UserControllerTest.java`

- [ ] **Step 1: 编写 ShareService**

生成分享卡片：
- 使用 Java 2D Graphics 在服务端渲染卡片图片（750×600 JPG）
- 渲染内容：场景名、评分、维度星级、渐变背景
- 调用微信 API 生成小程序码
- 上传至 CDN，返回 URL（TTL 7 天）

- [ ] **Step 2: 编写 UserController**

```
GET /api/user/profile    → 用户信息
GET /api/user/history    → 对练历史（分页）
GET /api/user/stats      → 统计数据：{ totalSessions, avgScore, bestScore, recentScores: [78, 85, 72, 90] }
```

`recentScores` 返回最近 10 次分数数组，用于前端绘制趋势折线图。

- [ ] **Step 3: 编写 ShareCardResponse DTO**

- [ ] **Step 4: 编写测试**

- `ShareServiceTest.java`：测试卡片渲染（使用测试数据生成图片，验证尺寸和格式正确）
- `UserControllerTest.java`：Mock AuthService，测试 profile/history/stats 接口返回

- [ ] **Step 5: Run tests**

Run: `mvn test -Dtest=ShareServiceTest,UserControllerTest`
Expected: 通过。

- [ ] **Step 6: Commit**

```bash
git add .
git commit -m "feat(server): add share card generation and user profile API"
```

---

## Task 9: 前端项目初始化

**Files:**
- Create: `arguing-tutorial-client/` 整个目录

- [ ] **Step 1: 使用 HBuilderX 或 CLI 创建 uni-app 项目**

```bash
cd D:/code/arguing_tutorial
npx degit dcloudio/uni-preset-vue#vite-ts arguing-tutorial-client
cd arguing-tutorial-client
npm install
```

- [ ] **Step 2: 配置 pages.json**

```json
{
  "pages": [
    { "path": "pages/index/index", "style": { "navigationBarTitleText": "吵架修炼场" } },
    { "path": "pages/practice/index", "style": { "navigationStyle": "custom" } },
    { "path": "pages/report/index", "style": { "navigationBarTitleText": "复盘报告" } },
    { "path": "pages/profile/index", "style": { "navigationBarTitleText": "我的" } }
  ],
  "globalStyle": {
    "navigationBarTextStyle": "white",
    "navigationBarBackgroundColor": "#1a1a2e"
  }
}
```

- [ ] **Step 3: 配置 API 层**

`api/index.js`: 创建 uni.request 封装，自动携带 `X-Guest-Token` header。
`api/scene.js`, `api/session.js`, `api/report.js`, `api/user.js`: 各模块 API 方法。

- [ ] **Step 4: 编写 useGuest composable**

```javascript
// composables/useGuest.js
export function useGuest() {
  // 从 uni.getStorageSync('guest_token') 读取
  // 若无则调用 POST /api/auth/guest 获取
  // 返回 { token, ensureToken() }
}
```

- [ ] **Step 5: 验证项目编译**

Run: `npm run dev:mp-weixin`
Expected: 编译成功，可在微信开发者工具中打开。

- [ ] **Step 6: Commit**

```bash
git add arguing-tutorial-client/
git commit -m "feat(client): init uni-app project with API layer and routing"
```

---

## Task 10: 首页 — 场景列表

**Files:**
- Create: `arguing-tutorial-client/src/pages/index/index.vue`
- Create: `arguing-tutorial-client/src/components/SceneCard.vue`

- [ ] **Step 1: 编写 SceneCard 组件**

场景卡片：渐变背景色（按分类区分）、场景名称、描述、难度星级、分类标签。点击触发 `navigateTo('/pages/practice/index?sceneId=xxx')`。

- [ ] **Step 2: 编写首页**

```vue
<template>
  <!-- 顶部 banner -->
  <view class="banner">吵架修炼场</view>
  <!-- 分类 tab -->
  <view class="tabs">全部 | 职场 | 技术 | 日常</view>
  <!-- 场景卡片列表 -->
  <view class="scene-list">
    <SceneCard v-for="scene in scenes" :key="scene.id" :scene="scene" />
  </view>
  <!-- 自定义场景入口 -->
  <view class="custom-entry">+ 自定义场景</view>
</template>
```

onMounted 调用 `GET /api/scenes` 加载数据。

- [ ] **Step 3: 验证页面渲染**

微信开发者工具中查看，确认场景卡片正确展示。

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat(client): add home page with scene cards"
```

---

## Task 11: 沉浸式对练页 — 语音录制与对话

**Files:**
- Create: `arguing-tutorial-client/src/pages/practice/index.vue`
- Create: `arguing-tutorial-client/src/components/VoiceRecorder.vue`
- Create: `arguing-tutorial-client/src/components/AudioPlayer.vue`
- Create: `arguing-tutorial-client/src/composables/useRecording.js`

- [ ] **Step 1: 编写 useRecording composable**

封装 `wx.getRecorderManager()`：
- `start()` → 开始录音（AAC, 16000Hz）
- `stop()` → 停止录音，返回临时文件路径
- 最短 1s 校验、最长 60s 自动截止
- 上滑取消手势

- [ ] **Step 2: 编写 VoiceRecorder 组件**

底部大圆形麦克风按钮：
- 按住开始录音，松开结束
- 录音中显示波形动画
- 上滑显示"松开取消"

- [ ] **Step 3: 编写 AudioPlayer 组件**

封装 `InnerAudioContext`：
- `play(url)` → 播放 AI 语音
- `onEnded` 回调 → 播放结束后开启下一轮录音
- `onError` 回调 → 降级为纯文字

- [ ] **Step 4: 编写对练页核心逻辑**

```
onLoad(sceneId):
  1. 调用 POST /api/sessions 创建会话，获取开场白
  2. 播放 AI 开场语音
  3. 等待播放结束，开启用户录音

onRecordingStop(audioFile):
  1. 上传音频到 POST /api/sessions/{id}/chat
  2. 显示"思考中"状态
  3. 收到响应后播放 AI 语音 + 更新字幕
  4. 更新进度条（currentRound/totalRounds）
  5. 若达到 10 轮，自动调用 endSession 并跳转复盘页
```

- [ ] **Step 5: 编写页面布局**

- 全屏深色背景（根据 scene.backgroundConfig 动态设置）
- 底部弱化文字字幕区域
- 顶部进度条 + 场景名称
- 中间区域预留给数字人组件（Task 12）
- 底部麦克风按钮 + 提示按钮 + 结束按钮

- [ ] **Step 5.5: 处理录音权限**

进入对练页时调用 `uni.authorize({ scope: 'scope.record' })`。
若用户拒绝：显示弹窗解释需要麦克风权限，提供"打开设置"按钮跳转系统设置页。
设置页返回后重新检查权限状态。

- [ ] **Step 6: 验证完整对练流程**

测试：进入场景 → 听到 AI 开场白 → 按住说话 → 听到 AI 回复 → 持续 10 轮 → 自动跳转复盘。

- [ ] **Step 7: Commit**

```bash
git add .
git commit -m "feat(client): add immersive practice page with voice recording"
```

---

## Task 12: 数字人渲染组件

**Files:**
- Create: `arguing-tutorial-client/src/components/AvatarRenderer.vue`
- Create: `arguing-tutorial-client/src/composables/useAvatar.js`

- [ ] **Step 1: 编写 useAvatar composable**

管理 Canvas 2D 绘制逻辑：
- `loadAvatar(avatarConfig)` → 从 CDN 加载模型 JSON + 纹理图集
- `playMouth(phoneme)` → 切换口型（a/i/u/e/o 5 种）
- `setEmotion(emotion)` → 切换表情动画（angry/sarcastic/hesitant/compromising/confident）
- `playIdle()` → 待机动画（呼吸 + 眨眼）
- `playThinking()` → 思考动画（呼吸 + 眨眼 + 皱眉）

- [ ] **Step 2: 编写 AvatarRenderer 组件**

```vue
<template>
  <canvas type="2d" id="avatar-canvas" class="avatar-canvas" />
</template>
```

Props: `avatarConfig`, `emotion`, `phonemeTimestamps`, `status` (idle/speaking/thinking)

通过 `uni.createCanvasContext` 获取 Canvas 上下文，用 `useAvatar` 驱动绘制。

- [ ] **Step 3: 实现 5 种口型关键帧**

基于 TTS 返回的音素时间戳数据，在播放 AI 语音时同步切换口型。使用 `requestAnimationFrame` 驱动动画循环。

- [ ] **Step 4: 实现 5 种表情动画**

每种表情对应不同的眉毛角度、眼睛大小、嘴角弧度。使用平滑过渡插值。

- [ ] **Step 5: 创建占位数字人资源**

创建 2 套简易的 2D 角色模型 JSON（"老板"和"PM"），用简单几何图形绘制，用于 MVP 验证。

- [ ] **Step 6: 将 AvatarRenderer 集成到对练页**

替换对练页中间区域的占位内容，传入 AI 返回的 emotion 和 phonemeTimestamps。

- [ ] **Step 7: 验证数字人口型同步**

测试：AI 说话时嘴巴同步开合，表情随 emotion 变化，待机时有呼吸和眨眼动画。

- [ ] **Step 8: Commit**

```bash
git add .
git commit -m "feat(client): add avatar renderer with lip sync and emotion"
```

---

## Task 13: 复盘报告页

**Files:**
- Create: `arguing-tutorial-client/src/pages/report/index.vue`
- Create: `arguing-tutorial-client/src/components/ScoreRadar.vue`
- Create: `arguing-tutorial-client/src/components/ShareCard.vue`

- [ ] **Step 1: 编写 ScoreRadar 组件**

使用 Canvas 2D 绘制五维雷达图（逻辑性、情绪控制、说服力、策略运用、表达清晰度）。

- [ ] **Step 2: 编写复盘报告页**

```
onLoad(sessionId):
  调用 GET /api/reports/{sessionId}
  渲染：
    - 总分（大字） + 与上次对比
    - ScoreRadar 雷达图
    - 做得好的（绿色卡片列表）
    - 可以改进的（橙色卡片列表）
    - 每轮逐一复盘（可折叠）
    - 底部按钮：再来一次 | 分享战绩
```

- [ ] **Step 3: 编写 ShareCard 组件**

调用 `GET /api/reports/{sessionId}/share-card` 获取分享卡片图片，展示预览并提供分享按钮：
- `wx.shareAppMessage` → 分享给好友
- `wx.shareTimeline` → 分享到朋友圈

- [ ] **Step 4: 验证复盘页渲染**

测试：对练结束后跳转复盘页 → 显示评分、雷达图、点评。点击分享 → 生成卡片。

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat(client): add report page with radar chart and share"
```

---

## Task 14: 个人中心

**Files:**
- Create: `arguing-tutorial-client/src/pages/profile/index.vue`
- Create: `arguing-tutorial-client/src/store/user.js`

- [ ] **Step 1: 编写 user store (Pinia)**

管理用户状态：登录状态、用户信息、游客/正式用户切换。

- [ ] **Step 2: 编写个人中心页**

```
- 头像 + 昵称（游客显示"点击登录"）
- 统计卡片：总场次、平均分、最高分
- 历史记录列表（场景名 + 日期 + 分数，可点击查看复盘）
- 游客提示："登录后保存完整历史" CTA 按钮
```

- [ ] **Step 3: 验证个人中心**

测试：游客模式显示有限历史，点击登录跳转微信授权，登录后显示完整数据。

- [ ] **Step 4: Commit**

```bash
git add .
git commit -m "feat(client): add profile page with history and stats"
```

---

## Task 15: 端到端集成与优化

**Files:**
- Modify: 多个文件的 bug fix 和优化

- [ ] **Step 1: 完整流程走测**

从首页 → 选场景 → 对练 10 轮 → 复盘 → 分享 → 个人中心，验证全流程无断点。

- [ ] **Step 2: 错误场景测试**

- ASR 识别为空 → 提示重试
- 网络断开 → 断网提示
- 录音 < 1s → 提示太短
- LLM 超时 → 重试提示

- [ ] **Step 3: 性能优化**

- 音频文件压缩（确保 < 2MB）
- 数字人资源预加载
- API 响应缓存（场景列表）

- [ ] **Step 4: 最终 Commit**

```bash
git add .
git commit -m "feat: end-to-end integration and polish"
git push origin master
```

---

## 依赖关系

```
Task 1 (DB) → Task 2 (Entity) → Task 3 (Scene API) → Task 3.5 (Custom Scene) → Task 5 (Session) → Task 6 (AI)
                                                                               ↓
                                                                         Task 7 (Analysis)
                                                                               ↓
                                                                         Task 8 (Share/User)

Task 4 (Auth) 依赖 Task 1+2，然后可与 Task 3 并行

Task 9 (Client init) → Task 10 (Home) → Task 11 (Practice) → Task 12 (Avatar)
                                                                ↓
                                                          Task 13 (Report)
                                                                ↓
                                                          Task 14 (Profile)

Task 15 (集成) 依赖所有前置 Task
```

**可并行的任务组：**
- 后端 Task 1-3 完成后，Task 4 和 Task 5 可并行
- 前端 Task 9-10 完成后，Task 11 等后端 Task 5-6 完成
- Task 12（数字人）可独立开发，与 Task 11 并行
