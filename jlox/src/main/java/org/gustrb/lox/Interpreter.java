package org.gustrb.lox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>,
        Stmt.Visitor<Void> {
    private Environment environment = new Environment();

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
        return environment.get(expr.name);
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
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        final var value = evaluate(stmt.expression);
        System.out.println(stringfy(value));
        return null;
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
        environment.assign(expr.name, val);
        return val;
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

    private void executeBlock(final List<Stmt> statements, final Environment environment) {
        final var previous = this.environment;
        try {
            this.environment = environment;
            for (final var stmt : statements)
                execute(stmt);
        } finally {
            this.environment = previous;
        }
    }
}
