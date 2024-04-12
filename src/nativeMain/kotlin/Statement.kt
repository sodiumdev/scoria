interface StatementVisitor<R> {
    fun visit(function: FunctionStatement): R
    fun visit(ifStatement: IfStatement): R
    fun visit(whileStatement: WhileStatement): R
    fun visit(expression: ExpressionStatement): R
    fun visit(print: PrintStatement): R
    fun visit(returnStatement: ReturnStatement): R
    fun visit(declareVariable: VariableDeclarationStatement): R
    fun visit(block: BlockStatement): R
}

sealed interface Statement {
    val line: Int

    fun <R> accept(visitor: StatementVisitor<R>): R
}

data class FunctionStatement(val name: Token, val params: List<Pair<Token, ExpressionType>>, val returnValue: ExpressionType?, var body: List<Statement>, val index: Int): Statement {
    override val line: Int = name.line

    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class WhileStatement(var condition: Expression, val body: Statement, override val line: Int): Statement {
    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class IfStatement(
    var condition: Expression, val thenBranch: Statement, val elseBranch: Statement?,
    override val line: Int): Statement {
    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class ExpressionStatement(var expression: Expression, override val line: Int): Statement {
    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class PrintStatement(var expression: Expression, override val line: Int): Statement {
    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class ReturnStatement(val keyword: Token, var returnValue: Expression?): Statement {
    override val line: Int = keyword.line

    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class BlockStatement(var statements: List<Statement>, override val line: Int): Statement {
    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class VariableDeclarationStatement(val name: Token, val local: Local): Statement {
    override val line: Int = name.line

    val index: Int = local.index
    var init: Expression? = local.value

    val isUsed: Boolean
        get() = local.isUsed

    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}
