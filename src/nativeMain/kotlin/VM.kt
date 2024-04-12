enum class InterpretResult {
    COMPILE_ERROR,
    RUNTIME_ERROR,
    OK
}

data class CallFrame(val function: FunctionObject, var ip: Int = 0, val stack: ArrayDeque<Value<*>> = ArrayDeque(), val registry: MutableMap<Int, Value<*>> = mutableMapOf())

class VM(private var chunk: Chunk?) {
    private val frames = ArrayDeque<CallFrame>(64)

    private val currentFrame: CallFrame
        get() = frames.last()

    private fun runtimeError(message: String): Throwable {
        val line = currentFrame.function.code.getLine(currentFrame.ip)

        return IllegalArgumentException("[line $line] in script: $message")
    }

    private fun readByte(): UByte = currentFrame.function.code.code[currentFrame.ip++]
    private fun readShort() = (readByte().toInt() shl 8) or readByte().toInt()
    private fun readConstant(): Value<*> = currentFrame.function.code.constants[readByte().toInt()]

    private fun run(): InterpretResult {
        while (true) {
            when (val inst = readByte().toInt()) {
                Opcode.ADD.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.stack.removeLast()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(a + b)
                }

                Opcode.MULTIPLY.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.stack.removeLast()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(a * b)
                }

                Opcode.DIVIDE.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.stack.removeLast()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(a / b)
                }

                Opcode.NEGATE.ordinal -> {
                    if (currentFrame.stack.size < 1)
                        throw runtimeError("Stack underflow")

                    val a = currentFrame.stack.last()
                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")

                    currentFrame.stack[currentFrame.stack.lastIndex] = -a
                }

                Opcode.ADD_IP.ordinal -> {
                    if (currentFrame.stack.size < 1)
                        throw runtimeError("Stack underflow")

                    val addr = readByte().toInt()

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.registry[addr]!!

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.registry[addr] = a + b
                }

                Opcode.MULTIPLY_IP.ordinal -> {
                    if (currentFrame.stack.size < 1)
                        throw runtimeError("Stack underflow")

                    val addr = readByte().toInt()

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.registry[addr]!!

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.registry[addr] = a * b
                }

                Opcode.DIVIDE_IP.ordinal -> {
                    if (currentFrame.stack.size < 1)
                        throw runtimeError("Stack underflow")

                    val addr = readByte().toInt()

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.registry[addr]!!

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.registry[addr] = a / b
                }

                Opcode.NEGATE_IP.ordinal -> {
                    val addr = readByte().toInt()

                    val a = currentFrame.registry[addr]!!

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")

                    currentFrame.registry[addr] = -a
                }

                Opcode.ADD_FT.ordinal -> {
                    val addrA = readByte().toInt()
                    val addrB = readByte().toInt()
                    val addrC = readByte().toInt()

                    val b = currentFrame.registry[addrB]!!
                    val a = currentFrame.registry[addrA]!!

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.registry[addrC] = a + b
                }

                Opcode.MULTIPLY_FT.ordinal -> {
                    val addrA = readByte().toInt()
                    val addrB = readByte().toInt()
                    val addrC = readByte().toInt()

                    val b = currentFrame.registry[addrB]!!
                    val a = currentFrame.registry[addrA]!!
                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.registry[addrC] = a * b
                }

                Opcode.DIVIDE_FT.ordinal -> {
                    val addrA = readByte().toInt()
                    val addrB = readByte().toInt()
                    val addrC = readByte().toInt()

                    val b = currentFrame.registry[addrB]!!
                    val a = currentFrame.registry[addrA]!!

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.registry[addrC] = a / b
                }

