interface ExpressionVisitor<R> {
    fun visit(call: CallExpression): R
    fun visit(unary: UnaryExpression): R
    fun visit(logical: LogicalExpression): R
    fun visit(binary: BinaryExpression): R
    fun visit(getVariable: GetVariableExpression): R
    fun visit(assignVariable: AssignVariableExpression): R
    fun visit(grouping: GroupingExpression): R
    fun visit(literal: LiteralExpression): R
}

enum class ExpressionType {
    BOOLEAN,
    INT,
    FLOAT,
    DOUBLE,
    LONG,
    OBJECT;

    companion object {
        operator fun get(value: Any?): ExpressionType {
            return when (value) {
                is Boolean -> BOOLEAN
                is Int -> INT
                is Float -> FLOAT
                is Double -> DOUBLE
                is Long -> LONG
                is Object -> OBJECT
                else -> throw IllegalArgumentException()
            }
        }
    }

    val isNumber: Boolean
        get() = when (this) {
            BOOLEAN -> false
            INT -> true
            FLOAT -> true
            DOUBLE -> true
            LONG -> true
            OBJECT -> false
        }

    fun or(type: ExpressionType): ExpressionType {
        if (type.ordinal > ordinal)
            return type

        return this
    }
}

sealed interface Expression {
    val hasCall: Boolean
    val type: ExpressionType
    val value: Any?
        get() = null
    val numberValue: NumberValue<*>
        get() = when (type) {
            ExpressionType.INT -> IntegerValue(value as Int)
            ExpressionType.FLOAT -> FloatValue(value as Float)
            ExpressionType.DOUBLE -> DoubleValue(value as Double)
            ExpressionType.LONG -> LongValue(value as Long)
            ExpressionType.BOOLEAN -> error("Can't convert a boolean into a number!")
            ExpressionType.OBJECT -> error("Can't convert an object into a number!")
        }
    val vmValue: Value<*>
        get() = when (type) {
            ExpressionType.INT -> IntegerValue(value as Int)
            ExpressionType.FLOAT -> FloatValue(value as Float)
            ExpressionType.DOUBLE -> DoubleValue(value as Double)
            ExpressionType.LONG -> LongValue(value as Long)
            ExpressionType.BOOLEAN -> BooleanValue(value as Boolean)
            ExpressionType.OBJECT -> ObjectValue(value as Object)
        }

    fun <R> accept(visitor: ExpressionVisitor<R>): R
}

data class CallExpression(val callee: Expression, val paren: Token, var arguments: List<Expression>): Expression {
    override var type: ExpressionType = ExpressionType.OBJECT
    override val hasCall: Boolean = true

    override fun <R> accept(visitor: ExpressionVisitor<R>): R = visitor.visit(this)
}

data class UnaryExpression(var right: Expression, val operator: Token): Expression {
    override val type
        get() = right.type
    override val value: Any?
        get() {
            right.value ?: return null

            return when (operator.type) {
                TokenType.MINUS -> (-right.numberValue).value
                TokenType.BANG -> when (right.type) {
                    ExpressionType.INT -> error("Can't apply the boolean not operation to an int!")
                    ExpressionType.FLOAT -> error("Can't apply the boolean not operation to a float!")
                    ExpressionType.DOUBLE -> error("Can't apply the boolean not operation to a double!")
                    ExpressionType.LONG -> error("Can't apply the boolean not operation to a long!")
                    ExpressionType.BOOLEAN -> !(right.value as Boolean)
                    ExpressionType.OBJECT -> error("Can't do unary expressions with objects!")
                }

                else -> {}
            }
        }

    override val hasCall: Boolean
        get() = right.hasCall

    override fun <R> accept(visitor: ExpressionVisitor<R>): R = visitor.visit(this)
}

data class LogicalExpression(var left: Expression, var right: Expression, val operator: Token): Expression {
    override val type: ExpressionType = ExpressionType.BOOLEAN
    override val value: Any?
        get() {
            left.value ?: return null
            right.value ?: return null

            when (left.type) {
                ExpressionType.INT -> error("Can't apply logical operations to an int!")
                ExpressionType.FLOAT -> error("Can't apply logical operations to a float!")
                ExpressionType.DOUBLE -> error("Can't apply logical operations to a double!")
                ExpressionType.LONG -> error("Can't apply logical operations to a long!")
                ExpressionType.OBJECT -> error("Can't do logical operations with objects!")
                ExpressionType.BOOLEAN -> {}
            }

            when (right.type) {
                ExpressionType.INT -> error("Can't apply logical operations to an int!")
                ExpressionType.FLOAT -> error("Can't apply logical operations to a float!")
                ExpressionType.DOUBLE -> error("Can't apply logical operations to a double!")
                ExpressionType.LONG -> error("Can't apply logical operations to a long!")
                ExpressionType.OBJECT -> error("Can't do logical operations with objects!")
                ExpressionType.BOOLEAN -> !(right.value as Boolean)
            }

            return when (operator.type) {
                TokenType.AND -> left.value as Boolean && right.value as Boolean
                TokenType.OR -> left.value as Boolean || right.value as Boolean

                else -> {}
            }
        }

