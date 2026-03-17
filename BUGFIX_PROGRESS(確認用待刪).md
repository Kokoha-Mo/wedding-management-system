# Progress 頁面問題修復紀錄

## 概述
修復 customer_progress 無法載入進度資料的問題，包括以下階段：
- **第一階段**：修復 403 Forbidden（認證失敗）
- **第二階段**：修復相對路徑 + Cookie 無法附帶
- **第三階段**：修復 500 Internal Server Error（Lazy Loading + 序列化問題）

---

## 第一階段修復（2026-03-16）：消除 403 Forbidden

### 問題根源
1. `JwtFilter` 有 `@Component` → Spring Boot 自動在 Security chain **外層**執行
2. 該 filter 用**空白角色** (`new ArrayList<>()`) 設定認證
3. `JwtAuthFilter` 見到已有認證就直接跳過
4. 結果：Customer 沒有 `ROLE_CUSTOMER` → 403

### 修改清單

#### 1. `src/main/java/.../filter/JwtFilter.java`
**修改**：移除 `@Component` 註解  
**原因**：避免 Spring Boot 自動重複執行此 filter，導致覆蓋 `JwtAuthFilter` 的 `ROLE_CUSTOMER`

```java
// 改前
@Component
public class JwtFilter extends OncePerRequestFilter {
    // ...
}

// 改後
// 注意：不加 @Component，避免 Spring Boot 在 Security chain 外自動重複執行
public class JwtFilter extends OncePerRequestFilter {
    // ...
}
```

#### 2. `src/main/java/.../config/SecurityConfig.java`
**修改 1**：新增 `/api/customer/logout` 的 permitAll 路由  
**原因**：新增 customer logout 端點需要安全例外

```java
// 改前
.requestMatchers("/api/customer/login").permitAll()
.requestMatchers("/api/employee/login").permitAll()

// 改後
.requestMatchers("/api/customer/login").permitAll()
.requestMatchers("/api/customer/logout").permitAll()  // ← 新增
.requestMatchers("/api/employee/login").permitAll()
```

**修改 2**：移除重複的手動 `JwtFilter` 注冊  
**原因**：Spring Security chain 只需要一次 filter，否則導致雙重處理

```java
// 改前
.authorizeHttpRequests(auth -> auth /* ... */);

http.addFilterBefore(new com.wedding.wedding_management_system.filter.JwtFilter(),
        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

return http.build();

// 改後
.authorizeHttpRequests(auth -> auth /* ... */);
// ← 移除重複的 http.addFilterBefore(...)

return http.build();
```

---

## 第二階段修復（2026-03-16）：修正相對路徑 + Cookie 無法附帶

### 問題根源
- 前端使用絕對路徑 `http://127.0.0.1:8080` vs `http://localhost:8080` → Cookie 跨 origin，瀏覽器不附帶
- 不同的域名導致 HttpOnly Cookie 不被傳送

### 修改清單

#### 3. `src/main/resources/static/client/login.js`
**修改**：絕對路徑 → 相對路徑  
**原因**：避免 origin 分裂，確保 Cookie 在同一域下附帶

```javascript
// 改前 - 第 86 行
const response = await fetch('http://127.0.0.1:8080/api/customer/login', {

// 改後
const response = await fetch('/api/customer/login', {

// 改前 - 第 142 行
await fetch('http://127.0.0.1:8080/api/customer/logout', {

// 改後
await fetch('/api/customer/logout', {
```

#### 4. `src/main/resources/static/client/customer_progress.html`
**修改 1**：GET progress 改為相對路徑 + 加 `withCredentials`  
**原因**：確保 Cookie 跨 tab 同步且被完整附帶

```javascript
// 改前 - 第 1272 行
axios.get(`http://localhost:8080/api/customer/projects/${projectId}/progress`)

// 改後
axios.get(`/api/customer/projects/${projectId}/progress`, { withCredentials: true })
```

**修改 2**：檔案 URL 從硬碼 localhost → `window.location.origin`  
**原因**：動態取得目前頁面的 origin，支援不同部署環境

```javascript
// 改前 - 第 1294 行
fileUrl = 'http://localhost:8080' + fileUrl;

// 改後
fileUrl = window.location.origin + fileUrl;

// 改前 - 第 1335 行
fileUrl = 'http://localhost:8080' + fileUrl;

