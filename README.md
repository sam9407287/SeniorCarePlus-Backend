# SeniorCarePlus Backend

一個基於Kotlin和Ktor的老年護理監控系統後端服務，提供實時健康數據收集、處理和警報功能。

## 功能特性

### 核心功能
- **實時健康監控**: 心率、體溫、尿布狀態監測
- **位置追蹤**: 患者位置實時監控
- **設備管理**: 監控設備狀態和電池電量
- **智能警報**: 異常情況自動警報
- **數據存儲**: 支持PostgreSQL和H2數據庫
- **MQTT通信**: 與IoT設備實時通信
- **WebSocket**: 實時數據推送到前端
- **RESTful API**: 完整的HTTP API接口

### 技術架構
- **框架**: Ktor 2.3.5
- **語言**: Kotlin 1.9.10
- **數據庫**: PostgreSQL (主) / H2 (備用)
- **ORM**: Exposed
- **消息隊列**: MQTT (Eclipse Paho)
- **序列化**: Kotlinx Serialization
- **日誌**: Logback

## 快速開始

### 環境要求
- JDK 11 或更高版本
- PostgreSQL 12+ (可選，會自動降級到H2)
- MQTT Broker (如 Mosquitto)

### 安裝步驟

1. **克隆項目**
   ```bash
   cd /Users/sam/Desktop/SeniorCarePlusBackend/MyApplicationBackend
   ```

2. **配置數據庫**
   - 確保PostgreSQL運行在localhost:5432
   - 創建數據庫: `seniorcareplus`
   - 用戶名: `postgres`, 密碼: `password`
   - 或者修改 `src/main/resources/application.conf` 中的配置

3. **配置MQTT**
   - 確保MQTT Broker運行在localhost:1883
   - 或者修改配置文件中的MQTT設置

4. **構建和運行**
   ```bash
   # 使用Gradle Wrapper構建
   ./gradlew build
   
   # 運行應用
   ./gradlew run
   ```

5. **驗證服務**
   - 訪問: http://localhost:8080
   - 健康檢查: http://localhost:8080/health
   - API文檔: http://localhost:8080/api/health/status

## API 接口

### REST API

#### 患者管理
- `GET /api/health/patients` - 獲取所有患者列表
- `GET /api/health/patient/{patientId}` - 獲取特定患者的健康數據

#### 警報管理
- `GET /api/health/alerts` - 獲取活躍警報列表

#### 系統管理
- `GET /api/health/status` - 服務狀態檢查
- `POST /api/health/cleanup` - 清理舊數據

### WebSocket 接口

#### 實時健康數據
```
ws://localhost:8080/ws/health
```
- 每5秒推送患者健康數據更新
- 支持訂閱特定患者數據

#### 實時警報
```
ws://localhost:8080/ws/alerts
```
- 每3秒推送警報更新
- 實時警報通知

## MQTT 主題

### 數據收集主題
- `seniorcareplus/heartrate/{patientId}` - 心率數據
- `seniorcareplus/temperature/{patientId}` - 體溫數據
- `seniorcareplus/diaper/{patientId}` - 尿布狀態
- `seniorcareplus/location/{patientId}` - 位置數據
- `seniorcareplus/device/status/{deviceId}` - 設備狀態

### 消息格式示例

#### 心率數據
```json
{
  "patientId": "patient_001",
  "deviceId": "device_001",
  "heartRate": 75,
  "quality": "good",
  "timestamp": 1640995200000
}
```

#### 體溫數據
```json
{
  "patientId": "patient_001",
  "deviceId": "device_001",
  "temperature": 36.5,
  "unit": "celsius",
  "quality": "good",
  "timestamp": 1640995200000
}
```

#### 位置數據
```json
{
  "patientId": "patient_001",
  "deviceId": "device_001",
  "x": 10.5,
  "y": 20.3,
  "z": 0.0,
  "accuracy": 1.0,
  "area": "room_101",
  "timestamp": 1640995200000
}
```

## 數據庫結構

### 主要表格
- **patients** - 患者基本信息
- **health_records** - 健康數據記錄
- **location_records** - 位置記錄
- **devices** - 設備信息
- **alerts** - 警報記錄
- **reminders** - 提醒事項

## 配置文件

### application.conf
```hocon
ktor {
    deployment {
        port = 8080
        port = ${?PORT}
    }
}

database {
    postgresql {
        url = "jdbc:postgresql://localhost:5432/seniorcareplus"
        user = "postgres"
        password = "password"
        driver = "org.postgresql.Driver"
    }
    h2 {
        url = "jdbc:h2:mem:seniorcareplus;DB_CLOSE_DELAY=-1"
        user = "sa"
        password = ""
        driver = "org.h2.Driver"
    }
}

mqtt {
    broker = "tcp://localhost:1883"
    clientId = "seniorcareplus-backend"
    keepAlive = 60
    cleanSession = true
}
```

## 開發指南

### 添加新的健康數據類型
1. 在 `Models.kt` 中定義數據模型
2. 在 `Tables.kt` 中添加數據庫表結構
3. 在 `MqttService.kt` 中添加MQTT主題處理
4. 在 `DataStorageService.kt` 中添加存儲邏輯

### 添加新的API端點
1. 在 `HealthRoutes.kt` 中定義新路由
2. 在 `DataStorageService.kt` 中添加相應的數據操作方法

### 自定義警報規則
在 `MqttService.kt` 的 `checkHeartRateAlert` 和 `checkTemperatureAlert` 方法中修改警報閾值。

## 部署

### Docker 部署
```dockerfile
FROM openjdk:11-jre-slim
COPY build/libs/MyApplicationBackend-all.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

### 環境變量
- `PORT` - 服務端口 (默認: 8080)
- `DATABASE_URL` - 數據庫連接URL
- `MQTT_BROKER` - MQTT代理地址

## 監控和日誌

### 日誌級別
- INFO: 一般信息
- WARN: 警告信息
- ERROR: 錯誤信息
- DEBUG: 調試信息

### 健康檢查
- 服務狀態: `GET /health`
- 數據庫連接狀態
- MQTT連接狀態

## 故障排除

### 常見問題

1. **數據庫連接失敗**
   - 檢查PostgreSQL是否運行
   - 驗證連接參數
   - 系統會自動降級到H2內存數據庫

2. **MQTT連接失敗**
   - 檢查MQTT Broker是否運行
   - 驗證網絡連接
   - 檢查防火牆設置

3. **WebSocket連接問題**
   - 檢查CORS設置
   - 驗證WebSocket URL
   - 檢查網絡代理設置

## 貢獻指南

1. Fork 項目
2. 創建功能分支
3. 提交更改
4. 推送到分支
5. 創建 Pull Request

## 許可證

MIT License

## 聯繫方式

如有問題或建議，請聯繫開發團隊。