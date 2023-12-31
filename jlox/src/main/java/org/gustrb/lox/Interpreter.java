package org.gustrb.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>,
        Stmt.Visitor<Void> {
    private final Map<Expr, Integer> locals = new HashMap<>();
    final Environment globals = new Environment();
    private Environment environment = globals;

    public Interpreter() {
        // TODO: Add more functions and add them into their own, stdlib package
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() { return 0; }

            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public String toString() { return "<native fn>"; }
        });
    }

    public void interpret(List<Stmt> statements) {
        try {
            for (final var statement : statements)
                execute(statement);
        } catch(RuntimeError e) {
            Lox.runtimeError(e);
        }
    }

    @Override
    public Object visitLiteralExpr(final Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(final Expr.Logical expr) {
        final var left = evaluate(expr.left);
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitGroupingExpr(final Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitUnaryExpr(final Expr.Unary expr) {
        final var right = evaluate(expr.right);

        switch (expr.operator.type) {
            case BANG:  return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return -(double)right;
        }

        // Unreachable, I hope
        return null;
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr.name, expr);
    }

    private Object lookupVariable(final Token name, final Expr expr) {
        final var distance = locals.get(expr);
        if (distance != null) {
            return environment.getAt(distance, name.lexeme);
        }

        return globals.get(name);
    }

    private void checkNumberOperand(final Token operator, final Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(final Token operator, final Object left, final Object right) {
        if (left instanceof Double && right instanceof Double) return;

        throw new RuntimeError(operator, "Operands must be numbers");
    }

    @Override
    public Object visitBinaryExpr(final Expr.Binary expr) {
        final var left = evaluate(expr.left);
        final var right = evaluate(expr.right);

        switch (expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;

            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;

            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left <= (double) right;

            case BANG_EQUAL: return !isEqual(left, right);
            case EQUAL_EQUAL: return isEqual(left, right);

            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left - (double) right;

            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double) left / (double) right;

            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double) left * (double) right;

            case PLUS:
                // We are adding 2 numbers
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                }

                // If both of them are strings, concatenate em
                if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }

                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings");
        }

        return null;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        final var callee = evaluate(expr.callee);
        final List<Object> arguments = new ArrayList<>();

        for (final var argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        LoxCallable function = (LoxCallable) callee;

        if (arguments.size() != function.arity()) {
            throw new RuntimeError(
                    expr.paren,
                    "Expected " + function.arity() + " arguments but got " + arguments.size() + "."
            );
        }

        return function.call(this, arguments);
    }

    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;

        if (stmt.superclass != null) {
            superclass = evaluate(stmt.superclass);
            if (!(superclass instanceof LoxClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class");
            }
        }

        environment.define(stmt.name.lexeme, null);

        if (stmt.superclass != null) {
            environment = new Environment(environment);
            environment.define("super", superclass);
        }

        final Map<String, LoxFunction> methods = new HashMap<>();
        for (final var method : stmt.methods) {
            final var function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        final var klass = new LoxClass(stmt.name.lexeme, (LoxClass) superclass, methods);

        if (superclass != null) {
            environment = environment.enclosing;
        }

        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        final var function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }

        return null;
    }

    @Override
    public Void visitWhileStmt(final Stmt.While stmt) {
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        final var value = evaluate(stmt.expression);
        System.out.println(stringfy(value));
        return null;
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) value = evaluate(stmt.value);

        // Abusing java's exception to unwind the stack of visitors :0
        throw new Return(value);
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        final var value = stmt.initializer != null
                ? evaluate(stmt.initializer)
                : null;
        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        final var val = evaluate(expr.value);
        final Integer distance = locals.get(expr);

        if (distance != null) {
            environment.assignAt(distance, expr.name, val);
        } else {
            globals.assign(expr.name, val);
        }

        return val;
    }

    @Override
    public Object visitGetExpr(final Expr.Get expr) {
        final var obj = evaluate(expr.object);
        if (obj instanceof LoxInstance) {
            return ((LoxInstance) obj).get(expr.name);
        }

        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Object visitSetExpr(final Expr.Set expr) {
        final var obj = evaluate(expr.object);

        if (!(obj instanceof LoxInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields");
        }

        final var value = evaluate(expr.value);
        ((LoxInstance) obj).set(expr.name, value);
        return value;
    }

    @Override
    public Object visitThisExpr(final Expr.This expr) {
        return lookupVariable(expr.keyword, expr);
    }

    @Override
    public Object visitSuperExpr(final Expr.Super expr) {
        int distance = locals.get(expr);
        final var superclass = (LoxClass) environment.getAt(distance, "super");
        final var object = (LoxInstance) environment.getAt(distance -  1, "this");
        final var method = superclass.findMethod(expr.method.lexeme);

        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
        }

        return method.bind(object);
    }

    private boolean isTruthy(final Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;

        return true;
    }

    private boolean isEqual(final Object a, final Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;

        return a.equals(b);
    }

    private String stringfy(final Object object) {
        if (object == null) return "nil";
        if (object instanceof Double) {
            var text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }
        return object.toString();
    }

    private Object evaluate(final Expr expr) {
        return expr.accept(this);
    }

    private void execute(final Stmt stmt) {
        stmt.accept(this);
    }

    @Override
    public Void visitBlockStmt(final Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    public void executeBlock(final List<Stmt> statements, final Environment environment) {
        final var previous = this.environment;
        try {
            this.environment = environment;
            for (final var stmt : statements)
                execute(stmt);
        } finally {
            this.environment = previous;
        }
    }

    public void resolve(final Expr expr, final int depth) {
        locals.put(expr, depth);
    }
}
