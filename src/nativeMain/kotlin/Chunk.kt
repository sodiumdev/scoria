enum class Opcode {
    ADD,
    MULTIPLY,
    DIVIDE,
    NEGATE,

    ADD_IP,
    MULTIPLY_IP,
    DIVIDE_IP,
    SUBTRACT_IP,

    NEGATE_IP,

    ADD_FT,
    MULTIPLY_FT,
    DIVIDE_FT,
    SUBTRACT_FT,

    INVERT_BOOLEAN,
    INVERT_BOOLEAN_IP,
    INVERT_BOOLEAN_FT,

    IS_GREATER,
    IS_GREATER_EQUAL,
    IS_LESS,
    IS_LESS_EQUAL,

    LOAD,
    LOAD_IP,

    LOAD_GLOBAL,
    LOAD_GLOBAL_IP,

    STORE,
    STORE_GLOBAL,

    LDC,
    LDC_IP,

    POP,
    POP_IF_PRESENT,
    PRINT_POP,

    DUP,

    JUMP_IF_TRUE,
    JUMP_IF_FALSE,
    JUMP,
    LOOP,

    GET,
    GET_IP,

    SET,
    SET_LDC,

    CALL,
    CALL_POP,
    CALL_METHOD,
    CALL_METHOD_POP,

    RETURN,
}

class Chunk {
    val code = mutableListOf<UByte>()
    val constants = mutableListOf<Value<*>>()
    private val lines = mutableListOf<Int>()

    fun getLine(ip: Int): Int {
        var length = 0
        for (i in 1 until lines.size step 2) {
            length += lines[i]
            if (length == ip)
                return lines[i + 1]
        }

        return -1
    }

    private fun write(byte: UByte, line: Int) {
        code.add(byte)

        if (lines.isNotEmpty() && lines.last() == line)
            lines[lines.lastIndex - 1] += 1
        else {
            lines.add(1)
            lines.add(line)
        }
    }

    fun write(line: Int, vararg bytes: Int) {
        bytes.forEach {
            write(it.toUByte(), line)
        }
    }

    fun write(line: Int, opcode: Opcode, vararg bytes: Int) {
        write(line, opcode.ordinal, *bytes)
    }

    fun writeConstant(line: Int, constant: Value<*>) {
        constants.add(constant)

        write(line, Opcode.LDC)
        write(line, constants.size - 1)
    }

    fun disassemble() {
        var offset = 0
        while (offset < code.size) {
            offset += disassembleInstruction(offset)
        }
    }

    private fun readByte(offset: Int): UByte = code[offset]
    private fun readShort(offset: Int) = (readByte(offset).toInt() shl 8) or readByte(offset + 1).toInt()
    private fun readConstant(offset: Int): Value<*> = constants[readByte(offset).toInt()]

