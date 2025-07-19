# SeniorCarePlus Backend

## 🏥 老人照護系統後端服務

基於 Kotlin + Ktor 構建的老人照護IoT數據收集與管理系統，支援實時健康監測、位置追蹤和警報管理。

---

## 📊 功能狀態總覽

### ✅ **已完成功能**

| 功能模組 | 狀態 | 描述 |
|---------|------|------|
| 🏗️ **MQTT服務** | ✅ 運行中 | 連接HiveMQ雲端服務器，接收IoT設備數據 |
| 📍 **UWB位置追蹤** | ✅ 活躍收集 | 實時接收UWB標籤位置數據，6天累積129MB |
| 💾 **H2數據庫** | ✅ 穩定運行 | 文件型數據庫，數據持久化存儲 |
| 🔄 **數據解析引擎** | ✅ 正常 | 自動解析UWB JSON格式，標籤ID映射 |
| 🏥 **患者管理** | ✅ 完成 | 患者信息CRUD，設備關聯管理 |
| 📊 **REST API** | ✅ 可用 | `/api/patients`, `/health` 端點 |
| 🔍 **WebSocket支持** | ✅ 實現 | 實時數據流推送 |
| 📝 **日誌系統** | ✅ 完整 | 詳細的操作和錯誤日誌 |

### 🔧 **系統就緒，待設備接入**

| 功能模組 | 狀態 | 需求 |
|---------|------|------|
| ❤️ **心率監測** | 🔧 待接入 | 需要心率感測器/手環設備 |
| 🌡️ **體溫監測** | 🔧 待接入 | 需要體溫貼片/感測器 |
| 🍼 **尿布監測** | 🔧 待接入 | 需要智能尿布/濕度感測器 |
| 🚨 **緊急警報** | 🔧 待接入 | 需要緊急按鈕/跌倒偵測器 |

### ⚠️ **已知問題與限制**

| 問題類別 | 描述 | 影響等級 | 解決方案 |
|---------|------|---------|---------|
| 🐘 **PostgreSQL連接** | 本地PostgreSQL未安裝 | 🟡 中等 | 安裝PostgreSQL或使用Supabase |
| 🔒 **SSL證書** | 使用TrustAll SSL配置 | 🟡 中等 | 生產環境需要正式證書 |
| 📈 **並發性能** | H2數據庫並發限制 | 🟢 低 | 考慮遷移到PostgreSQL |
| 🏷️ **標籤映射** | 硬編碼UWB標籤ID | 🟢 低 | 實現動態設備註冊 |

---

## 🏗️ 數據庫架構

### 📋 **數據庫架構圖**

```
┌─────────────────────────────────────────────────────────────────┐
│                   SeniorCarePlus Database Schema                │
│                        (H2 File Database)                      │
│                     File: ./data/seniorcareplus.mv.db          │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────┐    1:N    ┌─────────────────┐
│    Patients     │◄─────────►│ HealthRecords   │
│                 │           │                 │
│ • id (PK)       │           │ • id (PK)       │
│ • name          │           │ • patient_id (FK)│
│ • room          │           │ • data_type     │
│ • device_id ⭐   │           │ • value (JSON)  │
│ • age           │           │ • device_id     │
│ • gender        │           │ • quality       │
│ • emergency_contact         │ • unit          │
│ • created_at    │           │ • timestamp     │
│ • updated_at    │           │ • created_at    │
└─────────────────┘           └─────────────────┘
        │
        │ 1:N
        ▼
┌─────────────────┐           ┌─────────────────┐
│ LocationRecords │           │     Alerts      │
│                 │           │                 │
│ • id (PK)       │    1:N    │ • id (PK)       │
│ • patient_id (FK)│◄─────────►│ • patient_id (FK)│
│ • x, y, z 📍     │           │ • alert_type    │
│ • accuracy      │           │ • title         │
│ • area          │           │ • message       │
│ • device_id     │           │ • severity      │
│ • timestamp     │           │ • status        │
│ • created_at    │           │ • device_id     │
└─────────────────┘           │ • triggered_at  │
        ▲                     │ • acknowledged_at│
        │                     │ • resolved_at   │
     📡 UWB Data              │ • created_at    │
    (129MB/6days)             └─────────────────┘
                                      ▲
                                      │ 1:N
┌─────────────────┐                   │
│    Devices      │           ┌─────────────────┐
│                 │           │  RemindersTable │
│ • id (PK)       │           │                 │
│ • device_id ⭐   │           │ • id (PK)       │
│ • device_type   │           │ • patient_id (FK)│
│ • status        │           │ • title         │
│ • last_seen     │           │ • description   │
│ • battery_level │           │ • reminder_type │
│ • firmware_version          │ • scheduled_time│
│ • created_at    │           │ • is_completed  │
│ • updated_at    │           │ • completed_at  │
└─────────────────┘           │ • created_at    │
                              │ • updated_at    │
                              └─────────────────┘
```

