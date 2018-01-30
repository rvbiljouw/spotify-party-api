package uk.bipush.queue.rmq

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class RMQPublisher<T>(uri: String,
                      val name: String,
                      exchangeType: String,
                      val exchange: Boolean,
                      durable: Boolean = true,
                      exclusive: Boolean = false,
                      autodelete: Boolean = false) {
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

        if (!exchange) {
            channel.queueDeclare(name, durable, exclusive, autodelete, mapOf())
        } else {
            channel.exchangeDeclare(name, exchangeType, durable, autodelete, mapOf())
        }
    }

    fun publish(obj: T, props: AMQP.BasicProperties? = null) {
        val msg = mapper.writeValueAsBytes(obj)
        if (exchange) {
            channel.basicPublish(name, "", props, msg)
        } else {
            channel.basicPublish("", name, null, msg)
        }
    }

}