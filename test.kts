class Config { var email: String = "" }
fun config(b: Config.() -> Unit) { val c = Config(); c.b(); println("RESULT=" + c.email) }
fun test(email: String) {
    config {
        this.email = email
    }
}
test("outer")
