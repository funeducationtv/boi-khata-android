package com.boikhata.domain.biometric

interface BiometricAuth {
    suspend fun isAvailable(): Boolean
    suspend fun authenticate(): Boolean
}

class BiometricAuthStub : BiometricAuth {
    override suspend fun isAvailable(): Boolean = false
    override suspend fun authenticate(): Boolean = false
}
