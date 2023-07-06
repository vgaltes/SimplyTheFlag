package com.vgaltes.simplytheflag

import java.time.Duration
import java.time.Instant


class SimplyTheFlag(private val valueRetriever: ValueRetriever, vararg additionalPackageNames: String) {
    private val flagsRetrieved = mutableMapOf<String, CachedValue<Boolean>>()
    private val availableFlags = mutableMapOf<String, String>()
    private val lastErrors = mutableMapOf<String, Exception>()

    fun state(flagName: String, vararg parameters: Any?): FlagState {
        try {
            lastErrors.remove(flagName)
            val lastRetrievedFlagValue = flagsRetrieved[flagName]

            val flagRetrievedAt = lastRetrievedFlagValue?.retrievedAt ?: Instant.MIN
            val cacheDuration = lastRetrievedFlagValue?.cacheDuration ?: Duration.ZERO

            val cachedValue = if(flagRetrievedAt + cacheDuration < Instant.now()) {
                val value = retrieveFlagValueFromProvider(flagName, parameters)
                flagsRetrieved[flagName] = value
                value
            } else {
                flagsRetrieved[flagName]!!
            }

            return FlagState.from(cachedValue.value)
        }
        catch (e: Exception) {
            lastErrors[flagName] = e
            return FlagState.ERROR
        }
    }

    fun lastError(flagName: String): Exception? = lastErrors[flagName]

    init {
        getClassesForPackage("com.vgaltes.simplytheflag", availableFlags)
        additionalPackageNames.forEach {
            getClassesForPackage(it, availableFlags)
        }
    }

    private fun retrieveFlagValueFromProvider(flagName: String, parameters: Array<out Any?>): CachedValue<Boolean> {
        val rawFlag = valueRetriever.retrieve(flagName)
        val flag = createFlag(rawFlag)
        val value = flag.isEnabled(parameters)
        return CachedValue(Instant.now(), Duration.ofMillis(flag.cacheMillis), value)
    }

    private fun createFlag(rawFlag: String): Flag {
        val flagDefinition = JacksonModule.OBJECT_MAPPER.readTree(rawFlag)
        val type = flagDefinition["type"].asText()
        val cacheMillis = flagDefinition["cacheMillis"].asLong()
        val rawParameters = flagDefinition["parameters"].toString()
        val flag = Class.forName(availableFlags[type]).constructors.find { it.parameterCount == 2 }
            ?.newInstance(cacheMillis, rawParameters)
        return flag as Flag
    }

    class CachedValue<T>(val retrievedAt: Instant, val cacheDuration: Duration, val value: T)

    enum class FlagState {
        ENABLED,
        DISABLED,
        ERROR;

        companion object {
            fun from(value: Boolean) : FlagState = if (value) ENABLED else DISABLED
        }
    }
}

interface Flag {
    val type: String
    val cacheMillis: Long
    fun isEnabled(parameters: Array<out Any?>): Boolean
}

interface ValueRetriever {
    fun retrieve(flagName: String): String
}
