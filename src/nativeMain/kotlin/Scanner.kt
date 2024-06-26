enum class TokenType {
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT,
    SEMICOLON, COLON,

    MINUS, MINUS_EQUAL,
    PLUS, PLUS_EQUAL,
    SLASH, SLASH_EQUAL,
    STAR, STAR_EQUAL,
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    IDENTIFIER, STRING, FLOAT, INTEGER,

    AND, CLASS, ELSE, FALSE,
    FOR, FN, IF, NULL, OR,
    RETURN, SUPER,
    TRUE, LET, WHILE,

    ERROR, END_OF_FILE;

    val couldStartExpression: Boolean
        get() = when (this) {
            LEFT_PAREN -> true
            RIGHT_PAREN -> false
            LEFT_BRACE -> true
            RIGHT_BRACE -> false
            COMMA -> false
            DOT -> false
            MINUS -> true
            PLUS -> true
            SEMICOLON -> false
            SLASH -> false
            STAR -> false
            COLON -> false
            BANG -> true
            BANG_EQUAL -> false
            EQUAL -> false
            EQUAL_EQUAL -> false
            GREATER -> false
            GREATER_EQUAL -> false
            LESS -> false
            LESS_EQUAL -> false
            IDENTIFIER -> true
            STRING -> true
            FLOAT -> true
            INTEGER -> true
            AND -> false
            CLASS -> false
            ELSE -> false
            FALSE -> true
            FOR -> false
            FN -> false
            IF -> false
            NULL -> true
            OR -> false
            RETURN -> false
            SUPER -> false
            TRUE -> true
            LET -> false
            WHILE -> false
            ERROR -> false
            END_OF_FILE -> false
            MINUS_EQUAL -> false
            PLUS_EQUAL -> false
            SLASH_EQUAL -> false
            STAR_EQUAL -> false
        }
}

data class Token(val type: TokenType, val content: String, val line: Int) {
    val couldStartExpression: Boolean = type.couldStartExpression
}

class Scanner(private val source: String) {
    companion object {
        val KEYWORDS = mapOf(
            "and" to TokenType.AND,
            "class" to TokenType.CLASS,
            "else" to TokenType.ELSE,
            "if" to TokenType.IF,
            "null" to TokenType.NULL,
            "or" to TokenType.OR,
            "return" to TokenType.RETURN,
            "super" to TokenType.SUPER,
            "let" to TokenType.LET,
            "while" to TokenType.WHILE,
            "else" to TokenType.ELSE,
            "for" to TokenType.FOR,
            "false" to TokenType.FALSE,
            "fn" to TokenType.FN,
            "true" to TokenType.TRUE
        )
    }

    private var start = 0
    private var current = 0

    private var line = 1

    private val currentChar: Char
        get() = if (atEnd) '\u0000' else source[current]

    private val nextChar: Char
        get() = if (atEnd) '\u0000' else source[current + 1]

    private val atEnd: Boolean
        get() = current >= source.length

    fun scan(): Token {
        skipWhitespace()

        start = current

        if (atEnd)
            return makeToken(TokenType.END_OF_FILE)

        val char = advance()

        if (char.isDigit())
            return makeNumberToken()
        if (char.isLetter())
            return makeIdentifierToken()

        when (char) {
            '(' -> return makeToken(TokenType.LEFT_PAREN)
            ')' -> return makeToken(TokenType.RIGHT_PAREN)
            '{' -> return makeToken(TokenType.LEFT_BRACE)
            '}' -> return makeToken(TokenType.RIGHT_BRACE)
            ';' -> return makeToken(TokenType.SEMICOLON)
            ':' -> return makeToken(TokenType.COLON)
            ',' -> return makeToken(TokenType.COMMA)
            '.' -> return makeToken(TokenType.DOT)

            '-' -> return makeToken(
                if (match('=')) TokenType.MINUS_EQUAL else TokenType.MINUS
            )

            '+' -> return makeToken(
                if (match('=')) TokenType.PLUS_EQUAL else TokenType.PLUS
            )

            '/' -> return makeToken(
                if (match('=')) TokenType.SLASH_EQUAL else TokenType.SLASH
            )

            '*' -> return makeToken(
                if (match('=')) TokenType.STAR_EQUAL else TokenType.STAR
            )

            '!' -> return makeToken(
                if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG
            )

            '=' -> return makeToken(
                if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL
            )

            '<' -> return makeToken(
                if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS
            )

            '>' -> return makeToken(
                if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER
            )

            '"' -> return makeStringToken()

            else -> return makeErrorToken("Unexpected character")
        }
    }

    private fun skipWhitespace() {
        while (true) {
            when (currentChar) {
                ' ', '\r', '\t' -> advance()
                '\n' -> {
                    line++
                    advance()
                }

                '/' -> if (nextChar == '/') {
                    while (currentChar != '\n' && !atEnd)
                        advance()
                } else return

                else -> return
            }
        }
    }

    private fun match(char: Char): Boolean {
        if (atEnd || currentChar != char)
            return false

        current++
        return true
    }

    private fun advance() = source[current++]

    private fun makeIdentifierToken(): Token {
        while (currentChar.isLetter() || currentChar.isDigit())
            advance()

        return makeToken(
            KEYWORDS[source.substring(start, current)] ?: TokenType.IDENTIFIER
        )
    }

    private fun makeNumberToken(): Token {
        while (currentChar.isDigit())
            advance()

        if (currentChar == '.' && nextChar.isDigit()) {
            advance()

            while (currentChar.isDigit())
                advance()

            return makeToken(TokenType.FLOAT)
        }

        return makeToken(TokenType.INTEGER)
    }

    private fun makeStringToken(): Token {
        while (currentChar != '"' && !atEnd) {
            if (currentChar == '\n')
                line++

            advance()
        }

        if (atEnd)
            return makeErrorToken("Unterminated string")

        advance()
        return makeToken(TokenType.STRING)
    }

    private fun makeToken(type: TokenType) = Token(
        type,
        source.substring(start, current),
        line
    )

    private fun makeErrorToken(message: String) = Token(
        TokenType.ERROR,
        message,
        line
    )
}