package org.gustrb.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Lox {
    private static boolean hadError = false;

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

        for (final var token : tokens)
            System.out.println(token);
    }

    public static void error(int line, String message) {
        report(line, "", message);
    }

    private static void report(int line, String where, String message) {
        // TODO: I should probably use StringBuilder here but I'm following the book
        //       (a.k.a feeling lazy)
        System.err.println("[line "+ line + "] Error" + where + ": " + message);
    }
}
