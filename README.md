# catwechat
## 创建数据库
```sql
CREATE DATABASE `wechat` CHARACTER SET 'utf8mb4' COLLATE 'utf8mb4_general_ci';
```
如果数据库已经创建，可以直接修改字段类型
```sql
ALTER TABLE wechat_listener_speak MODIFY `nickName` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
ALTER TABLE wechat_listener_check MODIFY `nickName` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
```