package org.opentripplanner.graphql.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses Java type expressions from the codegen configuration, like
 * {@code graphql.relay.Edge<TransitAlert>} or {@code Map.Entry<String, String>}, into JavaPoet
 * types.
 * <p>
 * A name is resolved in this order: a known import (by its first segment), a fully qualified name
 * (lower-case package segments followed by the class), and finally {@code java.lang}.
 */
final class JavaTypeParser {

  private static final Set<String> JAVA_LANG = Set.of(
    "Boolean",
    "Byte",
    "Character",
    "Double",
    "Float",
    "Integer",
    "Iterable",
    "Long",
    "Number",
    "Object",
    "Short",
    "String"
  );

  private static final Pattern TOKEN = Pattern.compile(
    "\\s*([A-Za-z_$][\\w$]*(?:\\.[A-Za-z_$][\\w$]*)*|<|>|,)"
  );

  private final Map<String, ClassName> imports;

  /**
   * @param imports types that can be referenced by simple name, keyed by that name
   */
  JavaTypeParser(Map<String, ClassName> imports) {
    this.imports = Map.copyOf(imports);
  }

  TypeName parse(String expression) {
    var tokens = tokenize(expression);
    var parser = new Parser(expression, tokens);
    TypeName type = parser.type();
    if (parser.pos != tokens.size()) {
      throw new IllegalArgumentException("Unexpected trailing input in type: " + expression);
    }
    return type;
  }

  /** Resolves a possibly dotted, non-generic type name. */
  ClassName resolveName(String name) {
    String[] segments = name.split("\\.");
    ClassName imported = imports.get(segments[0]);
    if (imported != null) {
      return nested(imported, segments, 1);
    }
    for (int i = 0; i < segments.length; i++) {
      if (Character.isUpperCase(segments[i].charAt(0))) {
        if (i == 0) {
          break;
        }
        String packageName = String.join(".", Arrays.copyOfRange(segments, 0, i));
        return nested(ClassName.get(packageName, segments[i]), segments, i + 1);
      }
    }
    if (JAVA_LANG.contains(segments[0])) {
      return nested(ClassName.get("java.lang", segments[0]), segments, 1);
    }
    throw new IllegalArgumentException("Cannot resolve Java type: " + name);
  }

  private static ClassName nested(ClassName type, String[] segments, int from) {
    ClassName result = type;
    for (int i = from; i < segments.length; i++) {
      result = result.nestedClass(segments[i]);
    }
    return result;
  }

  private static List<String> tokenize(String expression) {
    var tokens = new ArrayList<String>();
    Matcher matcher = TOKEN.matcher(expression);
    int pos = 0;
    while (pos < expression.length()) {
      if (!matcher.find(pos) || matcher.start() != pos) {
        if (expression.substring(pos).isBlank()) {
          break;
        }
        throw new IllegalArgumentException("Invalid Java type expression: " + expression);
      }
      tokens.add(matcher.group(1));
      pos = matcher.end();
    }
    return tokens;
  }

  private final class Parser {

    private final String expression;
    private final List<String> tokens;
    private int pos = 0;

    private Parser(String expression, List<String> tokens) {
      this.expression = expression;
      this.tokens = tokens;
    }

    private TypeName type() {
      ClassName raw = resolveName(next());
      if (!"<".equals(peek())) {
        return raw;
      }
      next();
      var arguments = new ArrayList<TypeName>();
      arguments.add(type());
      while (",".equals(peek())) {
        next();
        arguments.add(type());
      }
      expect(">");
      return ParameterizedTypeName.get(raw, arguments.toArray(TypeName[]::new));
    }

    private String peek() {
      return pos < tokens.size() ? tokens.get(pos) : null;
    }

    private String next() {
      if (pos >= tokens.size()) {
        throw new IllegalArgumentException("Unexpected end of type expression: " + expression);
      }
      return tokens.get(pos++);
    }

    private void expect(String token) {
      if (!token.equals(next())) {
        throw new IllegalArgumentException("Expected '" + token + "' in type: " + expression);
      }
    }
  }
}
