package ru.nsu.manager.dto

import ru.nsu.manager.model.RequestStatus

data class CrackStatusResponse(
    val status: String,
    val data: String
)