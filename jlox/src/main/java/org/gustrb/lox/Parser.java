package org.gustrb.lox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {
    private static class ParseError extends RuntimeException {}

    private final List<Token> tokens;
    private int current = 0;

    public Parser(final List<Token> tokens) {
        this.tokens = tokens;
    }

    public List<Stmt> parse() {
        final List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }
    private Expr expression() {
        return assignment();
    }

    private Stmt declaration() {
        try {
            if (match(TokenType.CLASS)) return classDeclaration();
            if (match(TokenType.FUN)) return function("function");
            if (match(TokenType.VAR)) return varDeclaration();
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        final var name = consume(TokenType.IDENTIFIER, "Expect class name.");

        Expr.Variable superClass = null;
        if (match(TokenType.LESS)) {
            consume(TokenType.IDENTIFIER, "Expect superclass name.");
            superClass = new Expr.Variable(previous());
        }

        consume(TokenType.LEFT_BRACE, "Expect '{' before class body");

        final List<Stmt.Function> methods = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd())
            methods.add(function("method"));

        consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");
        return new Stmt.Class(name, superClass, methods);
    }

    private Stmt statement() {
        if (match(TokenType.IF)) return ifStatement();
        if (match(TokenType.FOR)) return forStatement();
        if (match(TokenType.PRINT)) return printStatement();
        if (match(TokenType.WHILE)) return whileStatement();
        if (match(TokenType.RETURN)) return returnStatement();
        if (match(TokenType.LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
    }

    private Stmt ifStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        final var condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'if'.");

        final var thenBranch = statement();
        final var elseBranch = match(TokenType.ELSE)
                ? statement()
                : null;
        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        final var value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        final var keyword = previous();
        final var value = !check(TokenType.SEMICOLON) ? expression() : null;
        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        final var name = consume(TokenType.IDENTIFIER, "Expect variable name.");
        final var initializer = match(TokenType.EQUAL)
                ? expression()
                : null;

        consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        final var condition = expression();
        consume(TokenType.RIGHT_PAREN, "Expect ')' after 'while'.");

        final var body = statement();
        return new Stmt.While(condition, body);
    }

    private Stmt forStatement() {
        consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");

        Stmt initializer;
        if (match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (match(TokenType.VAR)) {
            initializer = varDeclaration();
        } else {
            initializer = expressionStatement();
        }

        Expr condition = null;
        if (!check(TokenType.SEMICOLON)) {
            condition = expression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");

        Expr increment = null;
        if (!check(TokenType.RIGHT_PAREN)) {
            increment = expression();
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");
        var body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(
                    body,
                    new Stmt.Expression(increment)
            ));
        }

        if (condition == null) condition = new Expr.Literal(true);
        body = new Stmt.While(condition, body);

        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, body));
        }

        return body;
    }

    private Stmt expressionStatement() {
        final var value = expression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(value);
    }

    private Stmt.Function function(final String kind) {
        final var name = consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        final List<Token> parameters = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(
                        consume(TokenType.IDENTIFIER, "Expect parameter name")
                );
            } while (match(TokenType.COMMA));
        }
        consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        consume(TokenType.LEFT_BRACE, "Expect '{' before " + kind + " body.");
        final var body = block();
        return new Stmt.Function(name, parameters, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(TokenType.RIGHT_BRACE) && !isAtEnd())
            statements.add(declaration());
        consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment() {
        final var expr = or();
        if (match(TokenType.EQUAL)) {
            final var equals = previous();
            final var value = assignment();

            if (expr instanceof Expr.Variable) {
                final var name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            if (expr instanceof  Expr.Get) {
                final var get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            error(equals, "Invalid assignment target.");
        }
        return expr;
    }

    private Expr or() {
        var expr = and();

        while (match(TokenType.OR)) {
            final var operator = previous();
            final var right = and();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr and() {
        var expr = equality();

        while (match(TokenType.AND)) {
            final var operator = previous();
            final var right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }

        return expr;
    }

    private Expr equality() {
        var expr = comparison();
        while (match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)) {
            final var operator = previous();
            final var right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr comparison() {
        var expr = term();
        while (match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)) {
            final var operator = previous();
            final var right = term();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr term() {
        var expr = factor();
        while (match(TokenType.MINUS, TokenType.PLUS)) {
            final var operator = previous();
            final var right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr factor() {
        var expr = unary();
        while (match(TokenType.SLASH, TokenType.STAR)) {
            final var operator = previous();
            final var right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }

        return expr;
    }

    private Expr unary() {
        if (match(TokenType.BANG, TokenType.MINUS)) {
            final var operator = previous();
            final var right = unary();
            return new Expr.Unary(operator, right);
        }

        return call();
    }

    private Expr call() {
        var expr = primary();
        while (true) {
            if (match(TokenType.LEFT_PAREN)) {
                expr = finishCall(expr);
            } else if(match(TokenType.DOT)) {
              final var name = consume(TokenType.IDENTIFIER, "Expect property name after '.'.");
              expr = new Expr.Get(expr, name);
            } else {
                break;
            }
        }
        return expr;
    }

    private Expr finishCall(final Expr callee) {
        final List<Expr> arguments = new ArrayList<>();
        if (!check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    error(peek(), "Can't have more than 255 arguments");
                }
                arguments.add(expression());
            } while (match(TokenType.COMMA));
        }

        final var paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after argument list.");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr primary() {
        if (match(TokenType.FALSE)) return new Expr.Literal(false);
        if (match(TokenType.TRUE)) return new Expr.Literal(true);
        if (match(TokenType.NIL)) return new Expr.Literal(null);

        if (match(TokenType.THIS)) return new Expr.This(previous());
        if (match(TokenType.SUPER)) {
            final var keyword = previous();
            consume(TokenType.DOT, "Expect '.' after 'super'.");
            final var method = consume(TokenType.IDENTIFIER, "Expect superclass method name");
            return new Expr.Super(keyword, method);
        }

        if (match(TokenType.NUMBER, TokenType.STRING)) return new Expr.Literal(previous().literal);
        if (match(TokenType.IDENTIFIER)) return new Expr.Variable(previous());

        if (match(TokenType.LEFT_PAREN)) {
            final var expr = expression();
            consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expected expression.");
    }

    private boolean match(TokenType... types) {
        for (final var type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private ParseError error(final Token token, final String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        advance();

        while (!isAtEnd()) {
            if (previous().type == TokenType.SEMICOLON) return;

            switch (peek().type) {
                case CLASS: case FOR: case FUN: case IF: case PRINT:
                case RETURN: case VAR: case WHILE:
                    return;
            }

            advance();
        }
    }

    private Token consume(final TokenType type, final String message) {
        if (check(type)) return advance();

        throw error(peek(), message);
    }
}
