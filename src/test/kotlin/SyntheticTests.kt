import com.hadihariri.markcode.Synthetics
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class SyntheticTests: Spek({

    given("a list of imports defined in imports.txt") {
        on("accessing imports") {
            val values = Synthetics.imports
            it("should contain a map of key/value pairs") {
                assertNotEquals(0, values.count())
                assertEquals("java.util.Comparator", values["Comparator"])
            }
        }
    }

    given("a list of prefixes defined in prefixes.txt") {
        on("accessing prefixes") {
            val values = Synthetics.prefixes
            it("should contain a map of key/value pairs") {
                assertNotEquals(0, values.count())
                assertEquals("fun getFacebookName(accountId: Int) = \"fb:\$accountId\"", values["getFacebookName"])
            }
        }
    }




})