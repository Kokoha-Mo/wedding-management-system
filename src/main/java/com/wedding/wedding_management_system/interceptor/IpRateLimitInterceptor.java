package com.wedding.wedding_management_system.interceptor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class IpRateLimitInterceptor implements HandlerInterceptor {

    // 記錄每個 IP 的最後請求時間
    private final Map<String, Long> ipRequestMap = new ConcurrentHashMap<>();

    // 同一個 IP 幾毫秒內只能打一次 (60 秒)
    private static final long LIMIT_TIME_MS = 60 * 1000;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // 1. 取得使用者的真實 IP
        // (如果有經過 GCP Load Balancer 或 Nginx，要抓 X-Forwarded-For)
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }

        long currentTime = System.currentTimeMillis();

        // 2. 檢查這個 IP 是不是最近才剛打過 API
        if (ipRequestMap.containsKey(clientIp)) {
            long lastRequestTime = ipRequestMap.get(clientIp);

            if (currentTime - lastRequestTime < LIMIT_TIME_MS) {
                // 3. 如果小於 60 秒，直接擋下！回傳 429 Too Many Requests
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\": \"請求過於頻繁，請稍後再試\"}");

                return false; // false 代表擋下，不會進入 Controller
            }
        }

        // 4. 更新這個 IP 的最後請求時間，並放行
        ipRequestMap.put(clientIp, currentTime);
        return true; // true 代表放行，交給 Controller 繼續處理
    }
}