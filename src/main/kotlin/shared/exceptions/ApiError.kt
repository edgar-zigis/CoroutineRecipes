package shared.exceptions

import kotlinx.serialization.Serializable

@Serializable
data class ApiError(
    override val message: String
) : Error()