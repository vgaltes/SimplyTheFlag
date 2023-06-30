package com.vgaltes.simplytheflag

class BooleanFlag(override val cacheMillis: Long, private val rawParameters: String) : Flag {
    private var enabled = false
    override val type: String
        get() = BooleanFlag::class.java.typeName

    override fun evaluate(value: Any): Boolean {
        return enabled
    }

    init {
        val jsonNode = JacksonModule.OBJECT_MAPPER.readTree(rawParameters)
        enabled = jsonNode["enabled"].asBoolean()
    }
}