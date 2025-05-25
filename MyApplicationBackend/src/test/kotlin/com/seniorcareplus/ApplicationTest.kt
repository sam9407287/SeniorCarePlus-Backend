package com.seniorcareplus

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.*

class ApplicationTest {
    
    @Test
    fun testRoot() = testApplication {
        application {
            module()
        }
        
        client.get("/").apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertEquals("SeniorCarePlus Backend", response["service"]?.jsonPrimitive?.content)
            assertEquals("running", response["status"]?.jsonPrimitive?.content)
        }
    }
    
    @Test
    fun testHealthCheck() = testApplication {
        application {
            module()
        }
        
        client.get("/health").apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertEquals("healthy", response["status"]?.jsonPrimitive?.content)
        }
    }
    
    @Test
    fun testApiHealthStatus() = testApplication {
        application {
            module()
        }
        
        client.get("/api/health/status").apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertTrue(response["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        }
    }
    
    @Test
    fun testGetPatients() = testApplication {
        application {
            module()
        }
        
        client.get("/api/health/patients").apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertTrue(response["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        }
    }
    
    @Test
    fun testGetAlerts() = testApplication {
        application {
            module()
        }
        
        client.get("/api/health/alerts").apply {
            assertEquals(HttpStatusCode.OK, status)
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertTrue(response["success"]?.jsonPrimitive?.content?.toBoolean() ?: false)
        }
    }
    
    @Test
    fun testNotFoundEndpoint() = testApplication {
        application {
            module()
        }
        
        client.get("/nonexistent").apply {
            assertEquals(HttpStatusCode.NotFound, status)
            val response = Json.parseToJsonElement(bodyAsText()).jsonObject
            assertFalse(response["success"]?.jsonPrimitive?.content?.toBoolean() ?: true)
        }
    }
}