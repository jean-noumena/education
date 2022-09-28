package keycloak

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class JwtTest : FunSpec({
    data class Case(val token: String, val userName: String, val party: String)

    val systemToken = """
eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ0eFV1TXZjaFpaZjNERUwxNzh4Wk9Ubnc1aERRLS1zcHVvXzBJS3dkNWhrIn0.eyJleHAiOjE2NTM5MTQxODgsImlhdCI6MTY1MzkxMzg4OCwianRpIjoiZDczNzA4MTgtNDIxOC00NTg2LWI3M2UtODc5YjExNmU2NzRmIiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjExMDAwL3JlYWxtcy9kbXVzaWMiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiNmY3NTM5NWQtOTllZi00ZmQxLWE5NzItNDdmMjk5OTY2ODEzIiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZG11c2ljIiwic2Vzc2lvbl9zdGF0ZSI6IjVmNzA0NThmLTRjMmYtNDU4My1hOTFlLTM3NDE5Mjg2MjkyOSIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1kbXVzaWMiLCJOTV9VU0VSIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsInNpZCI6IjVmNzA0NThmLTRjMmYtNDU4My1hOTFlLTM3NDE5Mjg2MjkyOSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoic3lzdGVtIiwiZ2l2ZW5fbmFtZSI6IiIsImZhbWlseV9uYW1lIjoiIiwicGFydHkiOlsic3lzdGVtIl19.rHJAK5FFsLQC8UqlmVYbUIxgyPU79Q6yCnYcQJ0oztDkKYcwr7ak4jtDc8d5drZGIPvhHg8MuFDq5s6Yp12mopntcTC-O5uRaZvRnRntD3vmHDyK9cZR7uGNv46uqTOzSw20sqcpTL0_0Af86E-qBsrT9rO2uzqMUJO9Aqvho23RjMV0SD7Z0wnF2eVBArH-p2ac3Sn32b77eeelsXqAmG89EagWWm8nnv1MXp3eOYua7_IrKyUm8j5BwzpmOKkggjrj88wF9bNsvqH52UsewqRlId5xMoWGIxTq8SvpdS4UWAZ7EuadtzwpvLOaPdJc0CjjJhtTmyjrvO1Xmf3JGQ
    """.trimIndent()
    val artistToken = """
eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJ0eFV1TXZjaFpaZjNERUwxNzh4Wk9Ubnc1aERRLS1zcHVvXzBJS3dkNWhrIn0.eyJleHAiOjE2NTM5MTM4NzUsImlhdCI6MTY1MzkxMzU3NSwianRpIjoiOGExZjMzNjUtODg2Yy00MWQ1LTkzYmYtYTFjMWI0ZjJjZmU4IiwiaXNzIjoiaHR0cDovL2tleWNsb2FrOjExMDAwL3JlYWxtcy9kbXVzaWMiLCJhdWQiOiJhY2NvdW50Iiwic3ViIjoiZTdlYmE4ZjQtZWIzYy00YjdjLWFiOGEtZTAxMzNlNzJkZGM0IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoiZG11c2ljIiwic2Vzc2lvbl9zdGF0ZSI6IjFjYTUwYWVkLTY1OWItNGE0Ny1hZDAwLTM1ODc2Y2UxNGE5YyIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1kbXVzaWMiLCJOTV9VU0VSIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoicHJvZmlsZSBlbWFpbCIsInNpZCI6IjFjYTUwYWVkLTY1OWItNGE0Ny1hZDAwLTM1ODc2Y2UxNGE5YyIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoieWFjaHQtYXJ0aXN0IiwiZ2l2ZW5fbmFtZSI6IiIsImZhbWlseV9uYW1lIjoiIiwicGFydHkiOlsiYXJ0aXN0Il19.ikIlpceNeXYx-qIJo25sQ6u2KOENY8BJFUu4OvBh6m9jdNR9Z3HX_3PLO8Vn_GlBy0jMR7ApG502jv2BWURVKZXv52g3ngxOiDQYAHrKTSP5yzshnzDwcKq-XN9N0rXSyskAK2MwZ3x3-Oyf2bSo8vjx7ihUYXlp5Jn0DM5PKenxzkHzKlpJPEzvG-n8KRmjJrQn4HkYUZtpQdGYJMDLsAxBtyIahHnNsJQCXUGoxBu8_cz4xB9y5lQahTxQe8qtoHmPNksd196ZTqL4PA-F381W3l9KIvNumBTo4CKKdcf5hwJEQXW1Ps0bn5qf9mGoEGpGE8a5rPPQ-nuXNC_UvQ
    """.trimIndent()
    mapOf(
        "system token" to Case(
            party = "system",
            userName = "system",
            token = systemToken
        ),
        "artist token" to Case(
            party = "artist",
            userName = "yacht-artist",
            token = artistToken
        )
    ).forEach { (name, case) ->
        test(name) {
            val jwt = decodeJwt("Bearer ${case.token}")
            jwt.payload.preferredUsername shouldBe case.userName
            jwt.payload.party[0] shouldBe case.party
        }
    }
})