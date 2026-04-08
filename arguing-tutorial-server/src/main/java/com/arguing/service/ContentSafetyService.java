package com.arguing.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 内容安全服务。
 * 提供敏感词过滤和内容审核功能。
 * 当前为简单实现：基于预设敏感词列表进行匹配和替换。
 */
@Service
public class ContentSafetyService {

    private static final Logger log = LoggerFactory.getLogger(ContentSafetyService.class);

    /** 预设敏感词列表 */
    private static final List<String> SENSITIVE_WORDS = List.of(
            "暴力", "色情", "赌博", "毒品", "自杀", "恐怖"
    );

    /** 替换文本 */
    private static final String REPLACEMENT = "***";

    /**
     * 敏感词过滤：将文本中的敏感词替换为 ***。
     *
     * @param text 原始文本
     * @return 过滤后的文本
     */
    public String filter(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String filtered = text;
        for (String word : SENSITIVE_WORDS) {
            if (filtered.contains(word)) {
                log.debug("检测到敏感词: {}", word);
                filtered = filtered.replace(word, REPLACEMENT);
            }
        }
        return filtered;
    }

    /**
     * 审核 AI 输出是否安全。
     * 简单实现：检查是否包含敏感词，有则返回 false。
     *
     * @param text AI 输出文本
     * @return true 表示安全，false 表示不安全
     */
    public boolean audit(String text) {
        if (text == null || text.isEmpty()) {
            return true;
        }

        for (String word : SENSITIVE_WORDS) {
            if (text.contains(word)) {
                log.warn("AI 输出包含敏感词: {}", word);
                return false;
            }
        }
        return true;
    }
}
