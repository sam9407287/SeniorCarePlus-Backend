package com.example.models

import kotlinx.serialization.Serializable

@Serializable
data class PatientEvent(
    val patientId: String,
    val type: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val locationData: LocationData? = null
) 