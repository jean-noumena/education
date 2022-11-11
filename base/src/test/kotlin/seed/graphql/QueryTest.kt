package seed.graphql

import org.junit.jupiter.api.Test
import seed.config.JSON
import kotlin.test.assertEquals

class QueryTest {

    @Test
    fun `de-serialising graphql response to get protocolId`() {
        // given
        val inputJson = """
            {
              "data": {
                "protocolStates": {
                  "totalCount": 1,
                  "nodes": [
                    {
                      "protocolId": "00e6b238-e19a-4c67-ad66-77ea2620be53"
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        // when
        val graphQlResponse = JSON.mapper.readValue(inputJson, GraphQlResponse::class.java)
        val expected = graphQlResponse.data.protocolStates.nodes.first().protocolId

        // then
        assertEquals("00e6b238-e19a-4c67-ad66-77ea2620be53", expected)
    }
}
