package com.example.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory

object DatabaseConfig {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private var initialized = false
    
    /**
     * 初始化数据库连接
     */
    fun init() {
        if (initialized) return
        
        try {
            // 配置数据库连接池
            val config = HikariConfig().apply {
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = "jdbc:postgresql://localhost:5432/myapplication_db"
                username = "postgres"
                password = "postgres"
                maximumPoolSize = 10
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            }
            
            val dataSource = HikariDataSource(config)
            Database.connect(dataSource)
            
            // 创建数据库表
            transaction {
                SchemaUtils.create(
                    Devices, 
                    Patients, 
                    PatientEvents,
                    PatientVitalSigns
                )
            }
            
            logger.info("数据库初始化成功")
            initialized = true
        } catch (e: Exception) {
            logger.error("数据库初始化失败: ${e.message}", e)
            throw e
        }
    }
} 