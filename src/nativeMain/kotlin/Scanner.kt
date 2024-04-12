enum class TokenType {
    LEFT_PAREN, RIGHT_PAREN,
    LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS,
    SEMICOLON, SLASH, STAR,
    COLON,

    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    IDENTIFIER, STRING, FLOAT, INTEGER,

    AND, CLASS, ELSE, FALSE,
    FOR, FN, IF, NULL, OR,
    RETURN, SUPER, THIS,
    TRUE, LET, WHILE,

    ERROR, END_OF_FILE
}

data class Token(val type: TokenType, val content: String, val line: Int)

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
            "this" to TokenType.THIS,
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
            '-' -> return makeToken(TokenType.MINUS)
            '+' -> return makeToken(TokenType.PLUS)
            '/' -> return makeToken(TokenType.SLASH)
            '*' -> return makeToken(TokenType.STAR)

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