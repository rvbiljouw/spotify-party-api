package uk.bipush.party.util

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import io.ebean.Expr
import io.ebean.Expression

/**
 * A serializable filter that we can translate into a safe database query.
 * Stops us from writing loads of endpoints for all the different types of filters people want.

 * @author rvbiljouw
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Filter {

    enum class Type {
        NOT_EQUALS, EQUALS, STARTS_WITH, ENDS_WITH, CONTAINS, GREATER_THAN, LESS_THAN, GREATER_THAN_EQ, LESS_THAN_EQ, OR, AND, IN
    }

    var type: Type? = null
    var fieldName: String? = null
    var value: Any? = null
    var children: List<Filter> = listOf()

    override fun toString(): String {
        return "Filter{" +
                "type=" + type +
                ", fieldName='" + fieldName + '\'' +
                ", value='" + value + '\'' +
                '}'
    }

    @JsonIgnore
    fun compile(): Expression {
        when (type) {

            Type.NOT_EQUALS -> return Expr.ne(fieldName, value)

            Type.EQUALS -> return Expr.eq(fieldName, value)

            Type.STARTS_WITH -> return Expr.startsWith(fieldName, "${value!!}%")

            Type.ENDS_WITH -> return Expr.endsWith(fieldName, "%${value!!}")

            Type.CONTAINS -> return Expr.contains(fieldName, "%$value%")

            Type.GREATER_THAN -> return Expr.gt(fieldName, value)

            Type.LESS_THAN -> return Expr.lt(fieldName, value)

            Type.GREATER_THAN_EQ -> return Expr.ge(fieldName, value)

            Type.LESS_THAN_EQ -> return Expr.le(fieldName, value)

            Type.OR -> {
                val (filterA, filterB) = children.map { it.compile() }.take(2)
                return Expr.or(filterA, filterB)
            }

            Type.AND -> {
                val (filterA, filterB) = children.map { it.compile() }.take(2)
                return Expr.or(filterA, filterB)
            }

            Type.IN -> {
                val values = value as List<Any>

                if (values.isEmpty()) {
                    throw IllegalArgumentException("Filter of type 'IN' must have values passed")
                }

                return Expr.`in`(fieldName, values)
            }

            else -> throw IllegalArgumentException(String.format("Unsupported expression type: %s", type))
        }
    }
}