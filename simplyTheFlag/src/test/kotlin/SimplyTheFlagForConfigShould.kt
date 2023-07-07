import TestContainers.ssmLocalStack
import com.vgaltes.simplytheflag.SSMValueRetriever
import com.vgaltes.simplytheflag.SimplyTheFlag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.ParameterType
import software.amazon.awssdk.services.ssm.model.PutParameterRequest
import java.time.Instant
import java.util.UUID

class SimplyTheFlagForConfigShould: StringSpec( {
    "should read a config value" {
        val name = "name-${UUID.randomUUID()}"
        val configValue = "value-${UUID.randomUUID()}"
        val value = configValueWithCache(2000, configValue)
        val client = buildClient(ssmLocalStack)
        val flags = SimplyTheFlag(SSMValueRetriever(client))

        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).build()).join()

        val state = flags.value(name)
        state.isSuccess shouldBe true
        state.getOrNull() shouldBe configValue
    }

    "should take cache into account" {
        val name = "name-${UUID.randomUUID()}"
        val configValue = "value-${UUID.randomUUID()}"
        var value = configValueWithCache(0, configValue)
        val client = buildClient(ssmLocalStack)
        val flags = SimplyTheFlag(SSMValueRetriever(client))

        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).build()).join()
        var state = flags.value(name)
        state.isSuccess shouldBe true
        state.getOrNull() shouldBe configValue

        val configValue2 = "value-${UUID.randomUUID()}"
        value = configValueWithCache(2000, configValue2)
        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).overwrite(true).build()).join()
        state = flags.value(name)
        state.isSuccess shouldBe true
        state.getOrNull() shouldBe configValue2

        value = configValueWithCache(2000, configValue)
        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).overwrite(true).build()).join()
        state = flags.value(name)
        state.isSuccess shouldBe true
        state.getOrNull() shouldBe configValue2
    }
})

private fun dateFromFlag(validFrom: Instant): String = """
        {
            "type": "FromDateFlag",
            "cacheMillis": 2000,
            "parameters": {
                "validFrom": "$validFrom"
            }
        }
    """.trimIndent()

private fun booleanFlagWithCache(millis: Long, enabled: Boolean): String = """
        {
            "type": "BooleanFlag",
            "cacheMillis": $millis,
            "parameters": {
                "enabled": $enabled
            }
        }
    """.trimIndent()

private fun configValueWithCache(millis: Long, value: String): String = """
        {
            "type": "StringConfig",
            "cacheMillis": $millis,
            "parameters": {
                "value": "$value"
            }
        }
    """.trimIndent()

private fun invalidBooleanFlag(): String = """
        {
            "type": "BooleanFlag",
            "cacheMillis": 2000,
            "parameters": {
                "enabled_wrongly_written": true
            }
        }
    """.trimIndent()

private fun buildClient(container: LocalStackContainer): SsmAsyncClient = SsmAsyncClient
    .builder()
    .endpointOverride(container.getEndpointOverride((LocalStackContainer.Service.SSM)))
    .region(Region.of(container.region))
    .credentialsProvider(
        StaticCredentialsProvider.create(
            AwsBasicCredentials.create(
                container.accessKey,
                container.secretKey,
            ),
        ),
    )
    .build()