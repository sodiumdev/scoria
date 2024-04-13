interface StatementVisitor<R> {
    fun visit(classStatement: ClassStatement): R
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

data class ClassStatement(val name: Token, val methods: List<FunctionStatement>, var fields: Map<String, Field>, val index: Int): Statement {
    override val line: Int = name.line

    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class FunctionStatement(val name: Token, val params: List<Pair<Token, ExpressionType>>, val returnValue: ExpressionType?, var body: List<Statement>, val index: Int): Statement {
    override val line: Int = name.line

    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class WhileStatement(var condition: Expression, var body: Statement, override val line: Int): Statement {
    override fun <R> accept(visitor: StatementVisitor<R>): R = visitor.visit(this)
}

data class IfStatement(
    var condition: Expression, var thenBranch: Statement, var elseBranch: Statement?,
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
    companion object {
        val NULL = ReturnStatement(
            Token(TokenType.RETURN, "return", -1),
            null
        )
    }

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
