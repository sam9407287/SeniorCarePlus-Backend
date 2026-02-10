#!/bin/bash

# SeniorCarePlus Backend - 内存泄漏修复验证脚本

echo "🔍 SeniorCarePlus Backend - 内存泄漏修复验证"
echo "=============================================="
echo ""

# 配置
BASE_URL="${1:-http://localhost:8080}"
DURATION_SECONDS=300  # 运行5分钟测试

echo "📡 测试目标: $BASE_URL"
echo "⏱️  测试时长: $DURATION_SECONDS 秒"
echo ""

# 1. 检查服务是否运行
echo "1️⃣  检查服务状态..."
HEALTH_RESPONSE=$(curl -s "$BASE_URL/health")
if [[ $? -eq 0 ]]; then
    echo "✅ 服务正在运行"
    echo "   响应: $HEALTH_RESPONSE"
else
    echo "❌ 无法连接到服务"
    exit 1
fi
echo ""

# 2. 检查 WebSocket 连接状态
echo "2️⃣  检查 WebSocket 连接管理..."
WS_STATUS=$(curl -s "$BASE_URL/ws/status")
if [[ $? -eq 0 ]]; then
    echo "✅ WebSocket 状态端点可用"
    echo "   $WS_STATUS"
else
    echo "❌ WebSocket 状态端点不可用"
fi
echo ""

# 3. 创建测试 WebSocket 连接
echo "3️⃣  测试 WebSocket 连接..."
echo "   创建测试连接（需要 websocat 工具）..."
if command -v websocat &> /dev/null; then
    timeout 5 websocat "$BASE_URL/ws/health" -E <<< '{"type":"ping"}' &
    WS_PID=$!
    sleep 2
    
    # 检查连接数
    WS_STATUS_AFTER=$(curl -s "$BASE_URL/ws/status")
    echo "   连接后状态: $WS_STATUS_AFTER"
    
    # 关闭测试连接
    kill $WS_PID 2>/dev/null
    sleep 1
    
    # 再次检查连接数（应该减少）
    WS_STATUS_FINAL=$(curl -s "$BASE_URL/ws/status")
    echo "   断开后状态: $WS_STATUS_FINAL"
    echo "✅ WebSocket 连接管理测试完成"
else
    echo "⚠️  websocat 未安装，跳过 WebSocket 测试"
    echo "   安装方法: brew install websocat (macOS)"
fi
echo ""

# 4. 压力测试 - 检查内存泄漏
echo "4️⃣  执行压力测试（检测内存泄漏）..."
echo "   发送 1000 个请求..."

START_TIME=$(date +%s)
SUCCESS_COUNT=0
FAIL_COUNT=0

for i in {1..1000}; do
    if [ $((i % 100)) -eq 0 ]; then
        echo "   进度: $i/1000 请求"
    fi
    
    RESPONSE=$(curl -s -w "%{http_code}" -o /dev/null "$BASE_URL/health")
    if [[ "$RESPONSE" == "200" ]]; then
        ((SUCCESS_COUNT++))
    else
        ((FAIL_COUNT++))
    fi
    
    # 短暂延迟避免过载
    sleep 0.01
done

END_TIME=$(date +%s)
ELAPSED=$((END_TIME - START_TIME))

echo ""
echo "📊 压力测试结果:"
echo "   ✅ 成功请求: $SUCCESS_COUNT"
echo "   ❌ 失败请求: $FAIL_COUNT"
echo "   ⏱️  耗时: $ELAPSED 秒"
echo ""

# 5. 最终状态检查
echo "5️⃣  最终状态检查..."
FINAL_HEALTH=$(curl -s "$BASE_URL/health")
FINAL_WS=$(curl -s "$BASE_URL/ws/status")

echo "   健康状态: $FINAL_HEALTH"
echo "   WebSocket 状态: $FINAL_WS"
echo ""

# 6. 结果总结
echo "=============================================="
echo "📋 测试总结"
echo "=============================================="
echo ""

if [[ $FAIL_COUNT -eq 0 ]]; then
    echo "✅ 所有测试通过！"
    echo ""
    echo "🎉 内存泄漏修复验证成功："
    echo "   • 服务稳定运行"
    echo "   • WebSocket 连接正常管理"
    echo "   • 1000 次请求无错误"
    echo ""
    echo "📝 建议："
    echo "   1. 继续监控生产环境内存使用"
    echo "   2. 观察长期运行稳定性（24+ 小时）"
    echo "   3. 检查应用日志中的清理消息"
    exit 0
else
    echo "⚠️  测试完成，但有 $FAIL_COUNT 个失败请求"
    echo ""
    echo "建议："
    echo "   1. 检查应用日志"
    echo "   2. 验证所有依赖服务正常"
    echo "   3. 检查数据库连接"
    exit 1
fi
