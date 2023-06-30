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
import java.util.UUID

class SimplyTheFlagShould: StringSpec( {
    "should read a boolean flag" {
        val name = "name-${UUID.randomUUID()}"
        val value = """
            {
                "type": "BooleanFlag",
                "cacheMillis": 2000,
                "parameters": {
                    "enabled": true
                }
            }
        """.trimIndent()
        val ssmLocalStack = initSSMLocalStack()
        val client = buildClient(ssmLocalStack)
        val flags = SimplyTheFlag(client)

        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name(name).value(value).build()).join()

        flags.isEnabled(name) shouldBe true
        true shouldBe true
    }
})

fun initSSMLocalStack(): LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1.0"))
    .withServices(LocalStackContainer.Service.SSM)
    .withReuse(true)
    .apply {
        start()
    }

private fun buildClient(container: LocalStackContainer): SsmAsyncClient {
    val ssmAsyncClient = SsmAsyncClient
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

    return ssmAsyncClient
}