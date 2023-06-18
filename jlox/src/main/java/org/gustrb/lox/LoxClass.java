package org.gustrb.lox;

import java.util.List;
import java.util.Map;

public class LoxClass implements LoxCallable {
    final String name;
    private final Map<String, LoxFunction> methods;
    private final LoxClass superclass;

    public LoxClass(final String name, final LoxClass superclass, final Map<String, LoxFunction> methods) {
        this.name = name;
        this.methods = methods;
        this.superclass = superclass;
    }

    public LoxFunction findMethod(final String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        if (superclass != null) {
            return superclass.findMethod(name);
        }

        return null;
    }

    @Override
    public Object call(final Interpreter interpreter, final List<Object> arguments) {
        final var instance = new LoxInstance(this);

        final var constructor = findMethod("init");
        if (constructor != null) {
            constructor.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    @Override
    public int arity() {
        final var constructor = findMethod("init");
        if (constructor == null) {
            return 0;
        }
        return constructor.arity();
    }

    @Override
    public String toString() {
        return name;
    }
}
