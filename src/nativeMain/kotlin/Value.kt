sealed class Value<T> {
    abstract val value: T

    override fun toString(): String {
        return value.toString()
    }
}

class BooleanValue(override var value: Boolean): Value<Boolean>() {
    operator fun not() = BooleanValue(!value)
}

sealed class NumberValue<T: Number>: Value<T>() {
    abstract operator fun times(other: NumberValue<*>): NumberValue<T>
    abstract operator fun div(other: NumberValue<*>): NumberValue<T>
    abstract operator fun plus(other: NumberValue<*>): NumberValue<T>
    abstract operator fun unaryMinus(): NumberValue<T>
    abstract operator fun compareTo(rightValue: NumberValue<*>): Int

    operator fun minus(other: NumberValue<*>) = plus(-other)
}

class DoubleValue(override val value: Double): NumberValue<Double>() {
    override operator fun times(other: NumberValue<*>) = DoubleValue(value * other.value.toDouble())
    override operator fun div(other: NumberValue<*>) = DoubleValue(value / other.value.toDouble())
    override operator fun plus(other: NumberValue<*>) = DoubleValue(value + other.value.toDouble())
    override operator fun unaryMinus() = DoubleValue(-value)
    override fun compareTo(rightValue: NumberValue<*>): Int = value.compareTo(rightValue.value.toDouble())
}

class IntegerValue(override val value: Int): NumberValue<Int>() {
    override operator fun times(other: NumberValue<*>) = IntegerValue(value * other.value.toInt())
    override operator fun div(other: NumberValue<*>) = IntegerValue(value / other.value.toInt())
    override operator fun plus(other: NumberValue<*>) = IntegerValue(value + other.value.toInt())
    override operator fun unaryMinus() = IntegerValue(-value)
    override fun compareTo(rightValue: NumberValue<*>): Int = value.compareTo(rightValue.value.toInt())
}

class LongValue(override val value: Long): NumberValue<Long>() {
    override operator fun times(other: NumberValue<*>) = LongValue(value * other.value.toLong())
    override operator fun div(other: NumberValue<*>) = LongValue(value / other.value.toLong())
    override operator fun plus(other: NumberValue<*>) = LongValue(value + other.value.toLong())
    override operator fun unaryMinus() = LongValue(-value)
    override fun compareTo(rightValue: NumberValue<*>): Int = value.compareTo(rightValue.value.toLong())
}

class FloatValue(override val value: Float): NumberValue<Float>() {
    override operator fun times(other: NumberValue<*>) = FloatValue(value * other.value.toFloat())
    override operator fun div(other: NumberValue<*>) = FloatValue(value / other.value.toFloat())
    override operator fun plus(other: NumberValue<*>) = FloatValue(value + other.value.toFloat())
    override operator fun unaryMinus() = FloatValue(-value)
    override fun compareTo(rightValue: NumberValue<*>): Int = value.compareTo(rightValue.value.toFloat())
}

sealed interface Object

data object NullObject: Object {
    override fun toString(): String {
        return "null"
    }
}

data class StringObject(var string: String): Object {
    override fun toString(): String {
        return string
    }
}

data class FunctionObject(var name: String, var code: Chunk, var params: List<Pair<Token, ExpressionType>>, val returnType: ExpressionType?): Object {
    var isMethod: Boolean = false

    override fun toString(): String {
        val unsignedHashCode = this.hashCode().toLong() and 0xffffffffL
        val hashCodeStr = unsignedHashCode.toString(16)

        return "<function@$hashCodeStr; name=\"$name\">"
    }
}

data class ClassObject(var name: String, val methods: MutableMap<String, FunctionObject> = mutableMapOf()): Object {
    override fun toString(): String {
        val unsignedHashCode = this.hashCode().toLong() and 0xffffffffL
        val hashCodeStr = unsignedHashCode.toString(16)

        return "<class@$hashCodeStr; name=\"$name\">"
    }
}

data class InstanceObject(val clazz: ClassObject, val fields: MutableMap<String, Value<*>> = mutableMapOf()): Object {
    init {
        fields.putAll(clazz.methods.mapValues { ObjectValue(it.value) })
    }

    override fun toString(): String {
        val unsignedHashCode = this.hashCode().toLong() and 0xffffffffL
        val hashCodeStr = unsignedHashCode.toString(16)

        return "<instance@$hashCodeStr; class=$clazz>"
    }
}

class ObjectValue<T: Object>(override val value: T): Value<T>()
