package ru.nsu.manager.controller.internal

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse
import ru.nsu.manager.service.CrackHashService


@RestController
class WorkersController(
    private val crackHashService: CrackHashService,
) {
    @PatchMapping("/internal/api/manager/hash/crack/request")
    fun submitHashes(@RequestBody crackHash: CrackHashWorkerResponse): ResponseEntity<String> {
        crackHashService.submitHashes(crackHash)
        return ResponseEntity.ok("")
    }
}