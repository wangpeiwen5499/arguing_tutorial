package com.arguing.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API 日志切面。
 * 自动拦截所有 Controller 方法，打印入参、返回结果、执行耗时和异常信息。
 */
@Aspect
@Component
public class ApiLoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingAspect.class);

    /** 返回值截断长度，避免日志过大 */
    private static final int MAX_RESULT_LENGTH = 800;

    @Around("execution(* com.arguing.controller..*.*(..))")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        // 提取有效参数（排除 Servlet 对象）
        String params = extractParams(joinPoint.getArgs());
        log.info(">>> {}.{} 入参: {}", className, methodName, params);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            String resultStr = truncate(String.valueOf(result), MAX_RESULT_LENGTH);
            log.info("<<< {}.{} 返回: {} [{}ms]", className, methodName, resultStr, elapsed);
            return result;
        } catch (Throwable e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("!!! {}.{} 异常: {} [{}ms]", className, methodName, e.getMessage(), elapsed, e);
            throw e;
        }
    }

    /**
     * 提取有效参数，跳过 Servlet 相关对象。
     */
    private String extractParams(Object[] args) {
        if (args == null || args.length == 0) return "{}";

        Map<String, Object> paramMap = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg instanceof HttpServletRequest) {
                paramMap.put("arg" + i, "[HttpServletRequest]");
            } else if (arg instanceof HttpServletResponse) {
                paramMap.put("arg" + i, "[HttpServletResponse]");
            } else {
                String value = truncate(String.valueOf(arg), 300);
                paramMap.put("arg" + i, value);
            }
        }
        return paramMap.toString();
    }

    private String truncate(String str, int maxLen) {
        if (str == null) return "null";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen) + "...(truncated)";
    }
}
