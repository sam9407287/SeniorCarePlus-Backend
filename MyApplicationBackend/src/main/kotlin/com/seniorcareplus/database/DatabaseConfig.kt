package com.seniorcareplus.database

import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseConfig {
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)
    
    fun init() {
        try {
            logger.info("正在初始化PostgreSQL資料庫...")
            
            // 連接到PostgreSQL資料庫
            Database.connect(
                url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/seniorcareplus",
                driver = "org.postgresql.Driver",
                user = System.getenv("DATABASE_USER") ?: "postgres",
                password = System.getenv("DATABASE_PASSWORD") ?: "password"
            )
            
            logger.info("PostgreSQL資料庫連接成功")
            
            // 創建表格
            transaction {
                SchemaUtils.create(
                    Patients,
                    HealthRecords,
                    LocationRecords,
                    Devices,
                    Alerts,
                    RemindersTable
                )
            }
            
            logger.info("PostgreSQL資料庫表格創建完成")
            
            // 初始化MongoDB（預留）
            initMongoDB()
            
        } catch (e: Exception) {
            logger.error("PostgreSQL資料庫初始化失敗: ${e.message}")
            
            // 如果PostgreSQL連接失敗，使用H2作為後備
            try {
                logger.warn("正在使用H2內存資料庫作為後備...")
                Database.connect(
                    url = "jdbc:h2:mem:seniorcareplus;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
                    driver = "org.h2.Driver"
                )
                
                transaction {
                    SchemaUtils.create(
                        Patients,
                        HealthRecords,
                        LocationRecords,
                        Devices,
                        Alerts,
                        RemindersTable
                    )
                }
                
                logger.info("H2後備資料庫初始化成功")
            } catch (h2Exception: Exception) {
                logger.error("H2資料庫初始化也失敗: ${h2Exception.message}")
                throw h2Exception
            }
        }
    }
    
    /**
     * 初始化MongoDB連接（預留接口）
     */
    private fun initMongoDB() {
        try {
            // 這裡可以在未來添加MongoDB初始化邏輯
            // 例如：連接MongoDB，創建集合等
            logger.info("MongoDB初始化接口已預留，目前使用PostgreSQL")
        } catch (e: Exception) {
            logger.warn("MongoDB初始化失敗，繼續使用PostgreSQL: ${e.message}")
        }
    }
}