    override val hasCall: Boolean
        get() = left.hasCall || right.hasCall

    override fun <R> accept(visitor: ExpressionVisitor<R>): R = visitor.visit(this)
}

data class BinaryExpression(var left: Expression, var right: Expression, val operator: Token): Expression {
    override val type
        get() = when (operator.type) {
            TokenType.PLUS,
            TokenType.MINUS,
            TokenType.STAR,
            TokenType.SLASH -> left.type.or(right.type)

            TokenType.GREATER,
            TokenType.GREATER_EQUAL,
            TokenType.LESS,
            TokenType.LESS_EQUAL -> ExpressionType.BOOLEAN

            else -> throw IllegalStateException()
        }
    override val value: Any?
        get() {
            left.value ?: return null
            right.value ?: return null

            val leftValue: NumberValue<*> = left.numberValue
            val rightValue: NumberValue<*> = right.numberValue

            return when (operator.type) {
                TokenType.PLUS -> (leftValue + rightValue).value
                TokenType.MINUS -> (leftValue - rightValue).value
                TokenType.STAR -> (leftValue * rightValue).value
                TokenType.SLASH -> (leftValue / rightValue).value

                TokenType.GREATER -> leftValue > rightValue
                TokenType.GREATER_EQUAL -> leftValue >= rightValue
                TokenType.LESS -> leftValue < rightValue
                TokenType.LESS_EQUAL -> leftValue <= rightValue

                else -> {}
            }
        }

    override val hasCall: Boolean
        get() = left.hasCall || right.hasCall

    override fun <R> accept(visitor: ExpressionVisitor<R>): R = visitor.visit(this)
}

data class GetVariableExpression(val name: Token, val local: Local): Expression {
    override val type: ExpressionType = local.type

    override val hasCall: Boolean
        get() = local.value?.hasCall ?: false

    val index = local.index
    val isGlobal = local.isGlobal

    override fun <R> accept(visitor: ExpressionVisitor<R>): R = visitor.visit(this)
}

data class AssignVariableExpression(val name: Token, var assigned: Expression, val local: Local): Expression {
    override val type: ExpressionType
        get() = assigned.type
    override val value: Any?
        get() = assigned.value

    override val hasCall: Boolean
        get() = assigned.hasCall

    val index = local.index

    override fun <R> accept(visitor: ExpressionVisitor<R>): R = visitor.visit(this)
}

data class GroupingExpression(var groupedValue: Expression, val line: Int): Expression {
    override val type
        get() = groupedValue.type
    override val value: Any?
        get() = groupedValue.value

    override val hasCall: Boolean
        get() = groupedValue.hasCall

    override fun <R> accept(visitor: ExpressionVisitor<R>): R = visitor.visit(this)
}

data class LiteralExpression(override val value: Any?, val line: Int): Expression {
    override val type = when (value) {
        is Boolean -> ExpressionType.BOOLEAN
        is Int -> ExpressionType.INT
        is Float -> ExpressionType.FLOAT
        is Double -> ExpressionType.DOUBLE
        is Long -> ExpressionType.LONG
        is Object -> ExpressionType.OBJECT

        else -> error("Invalid literal value!")
    }

    override val hasCall: Boolean
        get() = false

    override fun <R> accept(visitor: ExpressionVisitor<R>): R = visitor.visit(this)

    fun toValue(): Value<out Any?> = when (type) {
        ExpressionType.BOOLEAN -> BooleanValue(value!! as Boolean)
        ExpressionType.INT -> IntegerValue(value!! as Int)
        ExpressionType.FLOAT -> FloatValue(value!! as Float)
        ExpressionType.DOUBLE -> DoubleValue(value!! as Double)
        ExpressionType.LONG -> LongValue(value!! as Long)
        ExpressionType.OBJECT -> ObjectValue(value?.let { it as Object } ?: NullObject)
    }
}
