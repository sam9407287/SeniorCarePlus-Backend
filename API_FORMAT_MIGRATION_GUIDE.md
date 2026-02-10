# API 格式遷移指南 - cloudData 加前綴格式

## 概述

本指南說明如何將後端 API 從舊的巢狀 `cloudData` 格式遷移到新的平坦化「cloudData 加前綴」格式。

### 變更時間軸
1. **第1步**：更新資料模型（已完成）
2. **第2步**：建立轉換幫助工具（已完成）
3. **第3步**：修改 API 路由（進行中）
4. **第4步**：測試驗證（待進行）

---

## Anchor API 變更

### 建立 Anchor (POST /api/anchors)

#### 舊格式（不再支援）
```json
{
  "name": "DW30C5",
  "macAddress": "ANCHOR:12485",
  "position": {"x": 59.65, "y": -69.24, "z": 1},
  "cloudData": {
    "id": 12485,
    "gateway_id": 4192812156,
    "position": {"x": -150.64, "y": 1.33, "z": 1},
    "receivedAt": "2025-11-11T15:58:05.295Z"
  }
}
```

#### 新格式（必須使用）
```json
{
  "name": "DW30C5",
  "macAddress": "ANCHOR:12485",
  "position": {"x": 59.65, "y": -69.24, "z": 1},
  "cloudDataId": 12485,
  "cloudDataGatewayId": 4192812156,
  "cloudDataPosition": {"x": -150.64, "y": 1.33, "z": 1},
  "cloudDataReceivedAt": "2025-11-11T15:58:05.295Z",
  "cloudDataFwUpdate": 0,
  "cloudDataLed": 1,
  "cloudDataBle": 1,
  "cloudDataInitiator": 1
}
```

#### 後端處理流程
1. 接收新格式的 `CreateAnchorRequest`
2. 使用 `DataTransformationHelper.convertCreateAnchorRequestToCloudDataJson()` 轉換為 JSON 字符串
3. 存儲轉換後的 JSON 到資料庫 `anchors.cloud_data` 列
4. 返回時，使用 `DataTransformationHelper.convertStoredJsonToAnchorData()` 轉換回前端格式

#### 實作示例
```kotlin
// 創建 Anchor
post("/anchors") {
    try {
        val request = call.receive<CreateAnchorRequest>()
        val anchorId = request.id ?: "anchor_${System.currentTimeMillis()}"
        
        // 轉換 cloudData
        val cloudDataJson = DataTransformationHelper.convertCreateAnchorRequestToCloudDataJson(request)
        
        transaction {
            Anchors.insert {
                it[Anchors.anchorId] = anchorId
                it[name] = request.name
                it[macAddress] = request.macAddress
                it[position] = Json.encodeToString(request.position ?: PositionData(0.0, 0.0, 0.0))
                it[cloudData] = cloudDataJson  // 存儲轉換後的 JSON
                it[status] = request.status ?: "offline"
                it[lastSeen] = request.lastSeen?.let {
                    try { java.time.LocalDateTime.parse(it) } catch (e: Exception) { null }
                }
                it[gatewayId] = request.gatewayId
            }
        }
        
        // 返回新格式的 AnchorData
        val newAnchor = transaction {
            Anchors.select { Anchors.anchorId eq anchorId }.single().let { row ->
                DataTransformationHelper.convertStoredJsonToAnchorData(
                    id = row[Anchors.anchorId],
                    name = row[Anchors.name],
                    macAddress = row[Anchors.macAddress],
                    position = Json.decodeFromString(row[Anchors.position]),
                    cloudDataJson = row[Anchors.cloudData],
                    gatewayId = row[Anchors.gatewayId],
                    status = row[Anchors.status],
                    lastSeen = row[Anchors.lastSeen]?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                    isBound = row[Anchors.isBound],
                    createdAt = row[Anchors.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                )
            }
        }
        
        call.respond(HttpStatusCode.Created, newAnchor)
    } catch (e: Exception) {
        call.respond(
            HttpStatusCode.InternalServerError,
            ApiResponse<Nothing>(success = false, message = "建立 Anchor 失敗: ${e.message}")
        )
    }
}
```

---

## Gateway API 變更

### 建立 Gateway (POST /api/gateways)

#### 舊格式（不再支援）
```json
{
  "floorId": "floor_1759425596164",
  "name": "GwF9E516B8_197",
  "macAddress": "GW:F9E516B8",
  "cloudData": {
    "gateway_id": 4192540344,
    "content": "gateway topic",
    "uwb_tx_power": {"boost_norm": 6.5, "boost_500": 9},
    "pub_topic": {"anchor_config": "UWB/GW16B8_AncConf"}
  }
}
```

#### 新格式（必須使用）
```json
{
  "floorId": "floor_1759425596164",
  "name": "GwF9E516B8_197",
  "macAddress": "GW:F9E516B8",
  "ipAddress": "192.168.1.100",
  "cloudDataGatewayId": 4192540344,
  "cloudDataContent": "gateway topic",
  "cloudDataUwbTxPower": {"boost_norm": 6.5, "boost_500": 9},
  "cloudDataPubTopic": {"anchor_config": "UWB/GW16B8_AncConf"},
  "cloudDataReceivedAt": "2025-11-15T15:29:42.213Z"
}
```

