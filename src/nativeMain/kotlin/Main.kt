fun main() {
    val vm = VM(null)

    vm.interpret("""   
fn main() {
  a()
}
    """.trimIndent())
}