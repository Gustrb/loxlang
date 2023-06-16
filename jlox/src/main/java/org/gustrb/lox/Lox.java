package org.gustrb.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Lox {
    private static boolean hadError = false;
    private static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        final var bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

        if (hadError)
            System.exit(65);
        if (hadRuntimeError)
            System.exit(70);
    }

    private static void runPrompt() throws IOException {
        final var input = new InputStreamReader(System.in);
        final var reader = new BufferedReader(input);
        while (true) {
            System.out.print("> ");
            final var line = reader.readLine();
            // Received a ctrl + d
            if (line == null)
                break;

            run(line);

            hadError = false;
        }
    }

    private static void run(String source) {
        final var scanner = new Scanner(source);
        final var tokens = scanner.scanTokens();
        final var parser = new Parser(tokens);
        final var statements = parser.parse();

        if (hadError) return;

        final var resolver = new Resolver(interpreter);
        resolver.resolve(statements);

        if (hadError) return;

        interpreter.interpret(statements);
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    public static void error(final Token token, final String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'" , message);
        }
    }

    public static void runtimeError(final RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    private static void report(int line, String where, String message) {
        // TODO: I should probably use StringBuilder here but I'm following the book
        //       (a.k.a feeling lazy)
        System.err.println("[line "+ line + "] Error" + where + ": " + message);
    }
}