### 🔑 **核心表說明**

**患者表 (Patients)**
- 系統核心實體，存儲老人基本信息
- `device_id` 字段連接各種IoT設備

**位置記錄表 (LocationRecords)**  
- 實時UWB位置追蹤數據
- 目前累積：129MB數據 (6天連續收集)
- 包含3D坐標、精度、區域信息

**健康記錄表 (HealthRecords)**
- 通用健康數據存儲 (JSON格式)
- 支持：心率、體溫、尿布狀態等

**警報表 (Alerts)**
- 緊急事件管理
- 支持：emergency/warning/info 三級警報

---

## 🚀 快速開始

### 📋 **系統需求**

- Java 17+
- Kotlin 1.9+
- Gradle 8.4+

### 🔧 **安裝步驟**

1. **克隆項目**
```bash
git clone https://github.com/sam9407287/SeniorCarePlus-Backend.git
cd SeniorCarePlus-Backend
```

2. **啟動服務**
```bash
./gradlew run
```

3. **驗證服務**
```bash
curl http://localhost:8080/health
```

### 🌐 **API端點**

| 端點 | 方法 | 描述 |
|------|------|------|
| `/health` | GET | 服務健康檢查 |
| `/api/patients` | GET | 獲取患者列表 |
| `/api/health/{deviceId}` | GET | 獲取指定設備健康數據 |
| `/api/alerts` | GET | 獲取警報列表 |
| `/ws/health` | WebSocket | 實時健康數據流 |

---

## 📊 **數據收集統計**

### 📈 **當前數據規模**

- **收集時間**: 2025/07/14 12:17 - 2025/07/20 07:10  
- **持續天數**: 6天不間斷  
- **位置數據**: 129MB (約21MB/天)  
- **記錄頻率**: 每秒多次UWB位置更新  
- **標籤數量**: 2個活躍UWB標籤  

### 🏷️ **標籤映射**

| 標籤ID | 16進制 | 對應患者 | 狀態 |
|--------|--------|----------|------|
| 1770 | 0x06EA | device_001 | ✅ 活躍 |
| 13402 | 0x345A | device_002 | ✅ 活躍 |

---

## 🔮 **未來規劃**

### 🎯 **短期目標 (1-2週)**

- [ ] PostgreSQL雲端數據庫集成
- [ ] 心率監測設備接入測試
- [ ] API文檔完善 (Swagger)
- [ ] 單元測試覆蓋

### 🚀 **中期目標 (1-2個月)**

- [ ] 實時警報推送系統
- [ ] 數據分析儀表板
- [ ] 移動端管理App集成
- [ ] 多租戶支持

### 🌟 **長期願景 (3-6個月)**

- [ ] AI健康預警系統
- [ ] 家屬通知系統
- [ ] 醫療記錄集成
- [ ] 跨機構數據共享

---

## 🛠️ **技術棧**

### 🔧 **後端技術**

- **Framework**: Ktor 2.3.5
- **Language**: Kotlin 1.9.10  
- **Database**: H2 (文件型) / PostgreSQL (雲端)
- **ORM**: Exposed
- **MQTT**: Eclipse Paho
- **Build Tool**: Gradle 8.4

### 📡 **IoT技術**

- **位置追蹤**: UWB (Ultra-Wideband) 技術
- **通訊協議**: MQTT over WebSocket (SSL)
- **雲端服務**: HiveMQ Cloud
- **數據格式**: JSON

---

## 📞 **聯絡信息**

- **Project**: SeniorCarePlus IoT Backend
- **GitHub**: https://github.com/sam9407287/SeniorCarePlus-Backend
- **Version**: 1.0.0
- **Last Updated**: 2025/07/20

---

## 📄 **授權**

MIT License - 詳見 LICENSE 文件