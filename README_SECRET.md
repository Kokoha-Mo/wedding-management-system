## 🔐 環境變數設定

敏感資訊不寫入程式碼，需在本機設定系統環境變數，僅須設定一次。
數值請參照雲端資料夾內的「 婚禮管理系統_環境變數.txt 」

### Windows
1. 搜尋「編輯系統環境變數」→「環境變數」→「系統變數」→「新增」
2. 依序加入以下三個變數：
   - `JWT_SECRET`
   - `SMTP_USERNAME`
   - `SMTP_PASSWORD`
3. 按「確定」後**完全關閉 IDE 重新開啟**


### Mac
在 `~/.zshrc` 加入以下內容，完成後執行 `source ~/.zshrc`，重新開啟 IDE。
```bash
export JWT_SECRET=組長提供的值
export SMTP_USERNAME=組長提供的值
export SMTP_PASSWORD=組長提供的值
```