// 改後
fileUrl = window.location.origin + fileUrl;
```

**修改 3**：POST communication 加 `withCredentials`  
**原因**：多部分表單上傳也需要帶 Cookie

```javascript
// 改前 - 第 1433 行
axios.post(`http://localhost:8080/api/customer/projects/${projectId}/communication`, formData, {
    headers: {
        'Content-Type': 'multipart/form-data'
    }
})

// 改後
axios.post(`/api/customer/projects/${projectId}/communication`, formData, {
    headers: {
        'Content-Type': 'multipart/form-data'
    },
    withCredentials: true  // ← 新增
})
```

**修改 4**：Vue setup 的 return 物件加 `isSubmitting`  
**原因**：修復 Vue 警告「isSubmitting 在 template 被使用但未被 return」

```javascript
// 改前 - 第 1487 行
return {
    data, dashOffset, getCatIcon, hasUnread, clearUnread,
    messages, newMessage, postMessage, selectedFiles,
    handleFileUpload, removeSelectedFile,
    isPhaseCompleted 
}

// 改後
return {
    data, dashOffset, getCatIcon, hasUnread, clearUnread,
    messages, newMessage, postMessage, selectedFiles,
    handleFileUpload, removeSelectedFile,
    isPhaseCompleted, isSubmitting  // ← 新增
}
```

#### 5. `src/main/resources/static/client/wedding_contact.html`
**修改**：API 呼叫改為相對路徑

```javascript
// 改前 - 第 1009 行
axios.post('http://localhost:8080/api/consultations', payload)

// 改後
axios.post('/api/consultations', payload)
```

#### 6. `src/main/resources/static/manager/consultation.html`
**修改 1**：GET consultations 改為相對路徑

```javascript
// 改前 - 第 596 行
axios.get('http://localhost:8080/api/consultations')

// 改後
axios.get('/api/consultations')
```

**修改 2**：PUT status 改為相對路徑

```javascript
// 改前 - 第 667 行
axios.put(`http://localhost:8080/api/consultations/${this.selectedItem.id}/status`, { status: newStatusStr })

// 改後
axios.put(`/api/consultations/${this.selectedItem.id}/status`, { status: newStatusStr })
```

---

## 第三階段修復（2026-03-16）：消除 500 Internal Server Error

### 問題根源
1. **Lazy Loading Exception**：`ProjectService` 沒有 `@Transactional`，JPA Session 提前關閉，後續訪問 lazy 關聯（`getDocuments()`、`getManager()` 等）丟出 `LazyInitializationException`
2. **Hibernate Proxy 序列化失敗**：Jackson 無法序列化 Hibernate 動態代理物件
3. **CORS 重複加頭**：`@CrossOrigin(origins="*")` 與全域 `CorsConfig` 衝突

### 修改清單

#### 7. `src/main/java/.../service/ProjectService.java`
**修改 1**：Import 新增 `@Transactional` 和 `Objects`

```java
// 改前
import org.springframework.stereotype.Service;

