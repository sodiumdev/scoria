class Compiler: ExpressionVisitor<Unit>, StatementVisitor<Unit> {
    val script = FunctionObject("<script>", Chunk(), listOf(), null)

    private var function: FunctionObject? = null
    private val chunk: Chunk
        get() = function!!.code

    override fun visit(classStatement: ClassStatement) {
        script.code.writeConstant(
            classStatement.line,
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

                        if (it.name.content == "<init>") {
                            val line = it.name.line
                            classStatement.fields.forEach {
                                it.value.value?.accept(this) ?: chunk.writeConstant(
                                    line,
                                    NullValue
                                )

                                chunk.write(
                                    line,
                                    Opcode.LOAD,
                                    0
                                )

                                chunk.constants.add(ObjectValue(StringObject(it.key)))
                                chunk.write(
                                    line,
                                    Opcode.SET,
                                    chunk.constants.size - 1
                                )
                            }
                        }

                        it.body.forEach {
                            it.accept(this)
                        }

                        val func = this.function!!
                        func.isMethod = true

                        this.function = script

                        func.name to func
                    }.toTypedArray()))
                )
            )
        )

        script.code.write(
            classStatement.line,
            Opcode.STORE_GLOBAL
        )

        script.code.write(
            classStatement.line,
            classStatement.index
        )
    }

    override fun visit(function: FunctionStatement) {
        this.function = FunctionObject(function.name.content, Chunk(), function.params, function.returnValue)

        function.body.forEach {
            it.accept(this)
        }

        val compiled = this.function!!

        this.function = null

        script.code.writeConstant(
            function.line,
            ObjectValue(
                compiled
            )
        )

        script.code.write(
            function.line,
            Opcode.STORE_GLOBAL
        )

        script.code.write(
            function.line,
            function.index
        )

        if (function.name.content == "main") {
            script.code.write(
                function.line,
                Opcode.LOAD_GLOBAL
            )

            script.code.write(
                function.line,
                function.index
            )

            script.code.write(
                function.line,
                Opcode.CALL
            )

            script.code.write(
                function.line,
                0
            )
        }
    }

    private fun emitJump(opcode: Opcode, line: Int): Int {
        chunk.write(
            line,
            opcode,
            0xff,
            0xff
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
            line,
            Opcode.LOOP
        )

        val offset = chunk.code.size - loopStart + 2

        chunk.write(
            line,
            (offset shr 8) and 0xff,
            offset and 0xff
        )
    }

    override fun visit(whileStatement: WhileStatement) {
        val loopStart = chunk.code.size

        whileStatement.condition.accept(this)

        val exitJump = emitJump(Opcode.JUMP_IF_FALSE, whileStatement.body.line)

        whileStatement.body.accept(this)
        emitLoop(loopStart, whileStatement.body.line)

        patchJump(exitJump)
    }

    override fun visit(ifStatement: IfStatement) {
        ifStatement.condition.accept(this)

        val thenJump = emitJump(Opcode.JUMP_IF_FALSE, ifStatement.thenBranch.line)
        ifStatement.thenBranch.accept(this)

        val elseJump = emitJump(Opcode.JUMP, ifStatement.elseBranch?.line ?: ifStatement.line)

        patchJump(thenJump)

        ifStatement.elseBranch?.accept(this)

        patchJump(elseJump)
    }

    override fun visit(expression: ExpressionStatement) {
        val expr = expression.expression

        if (expression.shouldPop)
            if (expr is CallExpression) {
                expr.shouldPop = true
                expr.accept(this)
            } else {
                expr.accept(this)
                chunk.write(
                    expression.line, Opcode.POP
                )
            }
        else expr.accept(this)
    }

    override fun visit(print: PrintStatement) {
        print.expression.accept(this)
        chunk.write(print.line, Opcode.PRINT_POP)
    }

    override fun visit(returnStatement: ReturnStatement) {
        if (returnStatement.returnValue != null)
            returnStatement.returnValue!!.accept(this)

        chunk.write(returnStatement.line, Opcode.RETURN)
    }

    override fun visit(declareVariable: VariableDeclarationStatement) {
        if (declareVariable.init != null) {
            if (declareVariable.init is BinaryExpression) {
                val binary = declareVariable.init as BinaryExpression
                if (writeBinaryShortTerm(binary, null, declareVariable.index, declareVariable.line))
                    return
            } else declareVariable.init!!.accept(this)
        } else chunk.writeConstant(
            declareVariable.line,
            NullValue
        )

        chunk.write(
            declareVariable.line,
            Opcode.STORE,
            declareVariable.index
        )
    }

    private fun writeBinaryShortTerm(binary: BinaryExpression, leftHandSide: Expression?, index: Int, line: Int): Boolean {
        val isLeftVariableExpr = binary.left is GetVariableExpression
        val isRightVariableExpr = binary.right is GetVariableExpression

        if (isLeftVariableExpr && isRightVariableExpr) {
            val left = (binary.left as GetVariableExpression)
            val right = (binary.right as GetVariableExpression)

            when (binary.operator.type) {
                TokenType.STAR -> Opcode.MULTIPLY_FT
                TokenType.SLASH -> Opcode.DIVIDE_FT
                TokenType.PLUS -> Opcode.ADD_FT
                TokenType.MINUS -> Opcode.SUBTRACT_FT

                else -> null
            }?.let {
                chunk.write(
                    line,
                    it,
                    left.index,
                    right.index,
                    index
                )

                return true
            }
        }

        if (leftHandSide != null) {
            if (isLeftVariableExpr && leftHandSide == binary.left) {
                binary.right.accept(this)

                when (binary.operator.type) {
                    TokenType.STAR -> Opcode.MULTIPLY_IP
                    TokenType.SLASH -> Opcode.DIVIDE_IP
                    TokenType.PLUS -> Opcode.ADD_IP
                    TokenType.MINUS -> Opcode.SUBTRACT_IP

                    else -> null
                }?.let {
                    chunk.write(
                        line,
                        it,
                        index
                    )

                    return true
                }
            }

            if (isRightVariableExpr && leftHandSide == binary.right) {
                binary.left.accept(this)

                when (binary.operator.type) {
                    TokenType.STAR -> Opcode.MULTIPLY_IP
                    TokenType.SLASH -> Opcode.DIVIDE_IP
                    TokenType.PLUS -> Opcode.ADD_IP
                    TokenType.MINUS -> Opcode.SUBTRACT_IP

                    else -> null
                }?.let {
                    chunk.write(
                        line,
                        it,
                        index
                    )

                    return true
                }
            }
        }

        return false
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
                val endJump = emitJump(Opcode.JUMP_IF_FALSE, line)

                logical.right.accept(this)

                patchJump(endJump)
            }

            TokenType.OR -> {
                val elseJump = emitJump(Opcode.JUMP_IF_FALSE, line)
                val endJump = emitJump(Opcode.JUMP, line)

                patchJump(elseJump)

                logical.right.accept(this)
                patchJump(endJump)
            }

            else -> {}
        }
    }

    override fun visit(setProperty: SetPropertyExpression) {
        setProperty.parent.accept(this)
        setProperty.rightHandSide.accept(this)

        chunk.constants.add(ObjectValue(StringObject(setProperty.name.content)))

        chunk.write(
            setProperty.name.line,
            Opcode.SET,
            chunk.constants.size - 1
        )
    }

    override fun visit(getProperty: GetPropertyExpression) {
        getProperty.parent.accept(this)

        chunk.constants.add(ObjectValue(StringObject(getProperty.name.content)))

        chunk.write(
            getProperty.name.line,
            Opcode.GET,
            chunk.constants.size - 1
        )
    }

    override fun visit(call: CallExpression) {
        call.callee.accept(this)

        call.arguments.forEach {
            it.accept(this)
        }

        chunk.write(
            call.paren.line,
            if (call.shouldPop) Opcode.CALL_POP else Opcode.CALL,
            call.arguments.size
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

        chunk.write(
            call.paren.line,
            if (call.shouldPop) Opcode.CALL_METHOD_POP else Opcode.CALL_METHOD,
            chunk.constants.size - 1,
            call.arguments.size
        )
    }

    override fun visit(getVariable: GetVariableExpression) {
        chunk.write(
            getVariable.name.line,
            if (getVariable.isGlobal) Opcode.LOAD_GLOBAL else Opcode.LOAD,
            getVariable.index
        )
    }

    override fun visit(duplicate: DuplicateExpression) {
        duplicate.expression.accept(this)

        chunk.write(
            duplicate.line,
            Opcode.DUP
        )
    }

    override fun visit(assignVariable: AssignVariableExpression) {
        if (assignVariable.rightHandSide is BinaryExpression) {
            val rhs = (assignVariable.rightHandSide as BinaryExpression)
            if (writeBinaryShortTerm(rhs, assignVariable.leftHandSide, assignVariable.index, assignVariable.name.line))
                return
        }

        assignVariable.rightHandSide.accept(this)

        chunk.write(
            assignVariable.name.line,
            Opcode.STORE,
            assignVariable.index
        )
    }

    override fun visit(unary: UnaryExpression) {
        if (unary.value != null) {
            chunk.writeConstant(
                unary.operator.line,
                unary.vmValue
            )

            return
        }

        unary.right.accept(this)

        when (unary.operator.type) {
            TokenType.MINUS -> chunk.write(unary.operator.line, Opcode.NEGATE)
            TokenType.BANG -> chunk.write(unary.operator.line, Opcode.INVERT_BOOLEAN)

            else -> {}
        }
    }

    override fun visit(binary: BinaryExpression) {
        if (binary.value != null) {
            chunk.writeConstant(
                binary.operator.line,
                binary.vmValue
            )

            return
        }

        if (!binary.left.type.isNumber || !binary.right.type.isNumber)
            error("Numbers are required to make binary operations!")

        if (binary.operator.type == TokenType.SLASH) {
            if (binary.right.value != null) {
                binary.left.accept(this)

                chunk.writeConstant(
                    binary.operator.line,
                    DoubleValue(
                        1.0 / (binary.right.value as NumberValue<*>).value.toDouble()
                    )
                )
                chunk.write(binary.operator.line, Opcode.MULTIPLY)

                return
            }

            if (binary.left.value == 0) {
                chunk.writeConstant(
                    binary.operator.line,
                    DoubleValue(
                        0.0
                    )
                )

                return
            }
        }

        if (binary.operator.type == TokenType.STAR
            && (binary.left.value == 0 || binary.right.value == 0)) {
            chunk.writeConstant(
                binary.operator.line,
                DoubleValue(
                    0.0
                )
            )

            return
        }

        binary.left.accept(this)
        binary.right.accept(this)

        when (binary.operator.type) {
            TokenType.MINUS -> chunk.write(binary.operator.line, Opcode.NEGATE, Opcode.ADD.ordinal)
            TokenType.SLASH -> chunk.write(binary.operator.line, Opcode.DIVIDE)
            TokenType.STAR -> chunk.write(binary.operator.line, Opcode.MULTIPLY)
            TokenType.PLUS -> chunk.write(binary.operator.line, Opcode.ADD)

            TokenType.GREATER -> chunk.write(binary.operator.line, Opcode.IS_GREATER)
            TokenType.GREATER_EQUAL -> chunk.write(binary.operator.line, Opcode.IS_GREATER_EQUAL)
            TokenType.LESS -> chunk.write(binary.operator.line, Opcode.IS_LESS)
            TokenType.LESS_EQUAL -> chunk.write(binary.operator.line, Opcode.IS_LESS_EQUAL)

            else -> {}
        }
    }

    override fun visit(grouping: GroupingExpression) {
        grouping.groupedValue.accept(this)
    }

    override fun visit(literal: LiteralExpression) {
        chunk.writeConstant(
            literal.line,
            literal.toValue()
        )
    }
}