/**
 * This script is used to generate the Expr class and
 * its subclasses, this code is just a script meant only to be used
 * during the development of the code, and it must be used to generate such
 * classes.
 *
 * The code is a bit rough around the edges, but since it is not a place
 * we need to change that often Imma left here until I am bothered enough to make
 * it better :P
 */

package org.gustrb.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class AstGenerator {
    final static String TABULATION = "    ";

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("Usage: generate_ast <output_dir>");
            System.exit(65);
        }

        final var outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList(
                "Assign  : Token name, Expr value",
                "Binary  : Expr left, Token operator, Expr right",
                "Grouping: Expr expression",
                "Literal : Object value",
                "Logical : Expr left, Token operator, Expr right",
                "Unary   : Token operator, Expr right",
                "Variable: Token name"
        ));
        defineAst(outputDir, "Stmt", Arrays.asList(
                "Block      : List<Stmt> statements",
                "Expression : Expr expression",
                "If         : Expr condition, Stmt thenBranch, Stmt elseBranch",
                "Print      : Expr expression",
                "While      : Expr condition, Stmt body",
                "Var        : Token name, Expr initializer"
        ));
    }

    private static void defineAst(final String outputDir, final String baseName, final List<String> types) throws IOException {
        final var path = outputDir + "/" + baseName + ".java";
        final var writer = new PrintWriter(path, "UTF-8");

        writer.println("package org.gustrb.lox;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + " {");
        defineVisitor(writer, baseName, types);

        for (final var type : types) {
            final var className = type.split(":")[0].trim();
            final var fields = type.split(":")[1].trim();
            defineType(writer, baseName, className, fields);
        }

        writer.println();
        writer.println(TABULATION + "abstract <R> R accept(Visitor<R> visitor);");

        writer.println("}");
        writer.close();
    }

    private static void defineType(
            final PrintWriter writer,
            final String baseName,
            final String className,
            final String fieldsList
    ) {
        writer.println(TABULATION + "static class " + className + " extends " + baseName + " {");

        // constructor
        writer.println(TABULATION + TABULATION + "public " + className + "(" + fieldsList + ") {");

        final var fields = fieldsList.split(", ");
        for (final var field : fields) {
            final var name = field.split(" ")[1];
            writer.println(TABULATION + TABULATION + TABULATION +  "this." + name + " = " + name + ";");
        }

        writer.println(TABULATION + TABULATION + "}");
        writer.println();

        writer.println(TABULATION + TABULATION + "@Override");
        writer.println(TABULATION + TABULATION + "<R> R accept(Visitor<R> visitor) {");
        writer.println(TABULATION + TABULATION + TABULATION + "return visitor.visit" + className + baseName + "(this);");
        writer.println(TABULATION + TABULATION + "}");

        for (final var field : fields)
            writer.println(TABULATION + TABULATION + "final " + field + ";");

        writer.println(TABULATION + "}");
    }

    private static void defineVisitor(final PrintWriter writer, final String baseName, final List<String> types) {
        writer.println(TABULATION + "interface Visitor<R> {");
        for (final var type : types) {
            final var typeName = type.split(":")[0].trim();
            final var signature = "R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");";
            writer.println(TABULATION + TABULATION + signature);
        }
        writer.println(TABULATION + "}");
    }
}
