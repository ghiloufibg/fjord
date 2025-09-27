package com.fjord.ide.debug;

import com.fjord.error.SourcePosition;
import com.fjord.runtime.Value;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class StackFrame {
    private final String functionName;
    private final File file;
    private final SourcePosition position;
    private final List<String> parameterNames;
    private final List<Value> argumentValues;

    public StackFrame(String functionName, File file, SourcePosition position,
                      List<String> parameterNames, List<Value> argumentValues) {
        this.functionName = functionName;
        this.file = file;
        this.position = position;
        this.parameterNames = parameterNames;
        this.argumentValues = argumentValues;
    }

    public String getFunctionName() { return functionName; }
    public File getFile() { return file; }
    public SourcePosition getPosition() { return position; }
    public List<String> getParameterNames() { return parameterNames; }
    public List<Value> getArgumentValues() { return argumentValues; }

    public String getDisplayName() {
        StringBuilder display = new StringBuilder();
        display.append(functionName);

        if (!parameterNames.isEmpty()) {
            display.append("(");
            for (int i = 0; i < parameterNames.size(); i++) {
                if (i > 0) display.append(", ");
                display.append(parameterNames.get(i));
                if (i < argumentValues.size()) {
                    display.append(" = ").append(argumentValues.get(i).getValue());
                }
            }
            display.append(")");
        } else {
            display.append("()");
        }

        return display.toString();
    }

    public String getLocationString() {
        String fileName = file != null ? file.getName() : "Unknown";
        return String.format("%s:%d", fileName, position.line);
    }

    public String getFullDisplayName() {
        return getDisplayName() + " at " + getLocationString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        StackFrame that = (StackFrame) obj;
        return Objects.equals(functionName, that.functionName) &&
               Objects.equals(file, that.file) &&
               Objects.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionName, file, position);
    }

    @Override
    public String toString() {
        return getFullDisplayName();
    }
}