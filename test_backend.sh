#!/bin/bash

echo "🚀 SeniorCarePlus 後端測試腳本"
echo "================================"

# 顏色定義
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 錯誤計數
ERROR_COUNT=0

# 檢查函數
check_command() {
    if command -v $1 &> /dev/null; then
        echo -e "${GREEN}✓${NC} $1 已安裝"
        return 0
    else
        echo -e "${RED}✗${NC} $1 未安裝"
        return 1
    fi
}

# HTTP請求測試函數
test_endpoint() {
    local url=$1
    local description=$2
    
    echo -n "測試 $description... "
    
    if curl -s -f "$url" > /dev/null; then
        echo -e "${GREEN}✓ 成功${NC}"
        return 0
    else
        echo -e "${RED}✗ 失敗${NC}"
        ((ERROR_COUNT++))
        return 1
    fi
}

echo -e "${BLUE}1. 檢查必要工具...${NC}"
check_command "java"
check_command "curl"

# 可選工具檢查
if check_command "mosquitto_pub"; then
    MQTT_AVAILABLE=true
else
    MQTT_AVAILABLE=false
    echo -e "${YELLOW}⚠ MQTT工具未安裝，跳過MQTT測試${NC}"
fi

if check_command "jq"; then
    JQ_AVAILABLE=true
else
    JQ_AVAILABLE=false
    echo -e "${YELLOW}⚠ jq未安裝，部分輸出可能不整齊${NC}"
fi

echo

echo -e "${BLUE}2. 等待服務啟動...${NC}"
echo "請確保後端服務正在運行 (./gradlew run)"
sleep 3

echo -e "${BLUE}3. 基本API測試...${NC}"

# 健康檢查
test_endpoint "http://localhost:8080/health" "健康檢查"

# API文檔
test_endpoint "http://localhost:8080/" "API文檔"

# 位置API測試
test_endpoint "http://localhost:8080/api/location/devices" "設備位置列表"
test_endpoint "http://localhost:8080/api/location/gateways" "Gateway列表"
test_endpoint "http://localhost:8080/api/location/anchors" "Anchor設備列表"
test_endpoint "http://localhost:8080/api/location/tags" "Tag設備列表"

echo

echo -e "${BLUE}4. 詳細API響應測試...${NC}"

# 健康檢查詳細輸出
echo "健康檢查詳細響應:"
if $JQ_AVAILABLE; then
    curl -s http://localhost:8080/health | jq .
else
    curl -s http://localhost:8080/health
fi

echo

# 設備列表詳細輸出
echo "設備位置詳細響應:"
if $JQ_AVAILABLE; then
    curl -s http://localhost:8080/api/location/devices | jq '.success, .message, (.data | length)'
else
    curl -s http://localhost:8080/api/location/devices
fi

echo

if $MQTT_AVAILABLE; then
    echo -e "${BLUE}5. MQTT功能測試...${NC}"
    
    # 發送測試位置數據
    echo "發送測試MQTT位置數據..."
    mosquitto_pub -h localhost -t "seniorcareplus/location/TEST001" -m '{
        "deviceId": "TEST001",
        "patientId": "P999", 
        "x": 2.5,
        "y": 1.8,
        "z": 0.0,
        "floor": 1,
        "batteryLevel": 85,
        "signal_strength": -35,
        "timestamp": '$(date +%s000)'
    }'
    
    # 等待處理
    sleep 2
    
    # 驗證數據是否更新
    echo "驗證MQTT數據處理..."
    if test_endpoint "http://localhost:8080/api/location/device/TEST001" "測試設備位置"; then
        echo "MQTT數據流測試: ✓ 成功"
        
        # 顯示設備數據
        echo "測試設備詳細信息:"
        if $JQ_AVAILABLE; then
            curl -s http://localhost:8080/api/location/device/TEST001 | jq '.data'
        else
            curl -s http://localhost:8080/api/location/device/TEST001
        fi
    else
        echo -e "${RED}MQTT數據流測試: ✗ 失敗${NC}"
        ((ERROR_COUNT++))
    fi
    
    echo
    
    # 發送Gateway狀態更新
    echo "發送Gateway狀態更新..."
    mosquitto_pub -h localhost -t "seniorcareplus/gateway/GW001" -m '{
        "gatewayId": "GW001",
        "status": "online",
        "connected_devices": 5,
        "timestamp": '$(date +%s000)'
    }'
    
    sleep 1
    
    # 發送電池狀態更新
    echo "發送電池狀態更新..."
    mosquitto_pub -h localhost -t "seniorcareplus/battery/TEST001" -m '{
        "deviceId": "TEST001",
        "level": 75
    }'
    
    sleep 1
    
else
    echo -e "${YELLOW}5. 跳過MQTT測試 (Mosquitto未安裝)${NC}"
fi

echo -e "${BLUE}6. WebSocket連接測試...${NC}"
echo "注意: WebSocket測試需要手動驗證"
echo "您可以:"
echo "1. 在瀏覽器中打開 WebSocket 測試頁面"
echo "2. 使用 wscat: wscat -c ws://localhost:8080/ws/location"
echo "3. 檢查瀏覽器開發者工具的 WebSocket 連接"

echo

echo -e "${BLUE}7. 系統資源檢查...${NC}"

# 檢查端口占用
echo "檢查端口8080占用:"
if lsof -i :8080 > /dev/null 2>&1; then
    echo -e "${GREEN}✓${NC} 端口8080正在使用中"
    lsof -i :8080
else
    echo -e "${RED}✗${NC} 端口8080未被占用 - 服務可能未啟動"
    ((ERROR_COUNT++))
fi

echo

# 檢查Mosquitto進程
if $MQTT_AVAILABLE; then
    echo "檢查MQTT Broker:"
    if pgrep mosquitto > /dev/null; then
        echo -e "${GREEN}✓${NC} Mosquitto正在運行"
    else
        echo -e "${YELLOW}⚠${NC} Mosquitto未運行，請啟動: brew services start mosquitto"
    fi
fi

echo

echo "================================"
echo -e "${BLUE}測試總結${NC}"
echo "================================"

if [ $ERROR_COUNT -eq 0 ]; then
    echo -e "${GREEN}🎉 所有測試通過！您的後端服務運行正常！${NC}"
    echo
    echo "✅ 測試通過項目:"
    echo "  • 服務啟動正常"
    echo "  • API端點響應正常" 
    echo "  • 數據處理正常"
    if $MQTT_AVAILABLE; then
        echo "  • MQTT消息處理正常"
    fi
    echo
    echo -e "${GREEN}🚀 您的SeniorCarePlus後端已準備就緒！${NC}"
    echo
    echo "下一步可以:"
    echo "1. 啟動Android前端應用進行集成測試"
    echo "2. 連接實際的UWB設備"
    echo "3. 進行壓力測試"
    
else
    echo -e "${RED}❌ 發現 $ERROR_COUNT 個問題${NC}"
    echo
    echo "請檢查:"
    echo "1. 後端服務是否正常啟動 (./gradlew run)"
    echo "2. 所有依賴是否正確安裝"
    echo "3. 數據庫連接是否正常"
    echo "4. MQTT Broker是否運行"
    echo
    echo "詳細排除指南請參考: BACKEND_TESTING_GUIDE.md"
fi

echo
echo "🔗 有用的連結:"
echo "  • API文檔: http://localhost:8080/"
echo "  • 健康檢查: http://localhost:8080/health"
echo "  • WebSocket測試: 請查看測試指南"

exit $ERROR_COUNT 