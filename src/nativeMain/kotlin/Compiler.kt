class Compiler: ExpressionVisitor<Unit>, StatementVisitor<Unit> {
    val functions = mutableListOf<FunctionObject>()

    private var function: FunctionObject? = null
    private val chunk: Chunk
        get() = function!!.code

    private val globals = mutableMapOf<Int, Value<*>>()

    override fun visit(function: FunctionStatement) {
        this.function = FunctionObject(function.name.content, Chunk(), function.params)

        function.body.forEach {
            it.accept(this)
        }

        chunk.writeConstant(ObjectValue(NullObject), function.line)
        chunk.write(Opcode.RETURN, function.line)

        globals[function.index] = ObjectValue(
            this.function!!
        )

        functions.add(this.function!!)
    }

    private fun emitJump(opcode: Opcode, line: Int): Int {
        chunk.write(
            opcode,
            line
        )

        chunk.write(
            0xff,
            line
        )
        chunk.write(
            0xff,
            line
        )

        return chunk.code.size - 2
    }

    private fun patchJump(offset: Int) {
        val jump = chunk.code.size - offset - 2
        chunk.code[offset] = ((jump shr 8) and 0xff).toUByte()
        chunk.code[offset + 1] = (jump and 0xff).toUByte()
    }

    private fun emitLoop(loopStart: Int, line: Int) {
        chunk.write(
            Opcode.LOOP,
            line
        )

        val offset = chunk.code.size - loopStart + 2

        chunk.write((offset shr 8) and 0xff, line)
        chunk.write(offset and 0xff, line)
    }

    override fun visit(whileStatement: WhileStatement) {
        val loopStart = chunk.code.size

        whileStatement.condition.accept(this)

        val exitJump = emitJump(Opcode.IFNE, whileStatement.body.line)
        chunk.write(
            Opcode.POP,
            whileStatement.body.line
        )
        whileStatement.body.accept(this)
        emitLoop(loopStart, whileStatement.body.line)

        patchJump(exitJump)
        chunk.write(
            Opcode.POP,
            whileStatement.body.line
        )
    }

    override fun visit(ifStatement: IfStatement) {
        ifStatement.condition.accept(this)

        val thenJump = emitJump(Opcode.IFNE, ifStatement.thenBranch.line)
        ifStatement.thenBranch.accept(this)

        val elseJump = emitJump(Opcode.JUMP, ifStatement.elseBranch?.line ?: ifStatement.line)

        patchJump(thenJump)

        ifStatement.elseBranch?.accept(this)

        patchJump(elseJump)
    }

    override fun visit(expression: ExpressionStatement) {
        expression.expression.accept(this)
    }

    override fun visit(print: PrintStatement) {
        print.expression.accept(this)
        chunk.write(Opcode.PRINT_POP, print.line)
    }

    override fun visit(returnStatement: ReturnStatement) {
        if (returnStatement.returnValue == null)
            visit(LiteralExpression(null, returnStatement.line))
        else returnStatement.returnValue!!.accept(this)

        chunk.write(Opcode.RETURN, returnStatement.line)
    }

    override fun visit(declareVariable: VariableDeclarationStatement) {
        if (declareVariable.init != null)
            declareVariable.init!!.accept(this)
        else visit(LiteralExpression(null, declareVariable.line))

        chunk.write(
            Opcode.STORE, declareVariable.line
        )

        chunk.write(declareVariable.index, declareVariable.line)
    }

    override fun visit(block: BlockStatement) {
        block.statements.forEach {
            it.accept(this)
        }
    }

    override fun visit(logical: LogicalExpression) {
        val line = logical.operator.line

        logical.left.accept(this)

        when (logical.operator.type) {
            TokenType.AND -> {
                val endJump = emitJump(Opcode.IFNE, line)

                chunk.write(
                    Opcode.POP,
                    line
                )
                logical.right.accept(this)

                patchJump(endJump)
            }

            TokenType.OR -> {
                val elseJump = emitJump(Opcode.IFNE, line)
                val endJump = emitJump(Opcode.JUMP, line)

                patchJump(elseJump)
                chunk.write(
                    Opcode.POP,
                    line
                )

                logical.right.accept(this)
                patchJump(endJump)
            }

            else -> {}
        }
    }

    override fun visit(call: CallExpression) {
        call.callee.accept(this)

        call.arguments.forEach {
            it.accept(this)
        }

        chunk.write(
            Opcode.CALL,
            call.paren.line
        )

        chunk.write(
            call.arguments.size,
            call.paren.line
        )
    }

    override fun visit(getVariable: GetVariableExpression) {
        if (getVariable.isGlobal) {
            globals[getVariable.index]?.let { chunk.writeConstant(it, getVariable.name.line) }

            return
        }

        chunk.write(
            Opcode.LOAD, getVariable.name.line
        )

        chunk.write(getVariable.index, getVariable.name.line)
    }

    override fun visit(assignVariable: AssignVariableExpression) {
        assignVariable.assigned.accept(this)

        chunk.write(
            Opcode.DUP, assignVariable.name.line
        )

        chunk.write(
            Opcode.STORE, assignVariable.name.line
        )

        chunk.write(assignVariable.index, assignVariable.name.line)
    }

    override fun visit(unary: UnaryExpression) {
        if (unary.value != null) {
            chunk.writeConstant(
                unary.vmValue,
                unary.operator.line
            )

            return
        }

        unary.right.accept(this)

        when (unary.operator.type) {
            TokenType.MINUS -> chunk.write(Opcode.NEGATE, unary.operator.line)
            TokenType.BANG -> {
                val thenJump = emitJump(Opcode.IFNE, unary.operator.line)

                chunk.writeConstant(BooleanValue(false), unary.operator.line)

                val elseJump = emitJump(Opcode.JUMP, unary.operator.line)

                patchJump(thenJump)

                chunk.writeConstant(BooleanValue(true), unary.operator.line)

                patchJump(elseJump)
            }
            else -> {}
        }
    }

    override fun visit(binary: BinaryExpression) {
        if (binary.value != null) {
            chunk.writeConstant(
                binary.vmValue,
                binary.operator.line
            )

            return
        }

        binary.left.accept(this)
        binary.right.accept(this)

        if (!binary.left.type.isNumber || !binary.right.type.isNumber)
            error("Numbers are required to make binary operations!")

        if (binary.operator.type == TokenType.SLASH && binary.right.value != null) {
            chunk.writeConstant(
                DoubleValue(
                    1.0 / (binary.right.value as NumberValue<*>).value.toDouble()
                ),
                binary.operator.line
            )
            chunk.write(Opcode.MULTIPLY, binary.operator.line)

            return
        }

        when (binary.operator.type) {
            TokenType.MINUS -> {
                chunk.write(Opcode.NEGATE, binary.operator.line)
                chunk.write(Opcode.ADD, binary.operator.line)
            }
            TokenType.SLASH -> chunk.write(Opcode.DIVIDE, binary.operator.line)
            TokenType.STAR -> chunk.write(Opcode.MULTIPLY, binary.operator.line)
            TokenType.PLUS -> chunk.write(Opcode.ADD, binary.operator.line)

            TokenType.GREATER -> chunk.write(Opcode.IS_GREATER, binary.operator.line)
            TokenType.GREATER_EQUAL -> chunk.write(Opcode.IS_GREATER_EQUAL, binary.operator.line)
            TokenType.LESS -> chunk.write(Opcode.IS_LESS, binary.operator.line)
            TokenType.LESS_EQUAL -> chunk.write(Opcode.IS_LESS_EQUAL, binary.operator.line)

            else -> {}
        }
    }

    override fun visit(grouping: GroupingExpression) {
        grouping.groupedValue.accept(this)
    }

    override fun visit(literal: LiteralExpression) {
        chunk.writeConstant(
            literal.toValue(),
            literal.line
        )
    }
}