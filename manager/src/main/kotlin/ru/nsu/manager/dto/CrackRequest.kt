package ru.nsu.manager.dto

data class CrackRequest(
    val hash: String,
    val maxLength: Int,
)