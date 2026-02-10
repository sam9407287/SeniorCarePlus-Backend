# 🚨 内存泄漏修复 - 紧急部署指南

## ⚡ 快速部署步骤

### 1. 提交代码
```bash
cd /Users/sam/Desktop/work/SeniorCarePlusBackend

git add .
git commit -m "fix: 修复内存泄漏问题 - GlobalScope、数据库连接池、MQTT清理"
git push origin main
```

### 2. Railway 自动部署
如果你使用 Railway，推送后会自动部署。

### 3. 手动部署（如果需要）
```bash
# 构建项目
./gradlew build

# 或者使用 Railway CLI
railway up
```

---

## 🔍 部署后验证

### 1. 检查应用是否正常启动
```bash
# 访问健康检查端点
curl https://your-app.railway.app/health

# 预期响应
{"status":"healthy","service":"SeniorCarePlus Backend","timestamp":...}
```

### 2. 查看启动日志
```bash
railway logs
```

应该看到：
```
✅ 數據庫初始化成功
✅ MQTT服務啟動成功
✅ SeniorCarePlus Backend 服務已啟動
```

### 3. 测试 WebSocket 连接状态
```bash
curl https://your-app.railway.app/ws/status
```

### 4. 监控内存使用
- 打开 Railway Dashboard
- 查看 Metrics 标签
- 观察内存曲线是否稳定

---

## 📊 预期改善

### 修复前症状
- 🔴 内存持续增长
- 🔴 需要频繁重启
- 🔴 运行几小时后变慢

### 修复后效果
- 🟢 内存使用稳定
- 🟢 可长期运行
- 🟢 性能保持稳定

---

## ⚠️ 如果出现问题

### 回滚步骤
```bash
# 1. 查看之前的提交
git log --oneline -5

# 2. 回滚到上一个版本
git revert HEAD

# 3. 推送回滚
git push origin main
```

### 查看日志
```bash
# Railway
railway logs --tail

# 或者在 Railway Dashboard 中查看 Deployments → Logs
```

---

## 📝 修复的主要内容

✅ **GlobalScope 协程泄漏** → 使用受管理的 CoroutineScope  
✅ **数据库连接池未关闭** → 添加 shutdown() 方法  
✅ **MQTT 客户端未清理** → 在 ApplicationStopping 中断开连接  
✅ **WebSocket 连接未追踪** → 使用 ConcurrentHashMap 管理  

详细信息请查看 `MEMORY_LEAK_FIXES.md`

---

## 📞 需要帮助？

查看完整文档：`MEMORY_LEAK_FIXES.md`