                Opcode.IS_GREATER.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.stack.removeLast()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(BooleanValue(a > b))
                }

                Opcode.IS_GREATER_EQUAL.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.stack.removeLast()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(BooleanValue(a >= b))
                }

                Opcode.IS_LESS.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.stack.removeLast()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(BooleanValue(a < b))
                }

                Opcode.IS_LESS_EQUAL.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.removeLast()
                    val a = currentFrame.stack.removeLast()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(BooleanValue(a <= b))
                }

                Opcode.LOAD.ordinal -> {
                    currentFrame.registry[readByte().toInt()]?.let { currentFrame.stack.addLast(it) }
                }

                Opcode.LOAD_IP.ordinal -> {
                    val addrA = readByte().toInt()
                    val addrB = readByte().toInt()

                    currentFrame.registry[addrA]?.let { currentFrame.registry[addrB] = it }
                }

                Opcode.STORE.ordinal -> {
                    if (currentFrame.stack.size < 1)
                        throw runtimeError("Stack underflow")

                    currentFrame.registry[readByte().toInt()] = currentFrame.stack.removeLast()
                }

                Opcode.LDC.ordinal -> {
                    currentFrame.stack.addLast(readConstant())
                }

                Opcode.LDC_IP.ordinal -> {
                    currentFrame.registry[readByte().toInt()] = readConstant()
                }

                Opcode.POP.ordinal -> {
                    currentFrame.stack.removeLast()
                }

                Opcode.PRINT_POP.ordinal -> {
                    println(currentFrame.stack.removeLast())
                }

                Opcode.DUP.ordinal -> {
                    currentFrame.stack.addLast(currentFrame.stack.last())
                }

                Opcode.IFEQ.ordinal -> {
                    val offset = readShort()

                    val value = currentFrame.stack.last().value
                    if (value !is Boolean)
                        throw runtimeError("Expected boolean value")

                    if (value)
                        currentFrame.ip += offset
                }

                Opcode.IFNE.ordinal -> {
                    val offset = readShort()

                    val value = currentFrame.stack.last().value
                    if (value !is Boolean)
                        throw runtimeError("Expected boolean value")

                    if (!value)
                        currentFrame.ip += offset
                }

                Opcode.JUMP.ordinal -> {
                    val offset = readShort()

                    currentFrame.ip += offset
                }

                Opcode.LOOP.ordinal -> {
                    val offset = readShort()

                    currentFrame.ip -= offset
                }

                Opcode.CALL.ordinal -> {
                    val argCount = readByte().toInt()

                    val callee = currentFrame.stack[currentFrame.stack.size - argCount - 1]

                    if (callee !is ObjectValue<*>)
                        throw runtimeError("Callee must be an object")
                    if (callee.value !is FunctionObject)
                        throw runtimeError("Callee must be a function")

                    val values = mutableMapOf<Int, Value<*>>()
                    (0..<argCount).forEach {
                        val value = currentFrame.stack.removeLast()
                        values[it] = value

                        val info = callee.value.params[it]
                        val expectedType = info.second
                        val butGot = ExpressionType[value.value]

                        if (expectedType != butGot)
                            throw runtimeError("Expected $expectedType as argument ${info.first.content} but got $butGot")
                    }

                    currentFrame.stack.removeLast()

                    frames.addLast(CallFrame(
                        callee.value,
                        registry = values
                    ))
                }

                Opcode.RETURN.ordinal -> {
                    val returnValue = currentFrame.stack.removeLast()

                    frames.removeLast()
                    if (frames.isEmpty())
                        return InterpretResult.OK

                    currentFrame.stack.addLast(returnValue)
                }

                else -> throw IllegalStateException("Unexpected opcode $inst")
            }
        }
    }

    fun interpret(source: String): InterpretResult {
        val parser = Parser(source)
        var ast = try {
            parser.parse()
        } catch (e: IllegalStateException) {
            e.printStackTrace()

            return InterpretResult.COMPILE_ERROR
        }

        val optimizer = LiteralOptimizer()
        ast.forEach {
            it.accept(optimizer)
        }

        val deadCodeEliminator = DeadCodeEliminator()
        ast = ast.mapNotNull {
            it.accept(deadCodeEliminator)
        }

        println(ast)

        val compiler = Compiler()
        ast.forEach {
            it.accept(compiler)
        }

        val function = compiler.functions.find { it.name == "main" }
        if (function != null) {
            frames.addLast(CallFrame(
                function
            ))

            this.chunk = function.code

            return try {
                run()
            } catch (e: IllegalStateException) {
                e.printStackTrace()

                InterpretResult.RUNTIME_ERROR
            }
        }

        return InterpretResult.OK
    }
}


