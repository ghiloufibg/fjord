# Fjord Programming Language

A modern functional programming language for the JVM with a focus on simplicity, performance, and educational value.

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Java Version](https://img.shields.io/badge/java-21-blue)
![License](https://img.shields.io/badge/license-MIT-green)

## ✨ Features

### 🚀 **Multiple Execution Modes**

- **Tree-walking Interpreter**: Traditional AST interpretation for development
- **Bytecode VM**: High-performance stack-based virtual machine (5-10x faster)
- **Compile-only Mode**: Static analysis and type checking without execution

### 🔍 **Advanced Error Reporting**

- Precise source location tracking (line and column numbers)
- Categorized error types (Lexical, Syntax, Semantic, Runtime)
- User-friendly error messages with context

### 🏷️ **Rich Type System**

- Static type checking with inference
- Function type signatures
- Type compatibility checking
- Numeric promotion (int → float)

### 📚 **Extensible Standard Library**

- Built-in functions: `println`, `toString`, `toInt`, `toFloat`, `abs`, `sqrt`
- Type-safe function registry
- Easy addition of custom built-in functions

### 🏗️ **Professional Architecture**

- Clean separation of compiler phases
- Visitor pattern for extensible AST operations
- Comprehensive semantic analysis
- Symbol table with scope management

## 📦 Installation

### Prerequisites

- Java 21 or higher
- Maven 3.6 or higher

### Build from Source

```bash
git clone <repository-url>
cd fjord
mvn clean compile
```

### Run Tests

```bash
mvn test
```

## 🚀 Quick Start

### Basic Usage

```bash
# Tree-walking interpreter (compatible mode)
java -cp target/classes com.fjord.Fjord program.fj

# High-performance bytecode VM (recommended)
java -cp target/classes com.fjord.Fjord --bytecode program.fj

# Compile-time checking only
java -cp target/classes com.fjord.Fjord --compile program.fj

# Verbose debugging information
java -cp target/classes com.fjord.Fjord --verbose program.fj
```

### Command Line Options

```
USAGE:
    fjord [OPTIONS] <source.fj>

OPTIONS:
    -h, --help       Show help message
    -v, --verbose    Enable verbose output
    -b, --bytecode   Use bytecode VM execution (faster)
    -c, --compile    Compile only, don't execute

EXAMPLES:
    fjord hello.fj                 # Tree-walking interpreter
    fjord --bytecode hello.fj       # Bytecode VM (recommended)
    fjord --verbose hello.fj        # Show detailed execution info
    fjord --compile hello.fj        # Check syntax and types only
```

## 📖 Language Guide

### Variables and Types

```fjord
// Immutable variable declarations
let x = 42              // Integer
let pi = 3.14159        // Float
let name = "Fjord"      // String
let flag = true         // Boolean
let nothing = unit      // Unit type
```

### Functions

```fjord
// Function definitions with type annotations
fn add(a: Int, b: Int): Int = a + b

// Functions are first-class values
fn greet(name: String): String = "Hello, " + name

// Recursive functions
fn factorial(n: Int): Int =
  if n <= 1 then 1
  else n * factorial(n - 1)
```

### Control Flow

```fjord
// Conditional expressions
let result = if x > 0 then "positive" else "non-positive"

// Multi-line conditionals
let category = if score >= 90 then "A"
              else if score >= 80 then "B"
              else if score >= 70 then "C"
              else "F"
```

### Block Expressions

```fjord
// Block expressions with local scope
let result = {
  let x = 10
  let y = 20
  x + y  // Last expression is the block's value
}
```

### Built-in Functions

```fjord
println("Hello, World!")           // Print with newline
let str = toString(42)             // Convert to string
let num = toInt("123")             // Parse integer
let float_val = toFloat("3.14")    // Parse float
let absolute = abs(-42)            // Absolute value
let root = sqrt(16.0)              // Square root
```

### Entry Point

```fjord
// Optional main function
fn main(): Unit = {
  println("Program started!")
  let result = add(5, 3)
  println("5 + 3 = " + toString(result))
}
```

## 🏗️ Architecture

Fjord follows a traditional multi-phase compiler design:

```
Source Code → Lexical Analysis → Syntax Analysis → Semantic Analysis → Code Generation → Execution
```

### Core Components

- **📁 `com.fjord.error`** - Comprehensive error reporting system
- **🏷️ `com.fjord.types`** - Rich type system with inference
- **🔤 `com.fjord.lex`** - Enhanced lexical analysis with position tracking
- **🌳 `com.fjord.ast`** - AST with visitor pattern support
- **🔍 `com.fjord.analyzer`** - Semantic analysis and symbol tables
- **⚡ `com.fjord.bytecode`** - VM instructions and code generation
- **🏭 `com.fjord.compiler`** - Compilation pipeline orchestration
- **🔧 `com.fjord.runtime`** - Enhanced runtime with built-ins

## 🎯 Performance

| Execution Mode | Performance  | Use Case                         |
|----------------|--------------|----------------------------------|
| Tree-walking   | Baseline     | Development, debugging           |
| Bytecode VM    | 5-10x faster | Production, performance-critical |
| Compile-only   | N/A          | CI/CD, type checking             |

## 🧪 Examples

### Hello World

```fjord
println("Hello, Fjord!")
```

### Fibonacci Sequence

```fjord
fn fibonacci(n: Int): Int =
  if n <= 1 then n
  else fibonacci(n - 1) + fibonacci(n - 2)

fn main(): Unit = {
  let result = fibonacci(10)
  println("Fibonacci(10) = " + toString(result))
}
```

### Mathematical Operations

```fjord
fn circleArea(radius: Float): Float = 3.14159 * radius * radius

fn main(): Unit = {
  let radius = 5.0
  let area = circleArea(radius)
  println("Circle area: " + toString(area))
}
```

### String Processing

```fjord
fn processName(firstName: String, lastName: String): String =
  "Mr./Ms. " + firstName + " " + lastName

fn main(): Unit = {
  let fullName = processName("John", "Doe")
  println(fullName)
}
```

## 🛠️ Development

### Project Structure

```
fjord/
├── src/main/java/com/fjord/
│   ├── ast/           # Abstract Syntax Tree nodes
│   ├── analyzer/      # Semantic analysis
│   ├── bytecode/      # VM and bytecode generation
│   ├── compiler/      # Compilation pipeline
│   ├── error/         # Error reporting system
│   ├── lex/           # Lexical analysis
│   ├── parser/        # Syntax analysis
│   ├── runtime/       # Runtime system and built-ins
│   └── types/         # Type system
├── src/test/java/     # Unit tests
└── docs/              # Documentation
```

### Adding Built-in Functions

```java
// 1. Implement the Callable interface
public class MyFunction implements Callable {
    @Override
    public Value call(List<Value> arguments) {
        // Your implementation
        return new StringValue("result");
    }

    @Override
    public Object getValue() {
        return this;
    }
}

// 2. Register in BuiltinRegistry
registry.

register("myFunction",new MyFunction(),
                 FunctionType.

unary(Type.STRING, Type.STRING),
                 "Description of my function");
```

### Running Tests

```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=ParserTest

# Run with verbose output
mvn test -Dtest=InterpreterTest -X
```

### Building Documentation

```bash
# Generate Javadoc
mvn javadoc:javadoc

# View documentation
open target/site/apidocs/index.html
```

## 📚 Educational Value

Fjord is designed to be an excellent learning resource for:

- **Compiler Design**: Clear separation of lexing, parsing, semantic analysis, and code generation
- **Type Systems**: Static type checking with inference
- **Virtual Machines**: Stack-based bytecode execution
- **Language Implementation**: Professional-grade architecture patterns

### Learning Path

1. **Start Simple**: Use tree-walking mode to understand basic interpretation
2. **Add Types**: Explore the type system and semantic analysis
3. **Go Fast**: Learn bytecode generation and VM execution
4. **Extend**: Add new language features and built-in functions

### Development Setup

```bash
# Clone the repository
git clone <repository-url>
cd fjord

# Build the project
mvn clean compile

# Run tests
mvn test

# Create a feature branch
git checkout -b feature/my-feature

# Make your changes and test
mvn test

# Submit a pull request
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- Inspired by functional programming languages like ML and Haskell
- Built on the JVM for excellent performance and portability
- Designed with education and simplicity in mind