private val NAME_TO_TYPE_MAP = mapOf(
    "int" to ExpressionType.INT,
    "float" to ExpressionType.FLOAT,
    "double" to ExpressionType.DOUBLE,
    "long" to ExpressionType.LONG,
    "bool" to ExpressionType.BOOLEAN,
    "any" to ExpressionType.OBJECT
)

data class Local(val index: Int, val type: ExpressionType, var value: Expression?, val isGlobal: Boolean, var isUsed: Boolean = false, var isReassigned: Boolean = false)

class Environment(private var parent: Environment?) {
    private val locals = mutableMapOf<String, Local>()
    val size: Int
        get() = locals.size

    fun assign(name: String, value: Local) {
        if (locals[name] != null)
            locals[name] = value
        else if (parent != null)
            parent?.assign(name, value)
        else error("Undefined variable $name!")
    }

    operator fun get(name: String): Local {
        val local = locals[name]
        if (local == null) {
            if (parent != null)
                return parent!![name]

            error("Undefined variable $name!")
        }

        return local
    }

    operator fun set(content: String, value: Local) {
        locals[content] = value
    }
}

class Parser(source: String) {
    companion object {
        fun errorAt(token: Token, message: String) = IllegalStateException(
            "[line %d] Error" + (
                    if (token.type == TokenType.END_OF_FILE)
                        " at end: $message"
                    else if (token.type != TokenType.ERROR)
                        " at \"${token.content}\": $message"
                    else ": $message")
        )
    }

    private var scanner = Scanner(source)

    init {
        advance()
    }

    private val atEnd: Boolean
        get() = current!!.type == TokenType.END_OF_FILE

    private var current: Token? = null
    private var previous: Token? = null

    private val globals = Environment(null)
    private var environment = globals

    private fun advance() {
        previous = current

        while (true) {
            current = scanner.scan()
            if (current!!.type != TokenType.ERROR)
                break

            throw errorAt(current!!, "")
        }
    }

    private fun check(type: TokenType): Boolean {
        if (atEnd)
            return false
        return current!!.type == type
    }

    private fun match(vararg types: TokenType): Boolean {
        if (types.any { check(it) }) {
            advance()
            return true
        }

        return false
    }

    private fun consume(type: TokenType, message: String): Token {
        if (match(type))
            return previous!!

        throw errorAt(previous!!, message)
    }

    private fun primary(): Expression {
        if (match(TokenType.FALSE)) return LiteralExpression(false, previous!!.line)
        if (match(TokenType.TRUE)) return LiteralExpression(true, previous!!.line)
        if (match(TokenType.NULL)) return LiteralExpression(null, previous!!.line)

        if (match(TokenType.STRING))
            return LiteralExpression(StringObject(previous!!.content.removePrefix("\"").removeSuffix("\"")), previous!!.line)
        if (match(TokenType.FLOAT))
            return LiteralExpression(previous!!.content.toFloat(), previous!!.line)
        if (match(TokenType.INTEGER))
            return LiteralExpression(previous!!.content.toInt(), previous!!.line)
        if (match(TokenType.IDENTIFIER)) {
            val local = environment[previous!!.content]
            local.isUsed = true

            return GetVariableExpression(previous!!, local)
        }

        if (match(TokenType.LEFT_PAREN)) {
            val expr = expression()
            consume(TokenType.RIGHT_PAREN, "Expected \")\" after grouping expression")

            return GroupingExpression(expr, previous!!.line)
        }

        throw IllegalStateException()
    }

