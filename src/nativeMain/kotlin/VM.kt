enum class InterpretResult {
    COMPILE_ERROR,
    RUNTIME_ERROR,
    OK
}

data class CallFrame(val function: FunctionObject, var ip: Int = 0, val stack: ArrayDeque<Value<*>> = ArrayDeque(), val registry: MutableMap<Int, Value<*>> = mutableMapOf())

class VM(private var chunk: Chunk?) {
    private val frames = ArrayDeque<CallFrame>(64)

    private val globals = mutableMapOf<Int, Value<*>>()

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
            when (val lastInstruction = readByte().toInt()) {
                Opcode.ADD.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.pop()
                    val a = currentFrame.stack.pop()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(a + b)
                }

                Opcode.MULTIPLY.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.pop()
                    val a = currentFrame.stack.pop()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(a * b)
                }

                Opcode.DIVIDE.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.pop()
                    val a = currentFrame.stack.pop()

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

                    val b = currentFrame.stack.pop()
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

                    val b = currentFrame.stack.pop()
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

                    val b = currentFrame.stack.pop()
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

                    val b = currentFrame.stack.pop()
                    val a = currentFrame.stack.pop()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(BooleanValue(a > b))
                }

                Opcode.IS_GREATER_EQUAL.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.pop()
                    val a = currentFrame.stack.pop()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(BooleanValue(a >= b))
                }

                Opcode.IS_LESS.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.pop()
                    val a = currentFrame.stack.pop()

                    if (a !is NumberValue)
                        throw runtimeError("Expected number but got \"${a::class.simpleName}\"")
                    if (b !is NumberValue)
                        throw runtimeError("Expected number but got \"${b::class.simpleName}\"")

                    currentFrame.stack.addLast(BooleanValue(a < b))
                }

                Opcode.IS_LESS_EQUAL.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val b = currentFrame.stack.pop()
                    val a = currentFrame.stack.pop()

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

                Opcode.LOAD_GLOBAL.ordinal -> {
                    globals[readByte().toInt()]?.let { currentFrame.stack.addLast(it) }
                }

                Opcode.LOAD_GLOBAL_IP.ordinal -> {
                    val addrA = readByte().toInt()
                    val addrB = readByte().toInt()

                    globals[addrA]?.let { currentFrame.registry[addrB] = it }
                }

                Opcode.STORE.ordinal -> {
                    if (currentFrame.stack.size < 1)
                        throw runtimeError("Stack underflow")

                    currentFrame.registry[readByte().toInt()] = currentFrame.stack.pop()
                }

                Opcode.STORE_GLOBAL.ordinal -> {
                    if (currentFrame.stack.size < 1)
                        throw runtimeError("Stack underflow")

                    globals[readByte().toInt()] = currentFrame.stack.pop()
                }

                Opcode.LDC.ordinal -> {
                    currentFrame.stack.addLast(readConstant())
                }

                Opcode.LDC_IP.ordinal -> {
                    currentFrame.registry[readByte().toInt()] = readConstant()
                }

                Opcode.POP.ordinal -> {
                    currentFrame.stack.pop()
                }

                Opcode.POP_IF_PRESENT.ordinal -> {
                    if (currentFrame.stack.isNotEmpty())
                        currentFrame.stack.pop()
                }

                Opcode.PRINT_POP.ordinal -> {
                    println(currentFrame.stack.pop())
                }

                Opcode.DUP.ordinal -> {
                    currentFrame.stack.dup()
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

                Opcode.GET.ordinal -> {
                    if (currentFrame.stack.size < 1)
                        throw runtimeError("Stack underflow")

                    val name = readConstant().value.toString()

                    val instance = currentFrame.stack.last()
                    if (instance !is ObjectValue)
                        throw runtimeError("Parent should be an object")
                    if (instance.value !is InstanceObject)
                        throw runtimeError("Parent should be an instance")

                    (instance.value as InstanceObject).fields[name]?.let {
                        currentFrame.stack.pop()
                        currentFrame.stack.addLast(it)
                    } ?: throw runtimeError("Undefined field $name")
                }

                Opcode.GET_IP.ordinal -> {
                    if (currentFrame.stack.size < 1)
                        throw runtimeError("Stack underflow")

                    val name = readConstant().value.toString()
                    val addr = readByte().toInt()

                    val instance = currentFrame.stack.pop()
                    if (instance !is ObjectValue)
                        throw runtimeError("Parent should be an object")
                    if (instance.value !is InstanceObject)
                        throw runtimeError("Parent should be an instance")

                    (instance.value as InstanceObject).fields[name]?.let {
                        currentFrame.registry[addr] = it
                    } ?: throw runtimeError("Undefined field $name")
                }

                Opcode.SET.ordinal -> {
                    if (currentFrame.stack.size < 2)
                        throw runtimeError("Stack underflow")

                    val name = readConstant().value.toString()

                    val instance = currentFrame.stack.pop()
                    if (instance !is ObjectValue)
                        throw runtimeError("Parent should be an object")
                    if (instance.value !is InstanceObject)
                        throw runtimeError("Parent should be an instance")

                    (instance.value as InstanceObject).fields[name] = currentFrame.stack.pop()
                }

                Opcode.CALL.ordinal -> {
                    val argCount = readByte().toInt()
                    if (currentFrame.stack.size < argCount + 1)
                        throw runtimeError("Stack underflow")

                    val callee = currentFrame.stack[currentFrame.stack.size - argCount - 1]

                    callValue(callee, argCount)
                }

                Opcode.CALL_METHOD.ordinal -> {
                    val name = readConstant().value.toString()

                    val argCount = readByte().toInt()
                    if (currentFrame.stack.size < argCount + 1)
                        throw runtimeError("Stack underflow")

                    val parent = currentFrame.stack[currentFrame.stack.size - argCount - 1]
                    if (parent !is ObjectValue)
                        throw runtimeError("Parent should be an object")
                    if (parent.value !is InstanceObject)
                        throw runtimeError("Parent should be an instance")

                    val callee = (parent.value as InstanceObject).fields[name] ?: throw runtimeError("Method $name does not exist")

                    callValue(callee, argCount)
                }

                Opcode.RETURN.ordinal -> {
                    val isVoid = currentFrame.function.returnType == null

                    val returnValue = if (!isVoid)
                        currentFrame.stack.pop()
                    else null

                    frames.removeLast()
                    if (frames.isEmpty())
                        return InterpretResult.OK

                    returnValue?.let { currentFrame.stack.addLast(it) }
                }

                else -> throw IllegalStateException("Unexpected opcode $lastInstruction")
            }
        }
    }

    private fun callValue(callee: Value<*>, argCount: Int) {
        if (callee !is ObjectValue<*>)
            throw runtimeError("Callee must be an object")

        when (callee.value) {
            is FunctionObject -> {
                val isMethod = callee.value.isMethod

                val values = mutableMapOf<Int, Value<*>>()
                if (isMethod)
                    values[0] = currentFrame.stack[currentFrame.stack.size - argCount - 1]

                (0..<argCount).forEach {
                    val value = currentFrame.stack.pop()
                    values[if (isMethod) it + 1 else it] = value

                    val info = callee.value.params[it]
                    val expectedType = info.second
                    val butGot = ExpressionType[value.value]

                    if (expectedType != butGot)
                        throw runtimeError("Expected $expectedType as argument ${info.first.content} but got $butGot")
                }

                currentFrame.stack.pop()

                frames.addLast(CallFrame(
                    callee.value,
                    registry = values
                ))
            }

            is ClassObject -> {
                val instance = InstanceObject(
                    callee.value
                )

                val init = instance.fields["<init>"] ?: throw runtimeError("Init method doesn't exist")
                if (init.value !is FunctionObject)
                    throw runtimeError("Init method is not a function object")

                val values = mutableListOf<Value<*>>()
                (0..<argCount).forEach {
                    val value = currentFrame.stack.pop()
                    values.add(value)

                    val info = (init.value as FunctionObject).params[it]
                    val expectedType = info.second
                    val butGot = ExpressionType[value.value]

                    if (expectedType != butGot)
                        throw runtimeError("Expected $expectedType as argument ${info.first.content} but got $butGot")
                }

                currentFrame.stack.addLast(ObjectValue(instance))
                currentFrame.stack.dup()

                currentFrame.stack.addAll(values)

                callValue(init, argCount)
            }

            else -> throw runtimeError("Callee must be callable")
        }
    }

    private fun <E> ArrayDeque<E>.dup() {
        try {
            addLast(last())
        } catch (e: NoSuchElementException) {
            throw runtimeError("Stack underflow")
        }
    }

    private fun <E> ArrayDeque<E>.pop(): E {
        try {
            return removeLast()
        } catch (e: NoSuchElementException) {
            throw runtimeError("Stack underflow")
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
        val deadCodeEliminator = DeadCodeEliminator()
        ast = ast.mapNotNull {
            it.accept(optimizer)
            it.accept(optimizer)
            it.accept(deadCodeEliminator)
        }

        val compiler = Compiler()
        ast.forEach {
            it.accept(compiler)
        }

        compiler.script.code.writeConstant(
            ObjectValue(
                NullObject
            ),
            -1
        )

        compiler.script.code.write(
            Opcode.RETURN,
            -1
        )

        frames.addLast(CallFrame(
            compiler.script
        ))

        this.chunk = compiler.script.code

        try {
            run()
        } catch (e: Exception) {
            e.printStackTrace()

            return InterpretResult.RUNTIME_ERROR
        }

        val mainFunction = compiler.functions.first { it.isMainFunction }

        frames.addLast(CallFrame(
            mainFunction
        ))

        this.chunk = mainFunction.code
        this.chunk?.disassemble()

        return try {
            run()
        } catch (e: Exception) {
            e.printStackTrace()

            return InterpretResult.RUNTIME_ERROR
        }
    }
}


