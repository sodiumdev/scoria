fun main() {
    val vm = VM(null)

    vm.interpret(""" 
fn main() {
    let x = 0;
    while x < 25 {
      x = x + 1;
      : x;
    }
    
    let y = 3;

    if 25 > 24 {
      : "true...";
    }
  
    if 43 <= 42 {
      y = y + 1; 
      : "this should not work!";
    }

    : y;
    : "hi"; // hi
}
    """.trimIndent())
}