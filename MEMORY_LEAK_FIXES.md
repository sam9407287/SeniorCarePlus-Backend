# SeniorCarePlus Backend - 内存泄漏修复文档

## 📋 修复概览

本次修复解决了后端服务中的多个严重内存泄漏问题，这些问题会导致生产环境中内存持续增长，最终导致 OOM（Out of Memory）错误。

---

## 🔴 发现的内存泄漏问题

### 1. **GlobalScope 协程泄漏** ⚠️ 严重程度: 高

**位置**: `MqttService.kt`

**问题描述**:
- 使用 `GlobalScope.launch` 创建协程，这些协程的生命周期与应用相同
- 无法取消这些协程，即使服务不再需要它们
- 特别是 `startHealthStatusPublisher()` 中的无限循环，会永久运行

**影响**:
```kotlin
// ❌ 问题代码
GlobalScope.launch {
    while (true) {
        publishHealthStatus()
        delay(30000)
    }
}
```

**修复**:
```kotlin
// ✅ 修复后
private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
private var healthPublisherJob: Job? = null

healthPublisherJob = serviceScope.launch {
    while (isActive) {  // 可以检查协程状态
        publishHealthStatus()
        delay(30000)
    }
}

// 清理时
fun disconnect() {
    healthPublisherJob?.cancel()
    serviceScope.cancel()
    // ...
}
```

---

### 2. **数据库连接池未关闭** ⚠️ 严重程度: 高

**位置**: `DatabaseConfig.kt`

**问题描述**:
- HikariCP 连接池创建后没有保存引用
- 应用关闭时无法关闭连接池
- 数据库连接会一直保持打开状态

**影响**:
- 数据库连接泄漏
- 资源占用持续增加
- 可能达到数据库最大连接数限制

**修复**:
```kotlin
// ✅ 添加数据源引用
private var dataSource: HikariDataSource? = null

// ✅ 添加关闭方法
fun shutdown() {
    try {
        logger.info("正在關閉數據庫連接池...")
        dataSource?.close()
        dataSource = null
        logger.info("✅ 數據庫連接池已關閉")
    } catch (e: Exception) {
        logger.error("關閉數據庫連接池失敗: ${e.message}", e)
    }
}
```

---

### 3. **MQTT 客户端未断开** ⚠️ 严重程度: 中

**位置**: `Application.kt`, `MqttService.kt`

**问题描述**:
- MQTT 服务实例在应用模块中创建，但未保存引用
- ApplicationStopping 钩子中没有清理逻辑
- MQTT 连接不会被正确断开

**修复**:
```kotlin
// ✅ Application.kt
var mqttService: MqttService? = null
var mqttJob: Job? = null

// 启动时保存引用
mqttJob = launch {
    mqttService = MqttService()
    mqttService?.connect()
}

// 关闭时清理
environment.monitor.subscribe(ApplicationStopping) {
    mqttJob?.cancel()
    mqttService?.disconnect()
    DatabaseConfig.shutdown()
}
```

---

### 4. **WebSocket 连接未追踪** ⚠️ 严重程度: 中

**位置**: `WebSocketRoutes.kt`

**问题描述**:
- WebSocket 连接没有被追踪和管理
- 无法查看当前活动连接数
- 难以诊断连接泄漏问题

**修复**:
```kotlin
// ✅ 添加连接管理
val healthConnections = ConcurrentHashMap<String, DefaultWebSocketSession>()
val alertConnections = ConcurrentHashMap<String, DefaultWebSocketSession>()

webSocket("/ws/health") {
    val connectionId = "health_${connectionCounter.incrementAndGet()}"
    healthConnections[connectionId] = this
    
    try {
        // 处理连接...
    } finally {
        healthConnections.remove(connectionId)
        logger.info("连接已断开 (剩余: ${healthConnections.size})")
    }
}
```

---

## ✅ 修复后的改进

### 资源管理生命周期

```
应用启动
    ↓
初始化数据库连接池 (保存引用)
    ↓
启动 MQTT 服务 (保存实例和 Job)
    ↓
注册 WebSocket 路由 (追踪连接)
    ↓
运行中...
    ↓
接收关闭信号 (ApplicationStopping)
    ↓
1. 取消 MQTT 协程
2. 断开 MQTT 连接
3. 关闭数据库连接池
4. WebSocket 连接自动清理
    ↓
应用关闭
```

---

## 🧪 验证修复

### 1. 检查协程是否正确取消

```bash
# 监控应用日志，查找以下消息
✅ MQTT 协程已取消
✅ 健康狀態發布任務已取消
✅ 所有 MQTT 協程已取消
```

### 2. 检查数据库连接池

