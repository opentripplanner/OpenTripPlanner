package org.opentripplanner.graphql.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.graphql.codegen.CodegenConfig.OutputFile;

/**
 * Generates Java types and data fetcher interfaces for a GraphQL API from its schema.
 * It replaces the JavaScript {@code graphql-codegen} with the {@code java} and
 * {@code java-resolvers} plugins and reads a subset of its configuration format. The generated
 * members follow the schema order, and the generated code is not formatted for readability.
 * <p>
 * Usage: {@code GraphQLCodegen <config.yml> <output-source-directory>}
 * <p>
 * A file is only written when its content changes, so an unchanged schema does not force the
 * generated sources, and everything that depends on them, to be recompiled.
 */
public final class GraphQLCodegen {

  private GraphQLCodegen() {}

  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      throw new IllegalArgumentException(
        "Usage: GraphQLCodegen <config.yml> <output-source-directory>"
      );
    }
    generate(Path.of(args[0]), Path.of(args[1]));
  }

  /** Generates all files listed in {@code configFile} into the source root {@code outputDir}. */
  public static void generate(Path configFile, Path outputDir) throws IOException {
    CodegenConfig config = CodegenConfig.read(configFile);
    TypeDefinitionRegistry registry = new SchemaParser().parse(config.schema().toFile());
    assertNoTypeExtensions(registry, config.schema());

    for (OutputFile output : config.outputFiles()) {
      write(generate(registry, output), outputDir);
    }
  }

  static JavaFile generate(TypeDefinitionRegistry registry, OutputFile output) {
    String expectedFileName = output.className() + ".java";
    if (!Path.of(output.fileName()).getFileName().toString().equals(expectedFileName)) {
      throw new IllegalArgumentException(
        "Output file " + output.fileName() + " must be named " + expectedFileName
      );
    }
    var context = new CodegenContext(registry, output, new JavaTypeParser(mapperImports(output)));
    return switch (output.generator()) {
      case TYPES -> new TypesGenerator(context).generate();
      case DATA_FETCHERS -> new DataFetchersGenerator(context).generate();
    };
  }

  private static Map<String, ClassName> mapperImports(OutputFile output) {
    var imports = new HashMap<String, ClassName>();
    for (String mapper : output.mappers().values()) {
      String importName = DataFetchersGenerator.mapperImport(mapper);
      if (importName == null) {
        continue;
      }
      ClassName type = ClassName.bestGuess(importName);
      ClassName existing = imports.putIfAbsent(type.simpleName(), type);
      if (existing != null && !existing.equals(type)) {
        throw new IllegalArgumentException(
          "Mappers import two types named " + type.simpleName() + ": " + existing + ", " + type
        );
      }
    }
    return imports;
  }

  private static void assertNoTypeExtensions(TypeDefinitionRegistry registry, Path schema) {
    if (
      !registry.objectTypeExtensions().isEmpty() ||
      !registry.interfaceTypeExtensions().isEmpty() ||
      !registry.unionTypeExtensions().isEmpty() ||
      !registry.enumTypeExtensions().isEmpty() ||
      !registry.inputObjectTypeExtensions().isEmpty()
    ) {
      throw new IllegalArgumentException("Type extensions are not supported: " + schema);
    }
  }

  private static void write(JavaFile javaFile, Path outputDir) throws IOException {
    Path file = outputDir
      .resolve(javaFile.packageName().replace('.', '/'))
      .resolve(javaFile.typeSpec().name() + ".java");
    String content = javaFile.toString();
    if (Files.exists(file) && Files.readString(file, StandardCharsets.UTF_8).equals(content)) {
      return;
    }
    Files.createDirectories(file.getParent());
    Files.writeString(file, content, StandardCharsets.UTF_8);
  }
}
