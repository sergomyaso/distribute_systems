package ru.nsu.manager.service

import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse
import ru.nsu.manager.dto.CrackRequest
import ru.nsu.manager.model.RequestStatus


interface CrackHashService {
    fun getTaskStatus(requestId: String): RequestStatus

    fun createTask(request: CrackRequest): String

    fun submitHashes(crackHash: CrackHashWorkerResponse)
}