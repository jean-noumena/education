package seed.config

import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS
import com.fasterxml.jackson.databind.DeserializationFeature.USE_BIG_INTEGER_FOR_INTS
import com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.withStandardMappings
import java.time.format.DateTimeFormatterBuilder

private class ISO8601Formatter :
    InstantSerializer(INSTANCE, false, DateTimeFormatterBuilder().appendInstant(0).toFormatter())

object JSON : ConfigurableJackson(
    KotlinModule()
        .asConfigurable()
        .done()
)

object SnakeCaseJsonConfiguration : ConfigurableJackson(
    KotlinModule()
        .asConfigurable()
        .withStandardMappings()
        .done()
        .deactivateDefaultTyping()
        .configure(FAIL_ON_UNKNOWN_PROPERTIES, true)
        .configure(FAIL_ON_IGNORED_PROPERTIES, true)
        .configure(USE_BIG_DECIMAL_FOR_FLOATS, true)
        .configure(USE_BIG_INTEGER_FOR_INTS, true)
        .setPropertyNamingStrategy(SNAKE_CASE)
)
