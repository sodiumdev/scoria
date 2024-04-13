fun main() {
    val vm = VM(null)

    vm.interpret(""" 
class Example {
  let field = 1 
  fn() { }

  fn sayHi() {
    : "hi"
  }
}

fn main() {
  let example = Example()

  example.sayHi()
}
    """.trimIndent())
}