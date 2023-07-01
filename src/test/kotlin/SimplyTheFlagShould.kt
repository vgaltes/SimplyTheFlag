import TestContainers.ssmLocalStack
import com.vgaltes.simplytheflag.SSMValueRetriever
import com.vgaltes.simplytheflag.SimplyTheFlag
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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

object TestContainers {
    val ssmLocalStack = initSSMLocalStack()
}

class SimplyTheFlagShould: StringSpec( {
    "should read a boolean flag" {
        val name = "name-${UUID.randomUUID()}"
        val value = booleanFlagWithCache(2000, true)
        val client = buildClient(ssmLocalStack)
        val flags = SimplyTheFlag(SSMValueRetriever(client))

        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).build()).join()

        flags.isEnabled(name) shouldBe true
    }

    "should read a fromDate flag" {
        val name = "name-${UUID.randomUUID()}"
        val value = dateFromFlag(Instant.now().minusSeconds(60))
        val client = buildClient(ssmLocalStack)
        val flags = SimplyTheFlag(SSMValueRetriever(client))

        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).build()).join()

        flags.isEnabled(name) shouldBe true
    }

    "should take cache into account" {
        val name = "name-${UUID.randomUUID()}"
        var value = booleanFlagWithCache(0, true)
        val client = buildClient(ssmLocalStack)
        val flags = SimplyTheFlag(SSMValueRetriever(client))

        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).build()).join()
        flags.isEnabled(name) shouldBe true

        value = booleanFlagWithCache(2000, false)
        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).overwrite(true).build()).join()
        flags.isEnabled(name) shouldBe false

        value = booleanFlagWithCache(2000, true)
        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).overwrite(true).build()).join()
        flags.isEnabled(name) shouldBe false
    }

    "should return false and error if the configuration of the flag is invalid" {
        val name = "name-${UUID.randomUUID()}"
        val invalidValue = invalidBooleanFlag()
        val client = buildClient(ssmLocalStack)
        val flags = SimplyTheFlag(SSMValueRetriever(client))

        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(invalidValue).build()).join()

        flags.isEnabled(name) shouldBe false
        flags.hasFailed(name) shouldBe true
        flags.lastError(name) shouldNotBe null

        val validValue = booleanFlagWithCache(2000, true)
        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(validValue).overwrite(true).build()).join()

        flags.isEnabled(name) shouldBe true
        flags.hasFailed(name) shouldBe false
        flags.lastError(name) shouldBe null
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

private fun invalidBooleanFlag(): String = """
        {
            "type": "BooleanFlag",
            "cacheMillis": 2000,
            "parameters": {
                "enabled_wrongly_written": true
            }
        }
    """.trimIndent()

fun initSSMLocalStack(): LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1.0"))
    .withServices(LocalStackContainer.Service.SSM)
    .withReuse(true)
    .apply {
        start()
    }

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