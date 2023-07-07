package com.vgaltes.simplytheflag

import java.time.Duration
import java.time.Instant


class SimplyTheFlag(private val valueRetriever: ValueRetriever) {
    private val flagsRetrieved = mutableMapOf<String, CachedValue<Boolean>>()
    private val configValuesRetrieved = mutableMapOf<String, CachedValue<String>>()
    private val availableFlags = mutableMapOf<String, String>()
    private val availableConfigs = mutableMapOf<String, String>()

    fun state(flagName: String, vararg parameters: Any?): Result<Boolean> {
        try {
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

            return Result.success(cachedValue.value)
        }
        catch (e: Exception) {
            return Result.failure(e)
        }
    }

    fun value(configName: String, vararg parameters: Any?): Result<String> {
        try {
            val lastRetrievedFlagValue = configValuesRetrieved[configName]

            val flagRetrievedAt = lastRetrievedFlagValue?.retrievedAt ?: Instant.MIN
            val cacheDuration = lastRetrievedFlagValue?.cacheDuration ?: Duration.ZERO

            val cachedValue = if(flagRetrievedAt + cacheDuration < Instant.now()) {
                val value = retrieveConfigValueFromProvider(configName, parameters)
                configValuesRetrieved[configName] = value
                value
            } else {
                configValuesRetrieved[configName]!!
            }

            return Result.success(cachedValue.value)
        }
        catch (e: Exception) {
            return Result.failure(e)
        }
    }

    init {
        getClassesForPackage("com.vgaltes.simplytheflag", availableFlags, availableConfigs)
    }

    private fun retrieveFlagValueFromProvider(flagName: String, parameters: Array<out Any?>): CachedValue<Boolean> {
        val rawFlag = valueRetriever.retrieve(flagName)
        val flag = createFlag(rawFlag)
        val value = flag.isEnabled(parameters)
        return CachedValue(Instant.now(), Duration.ofMillis(flag.cacheMillis), value)
    }

    private fun retrieveConfigValueFromProvider(configValueName: String, parameters: Array<out Any?>): CachedValue<String> {
        val rawFlag = valueRetriever.retrieve(configValueName)
        val configValue = createConfigValue(rawFlag)
        val value = configValue.value(parameters)
        return CachedValue(Instant.now(), Duration.ofMillis(configValue.cacheMillis), value)
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

    private fun createConfigValue(rawFlag: String): StringConfig {
        val flagDefinition = JacksonModule.OBJECT_MAPPER.readTree(rawFlag)
        val type = flagDefinition["type"].asText()
        val cacheMillis = flagDefinition["cacheMillis"].asLong()
        val rawParameters = flagDefinition["parameters"].toString()
        val configValue = Class.forName(availableConfigs[type]).constructors.find { it.parameterCount == 2 }
            ?.newInstance(cacheMillis, rawParameters)
        return configValue as StringConfig
    }

    class CachedValue<T>(val retrievedAt: Instant, val cacheDuration: Duration, val value: T)

}

interface Flag {
    val type: String
    val cacheMillis: Long
    fun isEnabled(parameters: Array<out Any?>): Boolean
}

interface ConfigValue {
    val type: String
    val cacheMillis: Long
    fun value(parameters: Array<out Any?>): String
}

interface ValueRetriever {
    fun retrieve(flagName: String): String
}
