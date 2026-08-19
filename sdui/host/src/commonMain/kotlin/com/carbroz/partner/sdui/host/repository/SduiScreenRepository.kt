package com.carbroz.partner.sdui.host.repository

import com.carbroz.partner.infrastructure.network.client.NetworkClient
import com.carbroz.partner.infrastructure.network.client.NetworkRequest

public interface SduiScreenRepository {
    public suspend fun fetchScreenJson(endpoint: String): Result<String>
}

public class DefaultSduiScreenRepository(
    private val networkClient: NetworkClient
) : SduiScreenRepository {

    override suspend fun fetchScreenJson(endpoint: String): Result<String> {
        return try {
            val response = networkClient.execute(
                NetworkRequest(
                    url = endpoint,
                    method = "GET"
                )
            )
            if (response.isSuccessful) {
                Result.success(response.bodyJson)
            } else {
                Result.failure(Exception("Failed to fetch SDUI screen from '$endpoint': HTTP status ${response.statusCode}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