    private fun finishCall(callee: Expression): Expression {
        val arguments = mutableListOf<Expression>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                arguments.add(expression())
            } while (match(TokenType.COMMA))
        }

        val paren = consume(
            TokenType.RIGHT_PAREN,
            "Expected \")\" after arguments"
        )

        return CallExpression(callee, paren, arguments)
    }

    private fun call(): Expression {
        var expr = primary()

        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr)
            } else break
        }

        return expr
    }

    private fun unary(): Expression {
        return if (match(TokenType.BANG, TokenType.MINUS))
            UnaryExpression(unary(), previous!!)
        else call()
    }

    private fun factor(): Expression {
        var expr = unary()

        while (match(TokenType.SLASH, TokenType.STAR)) {
            val operator = previous!!
            expr = BinaryExpression(expr, unary(), operator)
        }

        return expr
    }

    private fun term(): Expression {
        var expr = factor()

        while (match(TokenType.MINUS, TokenType.PLUS)) {
            val operator = previous!!
            expr = BinaryExpression(expr, factor(), operator)
        }

        return expr
    }

    private fun comparison(): Expression {
        var expr = term()

        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            val operator = previous!!
            expr = BinaryExpression(expr, term(), operator)
        }

        return expr
    }

    private fun equality(): Expression {
        var expr = comparison()

        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            val operator = previous!!
            expr = BinaryExpression(expr, term(), operator)
        }

        return expr
    }

    private fun and(): Expression {
        var expr = equality()

        while (match(TokenType.AND)) {
            val operator: Token = previous!!
            expr = LogicalExpression(expr, equality(), operator)
        }

        return expr
    }

    private fun or(): Expression {
        var expr = and()

        while (match(TokenType.OR)) {
            val operator = previous!!
            expr = LogicalExpression(expr, and(), operator)
        }

        return expr
    }

    private fun assignment(): Expression {
        val expr = or()

        if (match(TokenType.EQUAL)) {
            val equals = previous!!
            val value = assignment()

            if (expr is GetVariableExpression) {
                val local = environment[expr.name.content]
                local.isReassigned = true

                environment.assign(expr.name.content, Local(
                    local.index,
                    value.type,
                    value,
                    isGlobal = local.isGlobal,
                    isUsed = local.isUsed,
                    isReassigned = true
                ))

                return AssignVariableExpression(expr.name, value, local.index)
            }

            throw errorAt(equals, "Invalid assignment target")
        }

        return expr
    }

    private fun expression() = assignment()

    private fun printStatement(): Statement {
        val value = expression()
        consume(TokenType.SEMICOLON, "Expected \";\" after print value")

        return PrintStatement(value, previous!!.line)
    }

    private fun expressionStatement(): Statement {
        val expression = expression()
        consume(TokenType.SEMICOLON, "Expected \";\" after expression")

        return ExpressionStatement(expression, previous!!.line)
    }

    private fun block(): List<Statement> {
        val previous = environment
        environment = Environment(environment)

        val statements = mutableListOf<Statement>()

        while (!check(TokenType.RIGHT_BRACE) && !atEnd) {
            statements.add(declaration())
        }

        consume(TokenType.RIGHT_BRACE, "Expected \"}\" after block")

        environment = previous

        return statements
    }

    private fun ifStatement(): Statement {
        val condition = expression()
        if (condition.type != ExpressionType.BOOLEAN)
            throw errorAt(previous!!, "Expected boolean as if condition")

        consume(TokenType.LEFT_BRACE, "Expected \"{\" after if condition")
        val line = previous!!.line

        val thenBranch = BlockStatement(block(), line)
        val elseBranch: Statement? = if (match(TokenType.ELSE))
            statement()
        else null

        return IfStatement(condition, thenBranch, elseBranch, line)
    }

    private fun whileStatement(): Statement {
        val condition = expression()
        if (condition.type != ExpressionType.BOOLEAN)
            throw errorAt(previous!! , "Expected boolean as while condition")

        consume(TokenType.LEFT_BRACE, "Expected \"{\" after while condition")
        val line = previous!!.line

        val body = BlockStatement(block(), line)

        return WhileStatement(condition, body, line)
    }

    private fun forStatement(): Statement {
        val initializer: Statement? = if (match(TokenType.SEMICOLON))
            null
        else if (match(TokenType.LET))
            variableDeclaration()
        else expressionStatement()

        var condition: Expression? = null
        if (!check(TokenType.SEMICOLON))
            condition = expression()

        consume(TokenType.SEMICOLON, "Expected \";\" after loop condition")

        var increment: Expression? = null
        if (!check(TokenType.RIGHT_PAREN))
            increment = expression()

        consume(TokenType.LEFT_BRACE, "Expected \"{\" after for increment")
        val line = previous!!.line

        var body: Statement = BlockStatement(block(), line)

        if (increment != null)
            body = BlockStatement(
                listOf(
                    body,
                    ExpressionStatement(increment, line)
                ),
                line
            )

        if (condition == null)
            condition = LiteralExpression(true, line)
        body = WhileStatement(condition, body, line)

        if (initializer != null)
            body = BlockStatement(listOf(initializer, body), line)

        return body
    }

    private fun returnStatement(): Statement {
        val keyword = previous!!

        var value: Expression? = null
        if (!check(TokenType.SEMICOLON))
            value = expression()

        consume(TokenType.SEMICOLON, "Expected \";\" after return value")
        return ReturnStatement(keyword, value)
    }

    private fun statement(): Statement {
        if (match(TokenType.FOR))
            return forStatement()
        if (match(TokenType.IF))
            return ifStatement()
        if (match(TokenType.WHILE))
            return whileStatement()
        if (match(TokenType.RETURN))
            return returnStatement()
        if (match(TokenType.COLON))
            return printStatement()
        if (match(TokenType.LEFT_BRACE)) {
            val line = previous!!.line

            return BlockStatement(block(), line)
        }

        return expressionStatement()
    }

    private fun variableDeclaration(): Statement {
        val name = consume(TokenType.IDENTIFIER, "Expected variable name")

        var initializer: Expression? = null
        val type: ExpressionType

        if (match(TokenType.EQUAL)) {
            initializer = expression()
            type = initializer.type
        } else if (match(TokenType.COLON)) {
            type = parseTypeName()
        } else throw errorAt(previous!!, "Can't infer variable type from nothing")

        consume(TokenType.SEMICOLON, "Expected \";\" after variable declaration")

        val local = Local(
            environment.size,
            type,
            initializer,
            false
        )
        environment[name.content] = local

        return VariableDeclarationStatement(name, local)
    }

    private fun parseTypeName(): ExpressionType {
        val typeName = consume(TokenType.IDENTIFIER, "Expected variable type name")
        return NAME_TO_TYPE_MAP[typeName.content]!!
    }

    private fun functionDeclaration(): Statement {
        val name = consume(TokenType.IDENTIFIER, "Expected function name")

        val previous = environment
        environment = Environment(environment)

        consume(TokenType.LEFT_PAREN, "Expect \"(\" after function name")
        val parameters = mutableListOf<Pair<Token, ExpressionType>>()
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                val parameterName = consume(TokenType.IDENTIFIER, "Expected parameter name")
                consume(TokenType.COLON, "Expected \":\" after parameter name")
                val type = parseTypeName()

                environment[parameterName.content] = Local(
                    environment.size,
                    type,
                    null,
                    false
                )

                parameters.add(
                    parameterName to type
                )
            } while (match(TokenType.COMMA))
        }
        consume(TokenType.RIGHT_PAREN, "Expected \")\" after parameters")

        val returnType: ExpressionType? = if (match(TokenType.COLON))
            parseTypeName()
        else null

        consume(TokenType.LEFT_BRACE, "Expect \"{\" before function body")

        val statements = mutableListOf<Statement>()
        while (!check(TokenType.RIGHT_BRACE) && !atEnd) {
            statements.add(declaration())
        }

        environment = previous

        consume(TokenType.RIGHT_BRACE, "Expected \"}\" after block")

        val index = globals.size
        val local = Local(
            index,
            ExpressionType.OBJECT,
            null,
            true
        )

        if (name.content == "main")
            local.isUsed = true

        globals[name.content] = local

        return FunctionStatement(name, parameters, returnType, statements, index)
    }

    private fun synchronize() {
        advance()

        while (!atEnd) {
            if (previous!!.type === TokenType.SEMICOLON) return

            when (current!!.type) {
                TokenType.CLASS,
                TokenType.FN,
                TokenType.LET,
                TokenType.FOR,
                TokenType.IF,
                TokenType.WHILE,
                TokenType.COLON,
                TokenType.RETURN,
                -> return

                else -> advance()
            }
        }
    }

    private fun declaration(): Statement {
        try {
            if (match(TokenType.FN))
                return functionDeclaration()
            if (match(TokenType.LET))
                return variableDeclaration()

            return statement()
        } catch (e: IllegalStateException) {
            synchronize()

            throw e
        }
    }

    fun parse(): List<Statement> {
        val statements = mutableListOf<Statement>()
        while (!atEnd)
            statements.add(declaration())

        return statements.toList()
    }
}