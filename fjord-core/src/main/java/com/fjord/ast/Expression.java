package com.fjord.ast;

import com.fjord.runtime.Environment;
import com.fjord.runtime.Value;

/**
 * Base interface for all expressions in the Fjord abstract syntax tree.
 * Every expression can be evaluated against an environment to produce a
 * {@link Value}. Statements such as function definitions and let
 * declarations are represented as expressions returning the unit value.
 */
@FunctionalInterface
public interface Expression {
    /**
     * Evaluates this expression in the given environment.
     *
     * @param env the environment providing variable and function bindings
     * @return the resulting {@link Value} of the evaluation
     */
    Value evaluate(Environment env);
}