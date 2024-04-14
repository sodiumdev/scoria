fun main() {
    val vm = VM(null)

    vm.interpret("""   
fn main() {
  for let a = 0; a < 25; a += 1 {
    : a
  }
}
    """.trimIndent())
}