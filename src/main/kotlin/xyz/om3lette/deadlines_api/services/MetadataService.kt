package xyz.om3lette.deadlines_api.services

import org.springframework.stereotype.Service
import tools.jackson.databind.SerializationFeature
import tools.jackson.databind.json.JsonMapper
import xyz.om3lette.deadlines_api.data.metadata.response.MetadataResponse
import java.security.MessageDigest

@Service
class MetadataService(
    private val rolesService: RolesService
) {
    private fun computeHash(obj: Any): String {
        val mapper = JsonMapper.builder()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
            .build()

        val json = mapper.writeValueAsString(obj)
        val digest = MessageDigest
            .getInstance("SHA-256")
            .digest(json.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    val response: MetadataResponse by lazy {
        MetadataResponse(
            rolesMetadataVersion = computeHash(rolesService.metadata)
        )
    }
}