    private fun disassembleInstruction(offset: Int): Int {
        return when (val inst = readByte(offset).toInt()) {
            Opcode.ADD.ordinal -> {
                println("ADD")

                1
            }

            Opcode.MULTIPLY.ordinal -> {
                println("MULTIPLY")

                1
            }

            Opcode.DIVIDE.ordinal -> {
                println("DIVIDE")

                1
            }

            Opcode.NEGATE.ordinal -> {
                println("NEGATE")

                1
            }

            Opcode.ADD_IP.ordinal -> {
                println("ADD_IP ${readByte(offset + 1)}")

                2
            }

            Opcode.MULTIPLY_IP.ordinal -> {
                println("MULTIPLY_IP ${readByte(offset + 1)}")

                2
            }

            Opcode.DIVIDE_IP.ordinal -> {
                println("DIVIDE_IP ${readByte(offset + 1)}")

                2
            }

            Opcode.NEGATE_IP.ordinal -> {
                println("NEGATE_IP ${readByte(offset + 1)}")

                2
            }

            Opcode.ADD_FT.ordinal -> {
                println("ADD_FT ${readByte(offset + 1)} ${readByte(offset + 2)} ${readByte(offset + 3)}")

                4
            }

            Opcode.MULTIPLY_FT.ordinal -> {
                println("MULTIPLY_FT ${readByte(offset + 1)} ${readByte(offset + 2)} ${readByte(offset + 3)}")

                4
            }

            Opcode.DIVIDE_FT.ordinal -> {
                println("DIVIDE_FT ${readByte(offset + 1)} ${readByte(offset + 2)} ${readByte(offset + 3)}")

                4
            }

            Opcode.IS_GREATER.ordinal -> {
                println("IS_GREATER")

                1
            }

            Opcode.IS_GREATER_EQUAL.ordinal -> {
                println("IS_GREATER_EQUAL")

                1
            }

            Opcode.IS_LESS.ordinal -> {
                println("IS_LESS")

                1
            }

            Opcode.IS_LESS_EQUAL.ordinal -> {
                println("IS_LESS_EQUAL")

                1
            }

            Opcode.LOAD.ordinal -> {
                println("LOAD ${readByte(offset + 1)}")

                2
            }

            Opcode.LOAD_IP.ordinal -> {
                println("LOAD_IP ${readByte(offset + 1)} ${readByte(offset + 2)}")

                3
            }

            Opcode.LOAD_GLOBAL.ordinal -> {
                println("LOAD_GLOBAL ${readByte(offset + 1)}")

                2
            }

            Opcode.LOAD_GLOBAL_IP.ordinal -> {
                println("LOAD_GLOBAL_IP ${readByte(offset + 1)} ${readByte(offset + 2)}")

                3
            }

            Opcode.STORE.ordinal -> {
                println("STORE ${readByte(offset + 1)}")

                2
            }

            Opcode.STORE_GLOBAL.ordinal -> {
                println("STORE_GLOBAL ${readByte(offset + 1)}")

                2
            }

            Opcode.LDC.ordinal -> {
                println("LDC ${readConstant(offset + 1)}")

                2
            }

            Opcode.LDC_IP.ordinal -> {
                println("LDC_IP ${readByte(offset + 1)} ${readConstant(offset + 2)}")

                2
            }

            Opcode.POP.ordinal -> {
                println("POP")

                1
            }

            Opcode.POP_IF_PRESENT.ordinal -> {
                println("POP_IF_PRESENT")

                1
            }

            Opcode.PRINT_POP.ordinal -> {
                println("PRINT_POP")

                1
            }

            Opcode.DUP.ordinal -> {
                println("DUP")

                1
            }

            Opcode.JUMP_IF_TRUE.ordinal -> {
                println("JUMP_IF_TRUE ${readShort(offset + 1)}")

                3
            }

            Opcode.JUMP_IF_FALSE.ordinal -> {
                println("JUMP_IF_FALSE ${readShort(offset + 1)}")

                3
            }

            Opcode.JUMP.ordinal -> {
                println("JUMP ${readShort(offset + 1)}")

                3
            }

            Opcode.LOOP.ordinal -> {
                println("LOOP ${readShort(offset + 1)}")

                3
            }

            Opcode.GET.ordinal -> {
                println("GET ${readConstant(offset + 1)}")

                2
            }

            Opcode.GET_IP.ordinal -> {
                println("GET_IP ${readConstant(offset + 1)} ${readByte(offset + 1)}")

                3
            }

            Opcode.SET.ordinal -> {
                println("SET ${readConstant(offset + 1)}")

                2
            }

            Opcode.CALL.ordinal -> {
                println("CALL ${readByte(offset + 1)}")

                2
            }

            Opcode.CALL_POP.ordinal -> {
                println("CALL_POP ${readByte(offset + 1)}")

                2
            }

            Opcode.CALL_METHOD.ordinal -> {
                println("CALL_METHOD ${readConstant(offset + 1)} ${readByte(offset + 2)}")

                3
            }

            Opcode.CALL_METHOD_POP.ordinal -> {
                println("CALL_METHOD_POP ${readConstant(offset + 1)} ${readByte(offset + 2)}")

                3
            }

            Opcode.RETURN.ordinal -> {
                println("RETURN")

                1
            }

            else -> throw IllegalStateException("Unexpected opcode $inst")
        }
    }
}
