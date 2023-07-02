import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.testcontainers.containers.localstack.LocalStackContainer
import ratpack.http.HttpMethod
import ratpack.http.Status
import ratpack.impose.ForceDevelopmentImposition
import ratpack.impose.ImpositionsSpec
import ratpack.registry.Registry
import ratpack.test.MainClassApplicationUnderTest
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ssm.SsmAsyncClient
import software.amazon.awssdk.services.ssm.model.ParameterType
import software.amazon.awssdk.services.ssm.model.PutParameterRequest
import java.util.*

class SimplyTheFlagRatpackShould : StringSpec({
    "return true if the header exists" {
        val app = TestApplication()
        val headerName = "name-${UUID.randomUUID()}"
        val value = """
            {
                "type": "HeaderExistsFlag",
                "cacheMillis": 0,
                "parameters": {
                    "headerName": "$headerName"
                }
            }
        """.trimMargin()
        val client = buildClient(TestApp.localStackContainer)
        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name("headerExistsFlag").value(value).overwrite(true).build()).join()
        val response = app.httpClient.request("headerExists") { requestSpec ->
            requestSpec.headers.add(headerName, "exists")
            requestSpec.method(HttpMethod.GET)
        }

        response.status shouldBe Status.OK
        response.body.text shouldBe "headerExists"

        val response2 = app.httpClient.request("headerExists") { requestSpec ->
            requestSpec.method(HttpMethod.GET)
        }

        response2.status shouldBe Status.OK
        response2.body.text shouldBe "headerNotExists"
    }

    "return true if the header has the expected value" {
        val app = TestApplication()
        val headerName = "name-${UUID.randomUUID()}"
        val expectedValue = "value-${UUID.randomUUID()}"
        val value = """
            {
                "type": "HeaderHasValueFlag",
                "cacheMillis": 0,
                "parameters": {
                    "headerName": "$headerName",
                    "expectedValue": "$expectedValue"
                }
            }
        """.trimMargin()
        val client = buildClient(TestApp.localStackContainer)
        client.putParameter(PutParameterRequest.builder().type(ParameterType.STRING).name("headerHasValueFlag").value(value).overwrite(true).build()).join()
        val response = app.httpClient.request("headerHasValue") { requestSpec ->
            requestSpec.headers.add(headerName, expectedValue)
            requestSpec.method(HttpMethod.GET)
        }

        response.status shouldBe Status.OK
        response.body.text shouldBe "headerHasValue"

        val response2 = app.httpClient.request("headerHasValue") { requestSpec ->
            requestSpec.method(HttpMethod.GET)
        }

        response2.status shouldBe Status.OK
        response2.body.text shouldBe "headerNotHasValue"
    }
})

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

class TestApplication : MainClassApplicationUnderTest(TestApp.javaClass) {
    lateinit var registry: Registry

    init {
        address
    }

    override fun addImpositions(impositions: ImpositionsSpec) {
        impositions.add(ForceDevelopmentImposition.of(false))
    }
}