package ru.nsu.manager.controller.api

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import ru.nsu.manager.dto.CrackRequest
import ru.nsu.manager.dto.CrackResponse
import ru.nsu.manager.dto.CrackStatusResponse
import ru.nsu.manager.model.RequestStatus
import ru.nsu.manager.service.CrackHashService
import java.util.*

@RestController
@RequestMapping("/api/hash")
class CrackHashController(
    private val crackHashService: CrackHashService,
) {

    val logger: Logger? = LoggerFactory.getLogger(CrackHashController::class.java)

    @PostMapping("/crack")
    fun crack(@RequestBody request: CrackRequest): ResponseEntity<CrackResponse> {
        val result = crackHashService.createTask(request = request)
        return ResponseEntity.ok(CrackResponse(requestId = result))
    }

    @GetMapping("/status")
    fun getStatus(@RequestParam requestId: String): ResponseEntity<CrackStatusResponse> {
        val status: RequestStatus = try {
            crackHashService.getTaskStatus(requestId)
        } catch (e: IllegalStateException) {
            return ResponseEntity.notFound().build()
        }
        val response = CrackStatusResponse(
            status = status.status.name,
            data = status.toString()
        )
        logger?.info("Status$response")
        return ResponseEntity.ok(response)
    }
}