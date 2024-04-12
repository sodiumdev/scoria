fun main() {
    val vm = VM(null)

    vm.interpret(""" 
fn main() {
    let x = 4;
    x = x + 1;
    
    let y = 3;

    if 25 > 24 {
      : "true...";
    }
  
    if 43 <= 42 {
      y = y + 1; 
      : "this should not work!";
    }
    
    : x;

    : y;
    : "hi"; // hi
}
    """.trimIndent())
}