// 改後
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
```

**修改 2**：`getProjectProgress` 加 `@Transactional(readOnly = true)`  
**原因**：
- 將整個方法納入**同一個 JPA Session** 內執行
- 所有 lazy 關聯（`book.getCustomer()`、`book.getManager()`、`documents` 等）都能在 Session 期間正常 Load
- 避免 `LazyInitializationException`

```java
// 改前
public ProjectProgressDTO getProjectProgress(Integer projectId) {

// 改後
@Transactional(readOnly = true)
public ProjectProgressDTO getProjectProgress(Integer projectId) {
```

**修改 3**：防 NPE - role 為 null 時 fallback

```java
// 改前 - 第 281 行
commDto.setCreateBy(role);

// 改後
commDto.setCreateBy(role != null ? role : "");
```

**修改 4**：使用 `timeline` 區域變數 + `Objects.equals` + null check

```java
// 改前 - 第 333-341 行
String pmNameStr = book != null && book.getManager() != null ? book.getManager().getName() : "公司";
dto.getTimeline().stream()
    .filter(c -> c.getCreateBy().equals(pmNameStr))
    .findFirst()
    .ifPresent(latestComm -> {
        p2.setPmMessage(latestComm.getContent());
        p2.setPmUpdateTime(latestComm.getCreateAt().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")));
    });

// 改後
String pmNameStr = book != null && book.getManager() != null ? book.getManager().getName() : "公司";
timeline.stream()
    .filter(c -> Objects.equals(c.getCreateBy(), pmNameStr))
    .findFirst()
    .ifPresent(latestComm -> {
        p2.setPmMessage(latestComm.getContent());
        if (latestComm.getCreateAt() != null) {
            p2.setPmUpdateTime(latestComm.getCreateAt().format(DateTimeFormatter.ofPattern("MM/dd HH:mm")));
        }
    });
```

#### 8. `src/main/java/.../controller/CustomerController.java`
**修改 1**：移除 `@CrossOrigin(origins = "*")`  
**原因**：全域 `CorsConfig` 已處理 CORS，重複標注會讓 `Access-Control-Allow-Origin` header 被加兩次，導致瀏覽器拒絕

```java
// 改前
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")  // ← 移除
public class CustomerController {

// 改後
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
public class CustomerController {
```

**修改 2**：改 exception 處理為 `Exception` + `printStackTrace()`  
**原因**：
- 從 `RuntimeException` 改為 `Exception`，攔截包括 `LazyInitializationException` 等其他例外
- 呼叫 `printStackTrace()` 將完整 stack trace 印到後端 console，便於定位問題
- 返回 500 而不是 404，準確反映後端錯誤狀態

```java
// 改前
} catch (RuntimeException e) {
    return ResponseEntity.notFound().build(); // 找不到專案時回傳 404
}

// 改後
} catch (Exception e) {
    e.printStackTrace(); // 在後端 console 印出完整 stack trace
    Map<String, Object> err = new HashMap<>();
    err.put("error", e.getClass().getSimpleName());
    err.put("message", e.getMessage());
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
}
```

---

## 修復效果

| 階段 | 問題 | 根本原因 | 修復方式 | 效果 |
|------|------|--------|--------|------|
| 1 | 403 Forbidden | 雙重 JWT filter 導致認證失敗 | 移除 `@Component` + 移除重複注冊 | `ROLE_CUSTOMER` 正確設定 |
| 2 | Cookie 無法附帶 | Origin 分裂（localhost vs 127.0.0.1） | 改用相對路徑 + `withCredentials: true` | Cookie 在同一域下正常傳送 |
| 3 | 500 LazyInitializationException | JPA Session 提前關閉 | 加 `@Transactional` | 所有 lazy 關聯在 Session 內正常載入 |
| 3 | 500 Jackson 序列化失敗 | 無法序列化 Hibernate proxy | `@Transactional` 確保物件完全初始化 | JSON 序列化成功 |
| 2 | Vue 警告 | 未返回 `isSubmitting` | 加入 setup return | 控制台無警告 |

---

## 後續驗證步驟

1. **重啟 Spring Boot**
   ```bash
   mvnw spring-boot:run
   ```

2. **以登入帳號開啟 progress 頁面**
   ```
   http://localhost:8080/static/client/customer_progress.html?id=6
   ```

3. **檢查結果**
   - ✅ 頁面正常載入，顯示進度資料
   - ✅ 後端無 console 錯誤
   - ✅ 瀏覽器 console 無 Vue 警告
   - ✅ Network tab 顯示 `/api/customer/projects/6/progress` 為 200

4. **若仍有 500 錯誤**
   - 後端 console 會印出完整 stack trace（修改 8 的效果）
   - 根據 exception 類型和 message 快速定位

---

## 相關檔案變更統計

```
修改檔案: 8 個
├─ backend (Java)
│  ├─ JwtFilter.java              (1 行)
│  ├─ SecurityConfig.java          (2 行)
│  ├─ ProjectService.java          (8 行)
│  └─ CustomerController.java      (12 行)
└─ frontend (HTML/JS)
   ├─ login.js                     (2 行)
   ├─ customer_progress.html       (6 行)
   ├─ wedding_contact.html         (1 行)
   └─ consultation.html            (2 行)

總計修改行數: ~34 行程式碼
```

---

## 關鍵學習

1. **JWT + Cookie 跨域注意事項**
   - 同一 Cookie 不能跨 origin（localhost ≠ 127.0.0.1）
   - 相對路徑 `/api/...` 可自動依據當前頁面 origin
   - 需要 `withCredentials: true` 才能帶 Cookie

2. **Spring Data JPA Lazy Loading**
   - 不用 `@Transactional`，JPA Session 結束後無法訪問 lazy 關聯
   - Jackson 序列化時也會觸發 lazy load，需要同一 transaction

3. **CORS 設定衝突**
   - Method 級 `@CrossOrigin` 與全域 `CorsConfig` 會重複加頭
   - 應統一使用一種方式配置

4. **例外處理的準確性**
   - 區分 404 (找不到資源) vs 500 (伺服器錯誤)
   - 打印 `printStackTrace()` 便於開發除錯

