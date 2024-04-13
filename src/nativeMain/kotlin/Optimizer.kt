class LiteralOptimizer: ExpressionVisitor<Expression>, StatementVisitor<Unit> {
    override fun visit(setProperty: SetPropertyExpression): Expression {
        setProperty.parent = setProperty.parent.accept(this)
        setProperty.assigned = setProperty.assigned.accept(this)

        return setProperty
    }

    override fun visit(getProperty: GetPropertyExpression): Expression {
        getProperty.parent = getProperty.parent.accept(this)

        return getProperty
    }

    override fun visit(call: CallExpression): Expression {
        call.callee = call.callee.accept(this)
        call.arguments = call.arguments.map {
            it.accept(this)
        }

        return call
    }

    override fun visit(unary: UnaryExpression): Expression {
        unary.value?.let {
            return LiteralExpression(it, unary.operator.line)
        }

        unary.right = unary.right.accept(this)

        return unary
    }

    override fun visit(logical: LogicalExpression): Expression {
        logical.value?.let {
            return LiteralExpression(it, logical.operator.line)
        }

        logical.left = logical.left.accept(this)
        logical.right = logical.right.accept(this)

        return logical
    }

    override fun visit(binary: BinaryExpression): Expression {
        binary.value?.let {
            return LiteralExpression(it, binary.operator.line)
        }

        binary.left = binary.left.accept(this)
        binary.right = binary.right.accept(this)

        return binary
    }

    override fun visit(getVariable: GetVariableExpression): Expression {
        val value = getVariable.local.value?.accept(this) ?: run {
            getVariable.local.isUsed = true

            return getVariable
        }

        getVariable.local.isUsed = false
        getVariable.local.value = value

        return value
    }

    override fun visit(assignVariable: AssignVariableExpression): Expression {
        assignVariable.assigned = assignVariable.assigned.accept(this)

        return assignVariable
    }

    override fun visit(grouping: GroupingExpression) = grouping.groupedValue.accept(this)

    override fun visit(literal: LiteralExpression) = literal
    override fun visit(classStatement: ClassStatement) {
        classStatement.methods.forEach { it ->
            it.body.forEach {
                it.accept(this)
            }
        }

        classStatement.fields = classStatement.fields.mapValues {
            val expr = it.value.value?.accept(this) ?: return@mapValues it.value

            Field(
                expr.type,
                expr
            )
        }
    }

    override fun visit(function: FunctionStatement) {
        function.body.forEach {
            it.accept(this)
        }
    }

    override fun visit(ifStatement: IfStatement) {
        ifStatement.condition = ifStatement.condition.accept(this)
        ifStatement.thenBranch.accept(this)
        ifStatement.elseBranch?.accept(this)
    }

    override fun visit(whileStatement: WhileStatement) {
        whileStatement.condition = whileStatement.condition.accept(this)
        whileStatement.body.accept(this)
    }

    override fun visit(expression: ExpressionStatement) {
        expression.expression = expression.expression.accept(this)
    }

    override fun visit(print: PrintStatement) {
        print.expression = print.expression.accept(this)
    }

    override fun visit(returnStatement: ReturnStatement) {
        returnStatement.returnValue = returnStatement.returnValue?.accept(this)
    }

    override fun visit(declareVariable: VariableDeclarationStatement) {
        declareVariable.init = declareVariable.init?.accept(this)
    }

    override fun visit(block: BlockStatement) {
        block.statements.forEach {
            it.accept(this)
        }
    }
}

class DeadCodeEliminator: ExpressionVisitor<Expression?>, StatementVisitor<Statement?> {
    private var blockReturned: Boolean = false
    override fun visit(classStatement: ClassStatement): Statement {
        classStatement.methods.forEach { it ->
            blockReturned = false
            it.body = it.body.mapNotNull {
                it.accept(this)
            }
        }

        classStatement.fields = classStatement.fields.mapValues {
            val expr = it.value.value?.accept(this) ?: return@mapValues it.value

            Field(
                expr.type,
                expr
            )
        }

        return classStatement
    }

