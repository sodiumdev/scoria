# Scoria
...a not so lightweight language written in Kotlin/Native.

## Syntax
```rs
fn main() {
    let x = 0;
    x = x + 1;
    
    let y = 3;

    if 25 > 24 {
      : "true...";
    }
  
    if 43 <= 42 {
      y = y + 1; 
      : "this should not work!";
    }

    : y;
    : "hi"; // hi
}
```

This code compiles into the following AST: 
```rs
[
    FunctionStatement(
        name=Token(type=IDENTIFIER, content=main, line=1),
        params=[],
        returnValue=null,
        body=[
            BlockStatement(
                statements=[
                    PrintStatement(
                        expression=
                            LiteralExpression(
                                value=true...,
                                line=8
                            ),
                        line=8)
                ],
                line=7),
            PrintStatement(
                expression=
                    LiteralExpression(
                        value=5,
                        line=3
                    ),
                line=16),
            PrintStatement(
                expression=
                    LiteralExpression(
                        value=4,
                        line=12
                    ),
                line=18),
            PrintStatement(
                expression=
                    LiteralExpression(
                        value=hi,
                        line=19
                    ),
                line=19)
        ],
        index=0)
]
```

Then this AST is parsed and compiled into Scoria Bytecode.
