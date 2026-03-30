package com.wedding.wedding_management_system.config;

import java.util.Map;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import com.wedding.wedding_management_system.util.JwtToken;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 啟用一個簡單的訊息代理。前端訂閱的路徑前綴。
        // 例如：前端訂閱 /topic/project/1 來接收專案 1 的最新討論區訊息
        config.enableSimpleBroker("/topic");

        // 前端發送訊息給後端時的路徑前綴（雖然我們這次主要是用 HTTP POST 存檔，然後用 WebSocket 推播）
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 前端用來建立 WebSocket 連線的端點
        registry.addEndpoint("/ws-dream-venues")
                .setAllowedOriginPatterns("*") // 允許跨域 (配合你的 CORS 設定)
                .addInterceptors(new JwtHandshakeInterceptor()) // 🌟 加入我們的 JWT 攔截器
                .withSockJS(); // 支援 SockJS 退避機制（如果瀏覽器不支援 WebSocket 可以降級）
    }

    /**
     * 🌟 自訂的攔截器：在建立 WebSocket 連線前，檢查 Cookie 中的 JWT Token
     */
    public class JwtHandshakeInterceptor implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {

            String token = null;

            // 1. 直接從 HttpOnly Cookie 獲取 Token (最安全、最標準的做法)
            if (request instanceof ServletServerHttpRequest) {
                HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
                Cookie[] cookies = servletRequest.getCookies();

                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        // 支援客戶端、員工端、以及全域的 jwtToken
                        if ("customerToken".equals(cookie.getName()) ||
                                "employeeToken".equals(cookie.getName()) ||
                                "jwtToken".equals(cookie.getName())) {
                            token = cookie.getValue();
                            break;
                        }
                    }
                }
            }

            // 2. 驗證 Token 是否有效
            if (token != null && JwtToken.isValid(token)) {
                String email = JwtToken.getEmail(token);
                attributes.put("userEmail", email); // 存入 session 屬性供後續使用
                return true; // 允許建立連線
            }

            // 找不到 Cookie 或 Token 無效，無情拒絕連線
            System.out.println("❌ WebSocket 連線失敗：無效的憑證或未攜帶 Cookie");
            return false;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                WebSocketHandler wsHandler, Exception exception) {
            // 連線建立後的處理（目前不需特別處理）
        }
    }
}