# Fjord IDE

A simple integrated development environment for the Fjord programming language.

## Features

- **Multi-file editor** with tabbed interface
- **Syntax highlighting** using the Fjord lexer
- **Project explorer** for navigating .fj files
- **Integrated compiler** with multiple execution modes:
  - Tree-walking interpreter (default)
  - Bytecode VM execution
  - Compile-only mode for syntax checking
- **Output console** for program results and error messages
- **Line numbers** and basic editing features

## Running the IDE

### Prerequisites
- Java 21 or later
- Maven 3.6 or later

### From the project root directory:

```bash
# Build the project
mvn clean compile

# Run the IDE
mvn -pl fjord-ide javafx:run

# Or use the convenience script (Windows)
run-ide.bat
```

## Using the IDE

1. **Open a project folder** using File → Open Folder
2. **Create new files** using File → New File or the toolbar
3. **Edit Fjord code** with syntax highlighting in the center editor
4. **Run your code** using the Run button or Run menu:
   - **Run**: Execute with tree-walking interpreter
   - **Compile**: Check syntax and report errors
   - **Run with Bytecode VM**: Execute with faster bytecode VM

## Keyboard Shortcuts

- `Ctrl+N`: New file
- `Ctrl+O`: Open file
- `Ctrl+S`: Save current file
- `Ctrl+Z`: Undo
- `Ctrl+Y`: Redo
- `Ctrl+X`: Cut
- `Ctrl+C`: Copy
- `Ctrl+V`: Paste

## Architecture

The IDE is built using:
- **JavaFX** for the user interface
- **RichTextFX** for the code editor with syntax highlighting
- **Fjord Core** for language compilation and execution
- **Maven** for build management

## Example Fjord Code

```fjord
// Hello World
let message = "Hello, Fjord!"
println(message)

// Function definition
fn factorial(n) =
    if n <= 1 then 1
    else n * factorial(n - 1)

println("5! = " + toString(factorial(5)))
```