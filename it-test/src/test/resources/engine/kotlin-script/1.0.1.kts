import com.noumenadigital.npl.migration.util.mapPrototypesInUpshift
import com.noumenadigital.platform.migration.dsl.migration
import java.math.BigDecimal
import java.time.ZoneId
import java.time.ZonedDateTime

val protos = mapPrototypesInUpshift()

val iou = protos.match("Iou")

migration("${protos.current} to ${protos.target}")
    .transformProtocol(iou.current, iou.target) {
        put("paymentDeadline") {
            createDateTime(ZonedDateTime.of(2020, 1, 1, 12, 0, 0, 0, ZoneId.systemDefault()))
        }
        put("lateFee") {
            createNumber(BigDecimal.TEN)
        }
    }
    .retag(protos)
