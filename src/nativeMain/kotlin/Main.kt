fun main() {
    val vm = VM(null)

    vm.interpret(""" 
class Example {
  let field = 1 
  fn(a: int) {
    : a
  }

  fn say(a: int) {
    : a
  }
}

fn main() {
  let example = Example(1)

  example.say(342)
  example.say(4523)
}
    """.trimIndent())
}