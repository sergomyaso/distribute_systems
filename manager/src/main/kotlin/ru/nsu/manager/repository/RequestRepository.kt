package ru.nsu.manager.repository

import org.springframework.data.mongodb.repository.MongoRepository
import ru.nsu.manager.model.RequestStatus

interface RequestRepository: MongoRepository<RequestStatus, String> {
    fun findRequestStatusByRequestId(id: String): RequestStatus
}