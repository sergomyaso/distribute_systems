package ru.nsu.manager.service

import com.rabbitmq.client.*
import jakarta.xml.bind.JAXBContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashManagerRequest
import ru.nsu.ccfit.schema.crack_hash_response.CrackHashWorkerResponse
import ru.nsu.manager.configrutaions.RabbitQueue
import ru.nsu.manager.dto.CrackRequest
import ru.nsu.manager.model.RequestStatus
import ru.nsu.manager.model.Status
import java.io.ByteArrayInputStream
import ru.nsu.manager.repository.RequestRepository
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


@Service
@EnableAutoConfiguration
class CrackHashServiceImpl @Autowired constructor(
    private val rabbitConn: Connection,
    private val workerQueue: RabbitQueue,
    private val requestRepository: RequestRepository
) : CrackHashService {
    private val restTemplate: RestTemplate = RestTemplate()
    private val requestStatuses: ConcurrentHashMap<String, RequestStatus> = ConcurrentHashMap()
    private val queueWorkerResp: ConcurrentLinkedQueue<CrackHashWorkerResponse> = ConcurrentLinkedQueue()

    val logger: Logger? = LoggerFactory.getLogger(CrackHashServiceImpl::class.java)
    val WORKER_EXCHANGE = "worker_exchange"
    val MANAGE_EXCHANGE = "manager_exchange"

    init {
        rabbitConn.createChannel().use {
            val deliverCallback = DeliverCallback { consumerTag: String?, delivery: Delivery? ->
                val message = delivery?.body?.let { String(it, Charsets.UTF_8) }
                println("Received '$message'")

                val jaxbContext = JAXBContext.newInstance(CrackHashWorkerResponse::class.java)
                val unmarshaller = jaxbContext.createUnmarshaller()
                val resp: CrackHashWorkerResponse;
                if (message != null) {
                    resp =
                        unmarshaller.unmarshal(ByteArrayInputStream(message.toByteArray())) as CrackHashWorkerResponse
                    queueWorkerResp.add(resp);
                }
                it.basicAck(delivery?.envelope?.deliveryTag ?: 0, false)
            }

            it.basicConsume(workerQueue.queueName, false, deliverCallback, CancelCallback { })
        }
    }

    override fun createTask(request: CrackRequest): String {
        val requestId: String = UUID.randomUUID().toString()
        val requestStatus = RequestStatus(requestId = requestId, partsCount = WORKERS)
        requestStatuses[requestId] = requestStatus
        println(requestId)
        requestRepository.save(requestStatus)
        sendTasksToWorkers(request, requestId)
        return requestId
    }


    private fun sendTasksToWorkers(request: CrackRequest, requestId: String) {
        for (worker in 0 until WORKERS) {
            val requestToWorker = CrackHashManagerRequest()
            requestToWorker.requestId = requestId
            requestToWorker.hash = request.hash
            requestToWorker.maxLength = request.maxLength
            val alphabet: CrackHashManagerRequest.Alphabet = CrackHashManagerRequest.Alphabet()
            alphabet.symbols.addAll(ALPHABET.split(""))
            requestToWorker.alphabet = alphabet
            requestToWorker.partCount = WORKERS
            requestToWorker.partNumber = worker
            sendTaskToManagerQueue(requestToWorker)

//            val httpEntity: HttpEntity<CrackHashManagerRequest> = HttpEntity(requestToWorker)
//            val url = "http://worker:8080/internal/api/worker/hash/crack/task"
//            val responseEntity: ResponseEntity<String> = restTemplate.exchange(
//                url, HttpMethod.POST, httpEntity, String::class.java
//            )
        }
    }

    override fun submitHashes(crackHash: CrackHashWorkerResponse) {
        val id = crackHash.requestId
        val requestStatus = requestStatuses[id]
        requestStatus?.donePart(crackHash.answers.words)
    }

    override fun getTaskStatus(requestId: String): RequestStatus {
        return requestStatuses[requestId] ?: throw IllegalStateException("Request not found")
    }


    private fun sendTaskToManagerQueue(req: CrackHashManagerRequest) {
//        val jaxbContext = JAXBContext.newInstance(CrackHashWorkerResponse::class.java)
//        val marshaller = jaxbContext.createMarshaller()
//
        rabbitConn.createChannel().basicPublish(MANAGE_EXCHANGE, "", null, req.toString().toByteArray())
    }

    @Scheduled(fixedRate = 30)
    fun checkQueueWorkerResponses() {
        if (queueWorkerResp.size > 0) {
            submitHashes(queueWorkerResp.poll())
        }
    }

    @Scheduled(fixedRate = 1000)
    fun checkTaskStatuses() {
        requestStatuses.values
            .filter { requestStatus -> requestStatus.status === Status.IN_PROGRESS }
            .forEach { status ->
                // check if all tasks have been completed
                if (System.currentTimeMillis() - status.startTime > TIMEOUT_MS) {
                    status.status = Status.ERROR
                }
            }
    }

    companion object {
        private const val WORKERS = 1
        private const val TIMEOUT_MS: Long = 30000 // 30 seconds
        private const val ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789"

    }
}