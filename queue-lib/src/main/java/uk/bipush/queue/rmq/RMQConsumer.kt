package uk.bipush.queue.rmq

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rabbitmq.client.*

class RMQConsumer<T>(uri: String,
                     val name: String,
                     exchangeType: String,
                     val exchange: Boolean,
                     durable: Boolean = true,
                     exclusive: Boolean = false,
                     autodelete: Boolean = false,
                     callback: ConsumerCallback<T>,
                     dataType: Class<T>) {
    private val mapper = ObjectMapper()
            .registerModule(KotlinModule())
            .registerModule(JodaModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    private val conn: Connection
    private val channel: Channel

    init {
        val factory = ConnectionFactory()
        factory.setUri(uri)
        conn = factory.newConnection()
        channel = conn.createChannel()

        var queue: String = name

        if (!exchange) {
            queue = channel.queueDeclare(name, durable, exclusive, autodelete, mapOf()).queue
        } else {
            channel.exchangeDeclare(name, exchangeType, durable, autodelete, mapOf())
            queue = channel.queueDeclare().queue
            channel.queueBind(queue, name, "")
        }

        channel.basicConsume(queue, object : DefaultConsumer(channel) {
            override fun handleDelivery(consumerTag: String?, envelope: Envelope?, properties: AMQP.BasicProperties?, body: ByteArray) {
                var message: T? = null
                if (dataType != String::class.java) {
                    message = mapper.readValue(body, dataType)
                } else {
                    message = String(body) as T
                }
                if (message != null) {
                    callback.handle(message, ConsumerResult(super.getChannel(), envelope?.deliveryTag ?: -1))
                }
            }
        })
    }

}

interface ConsumerCallback<T> {
    fun handle(msg: T, result: ConsumerResult)
}

class ConsumerResult(private val channel: Channel, private val tag: Long) {

    fun ack(multiple: Boolean = false) {
        channel.basicAck(tag, multiple)
    }


    fun nack(multiple: Boolean = false, requeue: Boolean = false) {
        channel.basicNack(tag, multiple, requeue)
    }

    fun reject(requeue: Boolean = false) {
        channel.basicReject(tag, requeue)
    }

}