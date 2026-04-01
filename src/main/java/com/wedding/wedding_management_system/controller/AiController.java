package com.wedding.wedding_management_system.controller;

import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/customer/ai")
public class AiController {
    @Value("${GEMINI_API_KEY:}")
    private String geminiApiKey;

        private static final List<Map<String, Object>> SERVICE_LIST = List.of(
                Map.of("id", 3, "name", "新秘造型師", "price", 20000),
                Map.of("id", 4, "name", "新郎妝髮造型（新秘加購）", "price", 3000),
                Map.of("id", 5, "name", "主婚人/親友妝髮（新秘加購）", "price", 3600),
                Map.of("id", 6, "name", "專業婚禮主持人", "price", 15000),
                Map.of("id", 7, "name", "品牌總監級主持人（主持人加購）", "price", 8000),
                Map.of("id", 9, "name", "平面/動態紀實攝影師", "price", 25000),
                Map.of("id", 10, "name", "SDE 當日快剪快播（攝影加購）", "price", 12000),
                Map.of("id", 11, "name", "雙機位拍攝（攝影加購）", "price", 8000),
                Map.of("id", 12, "name", "類婚紗與全家福引導（攝影加購）", "price", 5000),
                Map.of("id", 13, "name", "空拍機壯闊空景拍攝（攝影加購）", "price", 10000),
                Map.of("id", 14, "name", "燈光音響與影音場控", "price", 18000),
                Map.of("id", 15, "name", "LIVE Band 音響工程（燈光加購）", "price", 35000),
                Map.of("id", 16, "name", "文定儀式引導與司儀", "price", 6000),
                Map.of("id", 17, "name", "迎娶儀式流程控管", "price", 6000),
                Map.of("id", 18, "name", "戶外/室內證婚規劃", "price", 15000),
                Map.of("id", 19, "name", "迎賓與拍照區主題設計", "price", 28000),
                Map.of("id", 20, "name", "宴客桌景與舞台視覺", "price", 32000),
                Map.of("id", 21, "name", "鮮花花藝佈置", "price", 45000),
                Map.of("id", 22, "name", "品牌級平面視覺套組", "price", 18000)
        );

        @PostMapping("/recommend")
        public ResponseEntity<?> recommend(@RequestBody Map<String, Object> body) {

            if (geminiApiKey == null || geminiApiKey.isEmpty()) {
                return ResponseEntity.status(500).body(Map.of("error", "GEMINI_API_KEY 未設定"));
            }

            try {
                // Step 1: 解析前端傳來的表單參數 (加入 null 判斷與預設值)
                String theme = (String) body.getOrDefault("theme", "未指定");
                String guestScale = body.containsKey("guestScale") ? String.valueOf(body.get("guestScale")) : "未知";
                String venue = (String) body.getOrDefault("venue", "未指定");
                String notes = (String) body.getOrDefault("notes", "無");

                // Step 2: 組裝服務清單字串
                String serviceListText = SERVICE_LIST.stream()
                        .map(s -> String.format("- ID %s：%s，NT$,%d",
                                s.get("id"), s.get("name"), (Integer) s.get("price")))
                        .collect(Collectors.joining("\n"));

                // Step 3: 組裝完整的 Prompt 模板
                String promptTemplate = """
                    你是 DREAM VENUES 的高端婚禮顧問，請根據新人資訊推薦最適合的婚禮服務組合。
                    
                    新人資訊：
                    - 主題風格：%s
                    - 賓客人數：%s 人
                    - 場地：%s
                    - 備註：%s
                    
                    可選服務清單（ID 對應項目）：
                    %s
                    
                    請推薦 4–6 項最適合的服務，只回傳 JSON，不要有任何多餘文字或 markdown：
                    {
                      "greeting": "給新人的溫暖開場白（2句，繁體中文）",
                      "recommendations": [
                        { "id": 服務ID數字, "reason": "推薦理由（15字以內）" }
                      ],
                      "closing": "一句鼓勵結語（15字以內）"
                    }
                    """;

                String finalPrompt = String.format(promptTemplate, theme, guestScale, venue, notes, serviceListText);

                Map<String, Object> geminiBody = Map.of(
                        "contents", List.of(
                                Map.of("parts", List.of(
                                        Map.of("text", finalPrompt)
                                ))
                        ),
                        "generationConfig", Map.of(
                                "responseMimeType", "application/json"
                        )
                );

                String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + geminiApiKey;

                RestTemplate restTemplate = new RestTemplate();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(geminiBody, headers);

                ResponseEntity<Map> geminiResponse = restTemplate.postForEntity(url, entity, Map.class);

                // Step 5: 從 Gemini response 取出生成的 JSON 字串
                List<Map<String, Object>> candidates = (List<Map<String, Object>>) geminiResponse.getBody().get("candidates");
                Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");
                String textResponse = (String) parts.get(0).get("text");

                // 保持原本 Anthropic / OpenAI 的回傳格式，這樣你前端解析邏輯就不用改
                return ResponseEntity.ok(Map.of(
                        "content", List.of(Map.of(
                                "type", "text",
                                "text", textResponse // 這裡面現在會是一個乾淨的 JSON 字串
                        ))
                ));

            } catch (HttpClientErrorException e) {
                System.out.println("Gemini 錯誤: " + e.getResponseBodyAsString());
                return ResponseEntity.status(e.getStatusCode()).body(Map.of("detail", e.getResponseBodyAsString()));
            } catch (Exception e) {
                log.error("AI 婚禮顧問推薦發生異常: ", e);
                return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
            }
        }
    }