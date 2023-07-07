package com.vgaltes.simplytheflag

class StringConfig(override val cacheMillis: Long, private val rawParameters: String) : ConfigValue {
    private var value = ""
    override val type: String
        get() = StringConfig::class.java.typeName

    override fun value(parameters: Array<out Any?>): String {
        return value
    }

    init {
        val jsonNode = JacksonModule.OBJECT_MAPPER.readTree(rawParameters)
        value = jsonNode["value"].asText()
    }
}