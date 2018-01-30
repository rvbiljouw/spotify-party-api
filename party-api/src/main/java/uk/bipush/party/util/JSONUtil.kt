package uk.bipush.party.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import java.net.URLDecoder

object JSONUtil {
    private val objectMapper = ObjectMapper()
            .registerModule(KotlinModule())
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun formToJSON(form: String): String {
        if (form.startsWith("{") || form.startsWith("[")) {
            return form
        }
        val split = form.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val map = HashMap<String, String>()
        for (s in split) {
            val keyValue = s.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (keyValue.size != 2) {
                continue
            }
            val key = keyValue[0]
            val value = URLDecoder.decode(keyValue[1], "UTF-8")
            map.put(key, value)
        }

        return  objectMapper.writeValueAsString(map)
    }
}