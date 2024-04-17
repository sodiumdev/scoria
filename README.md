# Scoria
## Please ⭐ this project if you like it!
## How does it work?
```rs
fn main() {
    : "hi" // hi
}
```

Take the code above, this code compiles and is optimized into the following AST: 
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

## Syntax Examples

```rs
class Example {
  let it = "hi"

  fn getIt(): any {
    return this.it // you have to use this to get fields!
  }
}

fn getIt(): any {
  return "it"
}

fn main() {
    // loops
    let a = 0
    while (a < 25) {
      a += 1
    }

    for let b = 0; b < 25; b += 1 {
      : b
    }

    // if statements
    if 25 < 26 {
      : "yep!"
    }

    // printing
    : a + 34

    let it = getIt() // function calls
    : it

    let example = Example() // classes and instances
    : example.getIt()
}
```