```bash
# 查看数据库活动连接
SELECT * FROM pg_stat_activity WHERE datname = 'seniorcareplus';

# 关闭应用后，连接应该减少到 0
```

### 3. 监控内存使用

```bash
# 使用 Railway/部署平台的监控工具
# 观察内存使用曲线，应该保持稳定而不是持续增长
```

### 4. WebSocket 连接状态

```bash
# 访问状态端点
curl http://localhost:8080/ws/status

# 响应示例:
{
  "healthConnections": 2,
  "alertConnections": 1,
  "totalConnections": 3,
  "timestamp": 1234567890
}
```

---

## 📊 预期效果

### 修复前
- ❌ 内存持续增长 (每小时 +50MB)
- ❌ 协程数量不断增加
- ❌ 数据库连接无法释放
- ❌ 需要定期重启服务

### 修复后
- ✅ 内存使用稳定
- ✅ 协程正确管理和清理
- ✅ 数据库连接及时释放
- ✅ 长期运行稳定

---

## 🎯 最佳实践总结

### 1. **永远不要使用 GlobalScope**
```kotlin
// ❌ 错误
GlobalScope.launch { ... }

// ✅ 正确
private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
scope.launch { ... }

// 清理时
scope.cancel()
```

### 2. **资源要有引用才能清理**
```kotlin
// ❌ 错误
val dataSource = HikariDataSource(config)
Database.connect(dataSource)
// 无法在后续关闭 dataSource

// ✅ 正确
private var dataSource: HikariDataSource? = null
dataSource = HikariDataSource(config)
Database.connect(dataSource!!)
// 可以在 shutdown() 中调用 dataSource?.close()
```

### 3. **使用 ApplicationStopping 进行清理**
```kotlin
environment.monitor.subscribe(ApplicationStopping) {
    // 按相反顺序清理资源
    // 1. 停止协程
    // 2. 断开网络连接
    // 3. 关闭数据库
}
```

### 4. **无限循环要可取消**
```kotlin
// ❌ 错误
while (true) { ... }

// ✅ 正确
while (isActive) {  // 检查协程状态
    try {
        // 工作内容
    } catch (e: CancellationException) {
        break  // 响应取消请求
    }
}
```

### 5. **追踪长生命周期的资源**
```kotlin
// 对于 WebSocket、数据库连接等
val activeConnections = ConcurrentHashMap<String, Connection>()

// 添加时
activeConnections[id] = connection

// 使用后移除
try {
    // 使用连接
} finally {
    activeConnections.remove(id)
}
```

---

## 🚀 部署建议

### 1. 监控指标

在生产环境中监控以下指标：

- **内存使用**: 应该在稳定范围内波动
- **协程数量**: 不应持续增长
- **数据库连接**: 应该在配置范围内 (2-10)
- **WebSocket 连接**: 与实际客户端数量一致

### 2. 日志监控

关注以下日志消息：

```
✅ 應用程序正在關閉，開始清理資源...
✅ MQTT 协程已取消
✅ MQTT 连接已断开
✅ 数据库连接池已关闭
✅ 應用程序清理完成
```

### 3. 告警设置

建议设置以下告警：

- 内存使用超过 80%
- 数据库连接数接近上限
- WebSocket 连接异常增长
- 应用重启频率异常

---

## 📝 更新日志

### 2026-02-10
- ✅ 修复 GlobalScope 协程泄漏
- ✅ 添加数据库连接池关闭逻辑
- ✅ 完善 MQTT 服务清理机制
- ✅ 改进 WebSocket 连接管理
- ✅ 添加 ApplicationStopping 清理钩子

---

## 🔗 相关文件

- `src/main/kotlin/com/seniorcareplus/Application.kt` - 应用主入口和清理逻辑
- `src/main/kotlin/com/seniorcareplus/database/DatabaseConfig.kt` - 数据库配置和关闭
- `src/main/kotlin/com/seniorcareplus/services/MqttService.kt` - MQTT 服务和协程管理
- `src/main/kotlin/com/seniorcareplus/routes/WebSocketRoutes.kt` - WebSocket 连接管理

---

## 💡 如果问题仍然存在

如果修复后仍有内存泄漏，可以：

1. **启用 JVM 内存分析**:
```bash
java -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof -jar app.jar
```

2. **使用 VisualVM 或 JProfiler** 分析堆转储文件

3. **检查第三方库**:
   - Paho MQTT 客户端
   - HikariCP
   - Ktor WebSocket

4. **查看 Kotlin 协程调试**:
```kotlin
// 添加到 Application.kt
System.setProperty("kotlinx.coroutines.debug", "on")
```

---

## 📞 联系信息

如有问题，请查看日志文件：
- `logs/application.log`
- `logs/seniorcareplus.log`

或检查 Railway 部署日志。
