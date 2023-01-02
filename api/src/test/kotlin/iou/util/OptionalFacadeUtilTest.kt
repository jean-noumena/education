package iou.util

import com.noumenadigital.npl.api.generated.lang.core.NoneFacade
import com.noumenadigital.npl.api.generated.lang.core.SomeFacade
import org.junit.jupiter.api.Test

class OptionalFacadeUtilTest {

    @Test
    fun `pass null to toOptionalFacade`() {
        val result = toOptionalFacade(null)

        assert(result.value is NoneFacade)
    }

    @Test
    fun `pass Int to toOptionalFacade`() {
        val result = toOptionalFacade(7)

        assert(result.value is SomeFacade<*>)
    }
}
