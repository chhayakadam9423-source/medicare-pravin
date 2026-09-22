package test
import org.junit.Test

class Config { var email: String = "empty" }
fun config(b: Config.() -> Unit) { val c = Config(); c.b(); println("RESULT=" + c.email) }

class ShadowTest {
    @Test
    fun testShadow() {
        val email = "outer"
        config {
            this.email = email
        }
    }
}