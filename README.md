# SeniorCarePlus Backend | 長者照護系統後端

A simplified backend service for the SeniorCarePlus elderly care system | 長者照護系統的簡化後端服務

## 🚀 Current Status | 當前狀態

### ✅ Successfully Implemented | 成功實現功能
- **Basic REST API Server** | **基礎REST API服務器**: Ktor-based HTTP server running on port 8080 | 基於Ktor的HTTP服務器，運行在8080端口
- **Health Check Endpoint** | **健康檢查端點**: `/health` - Service status monitoring | 服務狀態監控
- **Location API Endpoints** | **位置API端點**: Complete CRUD interface for location services | 完整的位置服務CRUD接口
  - `/api/location/devices` - Device list management | 設備列表管理
  - `/api/location/gateways` - Gateway device management | Gateway設備管理
  - `/api/location/anchors` - UWB anchor device management | UWB錨點設備管理
  - `/api/location/tags` - UWB tag device management | UWB標籤設備管理
- **CORS Configuration** | **CORS配置**: Cross-origin resource sharing enabled | 跨域資源共享已啟用
- **JSON Serialization** | **JSON序列化**: Kotlin serialization with proper formatting | Kotlin序列化，格式正確
- **Gradle Build System** | **Gradle構建系統**: Working build configuration with JVM target 17 | JVM目標17的工作構建配置
- **Java 23 Compatibility** | **Java 23兼容性**: Resolved JVM version conflicts | 解決JVM版本衝突

### ⚠️ Temporarily Removed (Moved to backup/) | 暫時移除功能（移至backup/目錄）
- **Database Integration** | **數據庫集成**: PostgreSQL/H2 database functionality | PostgreSQL/H2數據庫功能
- **MQTT Service** | **MQTT服務**: Real-time message queue telemetry transport | 實時消息隊列遙測傳輸
- **WebSocket Support** | **WebSocket支持**: Real-time bidirectional communication | 實時雙向通信
- **Authentication System** | **認證系統**: JWT-based user authentication | 基於JWT的用戶認證
- **Complex Data Models** | **複雜數據模型**: Full data model implementations | 完整數據模型實現
- **Health Data Services** | **健康數據服務**: Patient health monitoring services | 患者健康監控服務

### 🐛 Known Issues | 已知問題
- Database models need refactoring for compilation compatibility | 數據庫模型需要重構以實現編譯兼容性
- MQTT service requires dependency updates | MQTT服務需要依賴更新
- WebSocket connections need reconnection logic | WebSocket連接需要重連邏輯
- Test suite needs updating for simplified architecture | 測試套件需要為簡化架構更新

## 🏗️ Architecture | 架構

### Current Simplified Architecture | 當前簡化架構
```
Client Apps → REST API (Ktor) → Simple JSON Responses
客戶端應用 → REST API (Ktor) → 簡單JSON響應
```

### Target Full Architecture (Future) | 目標完整架構（未來）
```
Devices → MQTT Broker → Backend Service → Database
設備 → MQTT代理 → 後端服務 → 數據庫
                            ↓
Client Apps ← WebSocket/REST API ← Backend Service
客戶端應用 ← WebSocket/REST API ← 後端服務
```

## ��️ Development Setup | 開發環境設置

### Prerequisites | 先決條件
- Java 17+ (Currently tested with Java 23) | Java 17+（當前使用Java 23測試）
- Gradle 8.4+
- Git

### Quick Start | 快速開始
```bash
# Clone and build | 克隆並構建
git clone <repository-url>
cd SeniorCarePlusBackend
./gradlew build

# Run the service | 運行服務
./gradlew run

# Verify service is running | 驗證服務運行
curl http://localhost:8080/health
```

### Testing | 測試
```bash
# Run automated tests | 運行自動化測試
./test_backend.sh

# Manual API testing | 手動API測試
curl http://localhost:8080/                    # Service info | 服務信息
curl http://localhost:8080/api/location/devices # Devices list | 設備列表
```

## 📁 Project Structure | 項目結構

```
src/main/kotlin/com/seniorcareplus/
├── Application.kt           # Main application entry point | 主應用程序入口點
└── models/
    └── LocationData.kt      # Location data models | 位置數據模型

backup/                      # Temporarily moved complex features | 暫時移動的複雜功能
├── routes/                  # REST API route handlers | REST API路由處理器
├── services/                # Business logic services | 業務邏輯服務
├── database/                # Database configuration | 數據庫配置
├── mqtt/                    # MQTT messaging | MQTT消息傳遞
└── models/                  # Complete data models | 完整數據模型

test_backend.sh             # Automated testing script | 自動化測試腳本
websocket-test.html         # WebSocket testing page | WebSocket測試頁面
API_DOCUMENTATION.md        # Complete API documentation | 完整API文檔
```

