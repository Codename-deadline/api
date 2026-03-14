package xyz.om3lette.deadlines_api.controllers

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import xyz.om3lette.deadlines_api.services.MetadataService

@RestController
@RequestMapping("/api/metadata")
@Tag(name = "Metadata", description = "Application metadata versions")
class MetadataController(
    private val metadataService: MetadataService
) {
    @GetMapping
    @Operation(summary = "Metadata SHA-256 hashes")
    fun getMetadataVersions() = metadataService.response
}
