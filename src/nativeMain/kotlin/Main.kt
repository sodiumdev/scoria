fun main() {
    val vm = VM(null)

    vm.interpret("""   
class Example {
  let it = "hi"

  fn getIt(): any {
    return this.it
  }
}

fn getIt(): any {
  return "it"
}

fn main() {
    // loops
    let a = 0
    while (a < 25) {
      a += 1
    }

    for let b = 0; b < 25; b += 1 {
      : b
    }

    // if statements
    if 25 < 26 {
      : "yep!"
    }

    // printing
    : a + 34

    let it = getIt() // function calls
    : it

    let example = Example() // classes and instances
    : example.getIt()
}
    """.trimIndent())
}