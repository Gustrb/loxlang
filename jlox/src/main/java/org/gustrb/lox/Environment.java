package org.gustrb.lox;

import java.util.HashMap;
import java.util.Map;

public class Environment {
    private final Map<String, Object> values = new HashMap<>();
    public final Environment enclosing;

    public Environment() {
        this.enclosing = null;
    }

    public Environment(final Environment enclosing) {
        this.enclosing = enclosing;
    }

    public void define(final String name, final Object value) {
        values.put(name, value);
    }

    public void assign(final Token name, final Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    public Object get(final Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        // Walk recursively the nodes to find the inner most scope containing
        // that variable declaration
        if (enclosing != null) return enclosing.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