## 🔧 Configuration | 配置

### Server Configuration | 服務器配置
- **Host | 主機**: 0.0.0.0 (all interfaces | 所有接口)
- **Port | 端口**: 8080
- **Environment | 環境**: Development mode | 開發模式
- **JVM Target | JVM目標**: 17

### API Endpoints | API端點
| Endpoint | Method | Description | 描述 |
|----------|--------|-------------|------|
| `/` | GET | Service information | 服務信息 |
| `/health` | GET | Health check | 健康檢查 |
| `/api/location/devices` | GET | List all devices | 列出所有設備 |
| `/api/location/gateways` | GET | List gateway devices | 列出Gateway設備 |
| `/api/location/anchors` | GET | List anchor devices | 列出錨點設備 |
| `/api/location/tags` | GET | List tag devices | 列出標籤設備 |

## 🚧 Next Development Steps | 下一步開發步驟

### Phase 1: Restore Core Features | 第一階段：恢復核心功能
1. **Database Integration | 數據庫集成**
   - Fix Exposed ORM compatibility issues | 修復Exposed ORM兼容性問題
   - Restore PostgreSQL/H2 connections | 恢復PostgreSQL/H2連接
   - Implement proper data persistence | 實現適當的數據持久化

2. **MQTT Service | MQTT服務**
   - Update MQTT client dependencies | 更新MQTT客戶端依賴
   - Restore device communication | 恢復設備通信
   - Implement message routing | 實現消息路由

### Phase 2: Advanced Features | 第二階段：高級功能
1. **WebSocket Support | WebSocket支持**
   - Real-time location updates | 實時位置更新
   - Device status broadcasting | 設備狀態廣播
   - Client connection management | 客戶端連接管理

2. **Authentication System | 認證系統**
   - JWT token management | JWT令牌管理
   - User role-based access | 基於用戶角色的訪問
   - API security | API安全

### Phase 3: Production Ready | 第三階段：生產就緒
1. **Monitoring & Logging | 監控和日誌**
2. **Performance Optimization | 性能優化**
3. **Docker Containerization | Docker容器化**
4. **Production Deployment | 生產部署**

## 🧪 Testing | 測試

### Automated Testing | 自動化測試
```bash
./test_backend.sh
```

### Manual Testing | 手動測試
- Service health | 服務健康: `curl http://localhost:8080/health`
- API documentation | API文檔: Open `http://localhost:8080/` in browser | 在瀏覽器中打開
- WebSocket testing | WebSocket測試: Open `websocket-test.html` in browser | 在瀏覽器中打開

## 📚 Documentation | 文檔

- **API Documentation | API文檔**: See `API_DOCUMENTATION.md` | 見`API_DOCUMENTATION.md`
- **Testing Guide | 測試指南**: See `BACKEND_TESTING_GUIDE.md` | 見`BACKEND_TESTING_GUIDE.md`
- **Quick Start | 快速開始**: See `QUICK_START.md` | 見`QUICK_START.md`

## 🤝 Contributing | 貢獻

1. Features should be developed incrementally | 功能應該增量開發
2. All changes must pass existing tests | 所有更改必須通過現有測試
3. Complex features should be added gradually from `backup/` directory | 複雜功能應該從`backup/`目錄逐步添加
4. Maintain backward compatibility with simplified API | 保持與簡化API的向後兼容性

## 📄 License | 許可證

MIT License | MIT許可證

---

**Last Updated | 最後更新**: Current working version - Simplified REST API service | 當前工作版本 - 簡化REST API服務  
**Next Milestone | 下一個里程碑**: Database integration and MQTT service restoration | 數據庫集成和MQTT服務恢復

---

## 🔧 Legacy Features Documentation | 舊版功能文檔

### Original Features (Temporarily Disabled) | 原始功能（暫時禁用）

#### 核心功能 | Core Functions
- **實時健康監控 | Real-time Health Monitoring**: 心率、體溫、尿布狀態監測 | Heart rate, temperature, diaper status monitoring
- **位置追蹤 | Location Tracking**: 患者位置實時監控 | Real-time patient location monitoring  
- **設備管理 | Device Management**: 監控設備狀態和電池電量 | Monitor device status and battery levels
- **智能警報 | Smart Alerts**: 異常情況自動警報 | Automatic alerts for abnormal conditions

#### 技術架構 | Technical Architecture
- **框架 | Framework**: Ktor 2.3.5
- **語言 | Language**: Kotlin 1.9.10
- **數據庫 | Database**: PostgreSQL (主 | Primary) / H2 (備用 | Backup)
- **ORM**: Exposed
- **消息隊列 | Message Queue**: MQTT (Eclipse Paho)
- **序列化 | Serialization**: Kotlinx Serialization
- **日誌 | Logging**: Logback