import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.localstack.LocalStackContainer
import org.testcontainers.utility.DockerImageName
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.GetParameterRequest
import software.amazon.awssdk.services.ssm.model.ParameterType
import software.amazon.awssdk.services.ssm.model.PutParameterRequest

class SimplyTheFlagShould: StringSpec( {
    "pass the test" {
        val ssmLocalStack = initSSMLocalStack()

        val client = buildClient(ssmLocalStack)

        val flags = SimplyTheFlag(client)

        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name("patata").value("potato").build()).join()

        val parameter = client.getParameter(GetParameterRequest.builder().name("patata").build()).join()

        parameter.parameter().value() shouldBe "potato"

        flags.isEnabled("patata") shouldBe "potato"
        true shouldBe true
    }

})

fun initSSMLocalStack(): LocalStackContainer = LocalStackContainer(DockerImageName.parse("localstack/localstack:2.1.0"))
    .withServices(LocalStackContainer.Service.SSM)
    .withReuse(false)
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