# 💍 婚禮管理系統 - 開發與部署 SOP

本文件旨在規範小組成員的開發流程，確保環境隔離並防止雲端資源意外受損。

---

## 💻 一、本地開發環境設定 (Local Environment)

為了不影響雲端資料庫及其他組員，請務必使用 `local` Profile 進行開發。

### 1. 建立私人設定檔
在 `src/main/resources` 下手動建立 `application-local.properties`。
> **注意：此檔案已被納入 .gitignore，不會上傳至 Git。**

```properties
# 本機資料庫連線 (請依個人環境修改)
spring.datasource.url=jdbc:mysql://35.201.241.11:3306/wedding_management_system?useSSL=false&serverTimezone=Asia/Taipei&characterEncoding=utf-8
spring.datasource.username=root
spring.datasource.password=你的密碼