#### 後端處理流程
1. 接收新格式的 `CreateGatewayRequest`
2. 使用 `DataTransformationHelper.convertCreateGatewayRequestToCloudDataJson()` 轉換為 JSON 字符串
3. 存儲轉換後的 JSON 到資料庫 `gateways.cloud_data` 列
4. 返回時，使用 `DataTransformationHelper.convertStoredJsonToGatewayData()` 轉換回前端格式

---

## 資料庫存儲格式

### Anchors 表
```
cloud_data 列存儲的 JSON 格式：
{
  "id": 12485,
  "gateway_id": 4192812156,
  "name": "DW30C5",
  "node": "ANCHOR",
  "content": "config",
  "fw_update": 0,
  "led": 1,
  "ble": 1,
  "initiator": 1,
  "position": {"x": -150.64, "y": 1.33, "z": 1},
  "receivedAt": "2025-11-11T15:58:05.295Z"
}
```

### Gateways 表
```
cloud_data 列存儲的 JSON 格式：
{
  "content": "gateway topic",
  "gateway_id": 4192540344,
  "fw_ver": "0.82838s",
  "uwb_tx_power": {"boost_norm": 6.5, "boost_500": 9, ...},
  "pub_topic": {"anchor_config": "UWB/...", ...},
  "sub_topic": {"downlink": "UWB/..."},
  ...所有其他cloudData字段...
}
```

---

## API 返回格式

### GET /api/anchors/{id}

```json
{
  "id": "anchor_1762876688408",
  "gatewayId": "gw_1762873074837",
  "name": "DW30C5",
  "macAddress": "ANCHOR:12485",
  "status": "active",
  "position": {"x": 59.65, "y": -69.24, "z": 1},
  "lastSeen": "2025-11-11T15:58:05.295Z",
  "isBound": true,
  "createdAt": "2025-11-11T15:58:08.408Z",
  "cloudData": {
    "id": 12485,
    "gatewayId": 4192812156,
    "name": "DW30C5",
    "node": "ANCHOR",
    "content": "config",
    "fwUpdate": 0,
    "led": 1,
    "ble": 1,
    "initiator": 1,
    "position": {"x": -150.64, "y": 1.33, "z": 1},
    "receivedAt": "2025-11-11T15:58:05.295Z"
  }
}
```

### GET /api/gateways/{id}

```json
{
  "id": "gw_1763227337762",
  "floorId": "floor_1759425596164",
  "name": "GwF9E516B8_197",
  "macAddress": "GW:F9E516B8",
  "ipAddress": "192.168.1.100",
  "status": "online",
  "createdAt": "2025-11-15T17:22:17.762Z",
  "cloudDataContent": "gateway topic",
  "cloudDataGatewayId": 4192540344,
  "cloudDataFwVer": "0.82838s",
  "cloudDataUwbTxPower": {"boost_norm": 6.5, "boost_500": 9, "boost_250": 11.5, "boost_125": 14},
  "cloudDataPubTopic": {"anchor_config": "UWB/GW16B8_AncConf", ...},
  "cloudDataSubTopic": {"downlink": "UWB/GW16B8_Dwlink"},
  "cloudDataReceivedAt": "2025-11-15T15:29:42.213Z"
}
```

---

## 待實施的修改

以下 API 路由需要更新為使用新的轉換邏輯：

### Anchor 路由
- [ ] `GET /api/anchors` - 使用轉換工具返回新格式
- [ ] `GET /api/anchors/{id}` - 使用轉換工具返回新格式
- [ ] `POST /api/anchors` - 接收新格式並轉換存儲
- [ ] `PUT /api/anchors/{id}` - 接收新格式並轉換存儲
- [ ] `DELETE /api/anchors/{id}` - 無需改動

### Gateway 路由
- [ ] `GET /api/gateways` - 使用轉換工具返回新格式
- [ ] `GET /api/gateways/{id}` - 使用轉換工具返回新格式
- [ ] `GET /api/floors/{floorId}/gateways` - 使用轉換工具返回新格式
- [ ] `POST /api/gateways` - 接收新格式並轉換存儲
- [ ] `PUT /api/gateways/{id}` - 接收新格式並轉換存儲
- [ ] `DELETE /api/gateways/{id}` - 無需改動

---

## 前端變更要點

前端工程師需要：
1. 從舊的巢狀 `cloudData` 格式改為新的平坦化加前綴格式
2. 發送 API 時使用新格式
3. 接收 API 返回時解析新格式（`cloudData` 物件 + 加前綴標量字段）

---

## 測試清單

- [ ] 使用新格式成功建立 Anchor
- [ ] 使用新格式成功建立 Gateway
- [ ] 查詢 Anchor 返回正確的新格式
- [ ] 查詢 Gateway 返回正確的新格式
- [ ] 驗證資料庫存儲的 JSON 格式正確
- [ ] 前端能正確解析返回的新格式







