package ru.nsu.manager.model

data class RequestStatus(
    val requestId: String,
    val partsCount: Int,
    val startTime: Long = System.currentTimeMillis(),
    var status: Status = Status.IN_PROGRESS,
    var donePartsCount: Int = 0,
    val answers: MutableCollection<String> = HashSet()
) {

    fun donePart(words: List<String>) {
        if (status == Status.IN_PROGRESS) {
            answers.addAll(words)
            donePartsCount += 1
        }
        if (partsCount == donePartsCount) {
            status = Status.READY
        }
    }

}