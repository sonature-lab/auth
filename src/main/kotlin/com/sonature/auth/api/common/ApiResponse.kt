package com.sonature.auth.api.common

import com.fasterxml.jackson.annotation.JsonInclude
import java.util.UUID

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse<T>(
    val data: T? = null,
    val error: ApiError? = null,
    val requestId: String = UUID.randomUUID().toString()
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(data = data)
        fun <T> error(error: ApiError): ApiResponse<T> = ApiResponse(error = error)
    }
}
