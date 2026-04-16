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
    user_audio_url VARCHAR(2048),
    user_text TEXT,
    ai_text TEXT,
    ai_audio_url VARCHAR(2048),
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
INSERT INTO scene (name, description, category, difficulty, background_config, avatar_config, personality, opening_line) VALUES
('谈加薪', '你决定找老板谈涨工资，需要说服对方认可你的价值', '职场', 3,
 '{"type":"office","bgColor":"#1a1a2e","elements":["bookshelf","window"]}',
 '{"modelId":"boss_01","clothing":"suit","accessories":"glasses"}',
 '强硬，精于算计，不会轻易让步，善于用公司利益压人',
 '你来了？坐吧。最近工作怎么样？'),
('怼不合理需求', 'PM 提了一个明显不合理的需求，你需要在会议上说服大家', '技术', 2,
 '{"type":"open_office","bgColor":"#2d3436","elements":["monitor","whiteboard"]}',
 '{"modelId":"pm_01","clothing":"casual","accessories":"none"}',
 '固执，只关心 KPI 和排期，不懂技术细节但自信满满',
 '这个需求很简单吧，就加个按钮的事，下周五能上吗？'),
('Code Review 辩论', '同事对你的代码提出了质疑，你认为自己的方案更好', '技术', 4,
 '{"type":"meeting_room","bgColor":"#2c3e50","elements":["projector","whiteboard"]}',
 '{"modelId":"dev_01","clothing":"tshirt","accessories":"macbook"}',
 '技术能力强但固执，喜欢用反问和质疑施压，不会轻易认错',
 '这段代码的可读性不太好，为什么要用这种实现方式？'),
('跨部门资源争夺', '你需要从其他部门争取更多资源支持你的项目', '职场', 4,
 '{"type":"meeting_room","bgColor":"#1e3a5f","elements":["projector","coffee"]}',
 '{"modelId":"manager_01","clothing":"shirt","accessories":"none"}',
 '老练，深谙公司政治，善于转移话题和踢皮球',
 '你们项目的重要性我们理解，但 Q4 我们的资源也紧张'),
('日常技术选型争论', '团队在讨论技术方案，你支持的技术栈被质疑', '技术', 3,
 '{"type":"open_office","bgColor":"#2d3436","elements":["monitor","coffee"]}',
 '{"modelId":"dev_02","clothing":"hoodie","accessories":"headphones"}',
 '有主见但不恶意，喜欢用案例和最佳实践来论证',
 '说实话，这个方案在大型项目里踩过不少坑');
