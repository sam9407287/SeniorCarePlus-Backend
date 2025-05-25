# 使用OpenJDK 11作為基礎鏡像
FROM openjdk:11-jre-slim

# 設置工作目錄
WORKDIR /app

# 複製JAR文件
COPY build/libs/MyApplicationBackend-all.jar app.jar

# 暴露端口
EXPOSE 8080

# 設置環境變量
ENV JAVA_OPTS="-Xmx512m -Xms256m"

# 創建非root用戶
RUN groupadd -r appuser && useradd -r -g appuser appuser
RUN chown -R appuser:appuser /app
USER appuser

# 健康檢查
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# 啟動應用
CMD ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]