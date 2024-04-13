fun main() {
    val vm = VM(null)

    vm.interpret(""" 
class HiLmao {
    fn() {
        : "init";
    }

    fn print() {
        : "this";
    }
}
        
fn main() {
    HiLmao().print();
}
    """.trimIndent())
}