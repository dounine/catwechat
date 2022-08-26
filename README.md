# catwechat

## 表情字段
由于nickName中有特殊字符，需要修改一下
```sql
ALTER TABLE wechat_listener_speak MODIFY `nickName` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE wechat_listener_check MODIFY `nickName` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```