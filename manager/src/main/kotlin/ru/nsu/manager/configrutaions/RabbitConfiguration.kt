package ru.nsu.manager.configrutaions

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


data class RabbitQueue(val queue: String) {
    lateinit var queueName: String
}

@Configuration
class RabbitConfiguration {
    val WORKER_EXCHANGE = "worker_exchange"
    val MANAGE_EXCHANGE = "manager_exchange"

    companion object {
        @JvmStatic
        lateinit var rabbitConn: Connection

        @JvmStatic
        lateinit var workerQueue: RabbitQueue
    }

    init {
        val factory = ConnectionFactory()
        rabbitConn = factory.newConnection("amqp://guest:guest@localhost:5672/")


        // задекларировали обменники, если их там нет (если есть не гаркнет)
        rabbitConn.use { conn ->
            conn.createChannel().use {
                it.exchangeDeclare(WORKER_EXCHANGE, "fanout")
                it.exchangeDeclare(MANAGE_EXCHANGE, "fanout")

                var q = it.queueDeclare()
                it.queueBind(q.queue, WORKER_EXCHANGE, "")

                workerQueue = RabbitQueue(q.queue)
            }
        }
    }


    @Bean(name = ["rabbitConn"])
    fun rabbitConn(): Connection {
        return rabbitConn
    }

    @Bean(name = ["workerQueue"])
    fun workerQueue(): RabbitQueue {
        return workerQueue
    }
}