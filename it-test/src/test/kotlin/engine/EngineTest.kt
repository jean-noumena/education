package engine

import com.noumenadigital.npl.lang.DateTimeValue
import com.noumenadigital.npl.lang.NumberValue
import com.noumenadigital.npl.lang.PartyValue
import com.noumenadigital.npl.lang.ProtocolState
import com.noumenadigital.platform.engine.testing.Configuration
import com.noumenadigital.platform.engine.testing.DBConfig
import com.noumenadigital.platform.engine.testing.EngineMigrationTester
import io.kotest.core.spec.style.FunSpec
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EngineTest : FunSpec({
    val issuer = PartyValue("issuer")
    val payee = PartyValue("payee")

    fun getDbConfig(testName: String, dbSchema: String = DBDefaults.getRandomSchema(testName)) =
        DBConfig(DBDefaults.DB_URL, dbSchema, DBDefaults.DB_USER, DBDefaults.DB_PASS)

    fun ProtocolState.getFrameValue(slotName: String) = value.frame.slots[slotName]

    // Disabled. To re-enable, rename the `xtest` function to `test`
    xtest("npl 1.0.0 to 1.0.1") {
        val migrationPath = "engine/"
        val migrationHome =
            File(PathMatchingResourcePatternResolver().getResources(migrationPath).singleOrNull()?.url!!.path)
        val engine = EngineMigrationTester(migrationHome, Configuration(getDbConfig("seed")))
//        val engine = EngineMigrationTester(migrationHome, Configuration(dbConfig = null)) // in-memory test

        engine.runTo("1.0.0")

        // create an iou here
        val iouId = engine.createProtocol("/seed-1.0.0?/seed/Iou", listOf(NumberValue(10)), listOf(issuer, payee))

        engine.apply {
            val iou = getProtocolStateById(iouId)!!

            val forAmount = iou.getFrameValue("forAmount")
            val paymentDeadline = iou.getFrameValue("paymentDeadline")
            val lateFee = iou.getFrameValue("lateFee")

            assertNotNull(forAmount)
            assertEquals(10, (forAmount as NumberValue).value.intValueExact())
            assertNull(paymentDeadline)
            assertNull(lateFee)
        }

        engine.runTo("1.0.1")

        engine.apply {
            val iou = getProtocolStateById(iouId)!!

            val forAmount = iou.getFrameValue("forAmount")
            val paymentDeadline = iou.getFrameValue("paymentDeadline")
            val lateFee = iou.getFrameValue("lateFee")

            assertNotNull(forAmount)
            assertEquals(10, (forAmount as NumberValue).value.intValueExact())

            assertNotNull(paymentDeadline)
            assertEquals(
                ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault()),
                (paymentDeadline as DateTimeValue).value,
            )

            assertNotNull(lateFee)
            assertEquals(10, (lateFee as NumberValue).value.intValueExact())
        }
    }
})

internal object DBDefaults {
    const val DB_USER = "seed"
    const val DB_PASS = "secret"
    const val DB_URL = "jdbc:postgresql://localhost:5432/engine"

    fun getRandomSchema(prefix: String) = "$prefix-${UUID.randomUUID()}"
}