    override fun visit(function: FunctionStatement): Statement {
        blockReturned = false
        function.body = function.body.mapNotNull {
            it.accept(this)
        }

        return function
    }

    override fun visit(ifStatement: IfStatement): Statement? {
        if (ifStatement.condition.value == false)
            return ifStatement.elseBranch?.accept(this)
        else if (ifStatement.condition.value == true)
            return ifStatement.thenBranch.accept(this)

        blockReturned = false
        ifStatement.thenBranch = ifStatement.thenBranch.accept(this) ?: ifStatement.thenBranch

        blockReturned = false
        ifStatement.elseBranch = ifStatement.elseBranch?.accept(this)

        return ifStatement
    }

    override fun visit(whileStatement: WhileStatement): Statement? {
        if (whileStatement.condition.value == false)
            return null

        whileStatement.condition.accept(this)?.let { whileStatement.condition = it }

        blockReturned = false
        whileStatement.body = whileStatement.body.accept(this) ?: whileStatement.body

        return whileStatement
    }

    override fun visit(expression: ExpressionStatement): Statement? {
        if (blockReturned)
            return null

        expression.expression = expression.expression.accept(this) ?: expression.expression

        return expression
    }

    override fun visit(print: PrintStatement): Statement? {
        if (blockReturned)
            return null

        print.expression = print.expression.accept(this) ?: print.expression

        return print
    }

    override fun visit(returnStatement: ReturnStatement): Statement? {
        if (blockReturned)
            return null

        blockReturned = true

        returnStatement.returnValue = returnStatement.returnValue?.accept(this)

        return returnStatement
    }

    override fun visit(declareVariable: VariableDeclarationStatement): Statement? {
        if (blockReturned)
            return null
        if (!declareVariable.isUsed) {
            if (declareVariable.init?.hasCall == true)
                return ExpressionStatement(declareVariable.init!!, declareVariable.line)

            return null
        }

        declareVariable.init = declareVariable.init?.accept(this)

        return declareVariable
    }

    override fun visit(block: BlockStatement): Statement {
        blockReturned = false
        block.statements = block.statements.mapNotNull {
            it.accept(this)
        }

        return block
    }

    override fun visit(setProperty: SetPropertyExpression): Expression {
        setProperty.parent = setProperty.parent.accept(this) ?: setProperty.parent
        setProperty.assigned = setProperty.assigned.accept(this) ?: setProperty.assigned

        return setProperty
    }

    override fun visit(getProperty: GetPropertyExpression): Expression {
        getProperty.parent = getProperty.parent.accept(this) ?: getProperty.parent

        return getProperty
    }

    override fun visit(call: CallExpression): Expression {
        call.callee = call.callee.accept(this) ?: call.callee
        call.arguments = call.arguments.map {
            it.accept(this) ?: it
        }

        return call
    }

    override fun visit(unary: UnaryExpression): Expression {
        unary.right = unary.right.accept(this) ?: unary.right

        return unary
    }

    override fun visit(logical: LogicalExpression): Expression {
        logical.left = logical.left.accept(this) ?: logical.left
        logical.right = logical.right.accept(this) ?: logical.right

        return logical
    }

    override fun visit(binary: BinaryExpression): Expression {
        binary.left = binary.left.accept(this) ?: binary.left
        binary.right = binary.right.accept(this) ?: binary.right

        return binary
    }

    override fun visit(getVariable: GetVariableExpression) = getVariable

    override fun visit(assignVariable: AssignVariableExpression): Expression? {
        if (!assignVariable.local.isUsed) {
            if (assignVariable.assigned.hasCall)
                return assignVariable.assigned.accept(this)

            return null
        }

        return assignVariable
    }

    override fun visit(grouping: GroupingExpression) = grouping.groupedValue.accept(this) ?: grouping.groupedValue

    override fun visit(literal: LiteralExpression) = literal
}