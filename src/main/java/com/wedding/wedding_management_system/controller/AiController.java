package com.wedding.wedding_management_system.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer/ai")
public class AiController {
    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

    @PostMapping("/recommend")
    public ResponseEntity<?> recommend(@RequestBody Map<String, Object> body) {

        String apiKey = geminiApiKey;

        if (apiKey == null || apiKey.isEmpty()) {
            return ResponseEntity.status(500)
                    .body(Map.of("error", "GEMINI_API_KEY 未設定"));
        }

        // 從前端傳來的 messages 裡取出 prompt 文字
        String prompt = "";
        try {
            List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
            prompt = (String) messages.get(0).get("content");
        } catch (Exception e) {
            System.out.println("Step 2 失敗: " + e.getMessage());
            return ResponseEntity.status(400)
                    .body(Map.of("error", "無法解析 prompt"));
        }

        // 包裝成 Gemini 格式
        Map<String, Object> geminiBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(
                                Map.of("text", prompt)
                        ))
                )
        );

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + apiKey;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(geminiBody, headers);

        try {
            ResponseEntity<Map> geminiResponse = restTemplate.postForEntity(url, entity, Map.class);

            // 從 Gemini response 取出文字，包裝成前端期待的格式
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) geminiResponse.getBody().get("candidates");
            Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
            List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
            String text = (String) parts.get(0).get("text");

            // 包裝成跟 Anthropic 一樣的格式，前端不用改
            return ResponseEntity.ok(Map.of(
                    "content", List.of(Map.of("type", "text", "text", text))
            ));

        } catch (HttpClientErrorException e) {
            System.out.println("Gemini 錯誤: " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode())
                    .body(Map.of("detail", e.getResponseBodyAsString()));
        } catch (Exception e) {
            System.out.println("其他錯誤: " + e.getMessage());
            return ResponseEntity.status(500)
                    .body(Map.of("error", e.getMessage()));
        }
    }
}