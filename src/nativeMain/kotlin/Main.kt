fun main() {
    val vm = VM(null)

    vm.interpret("""
fn main() {
  let a = 0
  while a < 25 {
    : a
    a += 1
  }
}
    """.trimIndent())
}