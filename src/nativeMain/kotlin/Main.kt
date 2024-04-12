fun main() {
    val vm = VM(null)

    println(vm.interpret(""" 
fn main() {
    : "hi";
}
    """.trimIndent()))
}