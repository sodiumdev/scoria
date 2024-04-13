class Compiler: ExpressionVisitor<Unit>, StatementVisitor<Unit> {
    val functions = mutableListOf<FunctionObject>()

    val script = FunctionObject("<script>", Chunk(), listOf(), null)

    private var function: FunctionObject? = null
    private val chunk: Chunk
        get() = function!!.code

    override fun visit(classStatement: ClassStatement) {
        script.code.writeConstant(
            ObjectValue(
                ClassObject(
                    classStatement.name.content,
                    mutableMapOf(*(classStatement.methods.map { it ->
                        this.function = FunctionObject(
                            it.name.content,
                            Chunk(),
                            it.params,
                            it.returnValue
                        )

                        it.body.forEach {
                            it.accept(this)
                        }

                        val func = this.function!!
                        func.isMethod = true

                        this.function = script

                        func.name to func
                    }.toTypedArray()))
                )
            ),
            classStatement.line
        )

        script.code.write(
            Opcode.STORE_GLOBAL,
            classStatement.line
        )

        script.code.write(
            classStatement.index,
            classStatement.line
        )
    }

    override fun visit(function: FunctionStatement) {
        this.function = FunctionObject(function.name.content, Chunk(), function.params, function.returnValue)

        function.body.forEach {
            it.accept(this)
        }

        if (function.name.content == "main")
            this.function!!.isMainFunction = true

        functions.add(this.function!!)

        val compiled = this.function!!

        this.function = null

        script.code.writeConstant(
            ObjectValue(
                compiled
            ),
            function.line
        )

        script.code.write(
            Opcode.STORE_GLOBAL,
            function.line
        )

        script.code.write(
            function.index,
            function.line
        )
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
        if (expression.expression is CallExpression)
            chunk.write(
                Opcode.POP_IF_PRESENT, expression.line
            )
        else chunk.write(
            Opcode.POP, expression.line
        )
    }

    override fun visit(print: PrintStatement) {
        print.expression.accept(this)
        chunk.write(Opcode.PRINT_POP, print.line)
    }

    override fun visit(returnStatement: ReturnStatement) {
        if (returnStatement.returnValue != null)
            returnStatement.returnValue!!.accept(this)

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

    override fun visit(setProperty: SetPropertyExpression) {
        setProperty.parent.accept(this)
        setProperty.assigned.accept(this)

        chunk.constants.add(ObjectValue(StringObject(setProperty.name.content)))

        chunk.write(
            Opcode.SET,
            setProperty.name.line
        )

        chunk.write(
            chunk.constants.size - 1,
            setProperty.name.line
        )
    }

    override fun visit(getProperty: GetPropertyExpression) {
        getProperty.parent.accept(this)

        chunk.constants.add(ObjectValue(StringObject(getProperty.name.content)))

        chunk.write(
            Opcode.GET,
            getProperty.name.line
        )

        chunk.write(
            chunk.constants.size - 1,
            getProperty.name.line
        )
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

    override fun visit(call: MethodCallExpression) {
        call.callee.accept(this)
        call.arguments.forEach {
            it.accept(this)
        }

        chunk.constants.add(
            ObjectValue(
                StringObject(
                    call.name.content
                )
            )
        )

        val identifierLocation = chunk.constants.size - 1

        chunk.write(
            Opcode.CALL_METHOD,
            call.paren.line
        )

        chunk.write(
            identifierLocation,
            call.paren.line
        )

        chunk.write(
            call.arguments.size,
            call.paren.line
        )
    }

    override fun visit(getVariable: GetVariableExpression) {
        chunk.write(
            if (getVariable.isGlobal) Opcode.LOAD_GLOBAL else Opcode.LOAD, getVariable.name.line
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

        if (!binary.left.type.isNumber || !binary.right.type.isNumber)
            error("Numbers are required to make binary operations!")

        if (binary.operator.type == TokenType.SLASH) {
            if (binary.right.value != null) {
                binary.left.accept(this)

                chunk.writeConstant(
                    DoubleValue(
                        1.0 / (binary.right.value as NumberValue<*>).value.toDouble()
                    ),
                    binary.operator.line
                )
                chunk.write(Opcode.MULTIPLY, binary.operator.line)

                return
            }

            if (binary.left.value == 0) {
                chunk.writeConstant(
                    DoubleValue(
                        0.0
                    ),
                    binary.operator.line
                )

                return
            }
        }

        if (binary.operator.type == TokenType.STAR
            && (binary.left.value == 0 || binary.right.value == 0)) {
            chunk.writeConstant(
                DoubleValue(
                    0.0
                ),
                binary.operator.line
            )

            return
        }

        binary.left.accept(this)
        binary.right.accept(this)

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