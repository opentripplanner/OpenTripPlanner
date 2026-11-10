package org.opentripplanner.graphql.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import javax.lang.model.element.Modifier;

/**
 * Generates a class with a data fetcher interface for each GraphQL object type, with an abstract
 * {@code DataFetcher} method per field. The OTP data fetchers implement these interfaces, so the
 * compiler reports schema changes that the implementation does not follow. Interface and union
 * types get no interface; their type resolvers implement {@code TypeResolver} directly.
 * <p>
 * The {@code DataFetcher} result types come from the configured {@code scalars} and
 * {@code mappers}; other types are mapped to {@code Object}.
 */
final class DataFetchersGenerator {

  private static final ClassName DATA_FETCHER = ClassName.get("graphql.schema", "DataFetcher");

  private final CodegenContext context;

  DataFetchersGenerator(CodegenContext context) {
    this.context = context;
  }

  JavaFile generate() {
    var outer = TypeSpec.classBuilder(context.outerClass()).addModifiers(Modifier.PUBLIC);
    for (TypeDefinition<?> type : context.types()) {
      if (type instanceof ObjectTypeDefinition objectType) {
        outer.addType(fetcherInterface(objectType));
      }
    }
    return context.toJavaFile(outer.build());
  }

  private TypeSpec fetcherInterface(ObjectTypeDefinition type) {
    var builder = TypeSpec.interfaceBuilder(context.javaTypeName(type.getName())).addModifiers(
      Modifier.PUBLIC
    );
    CodegenContext.addJavadoc(builder, type.getDescription());
    for (FieldDefinition field : type.getFieldDefinitions()) {
      builder.addMethod(
        MethodSpec.methodBuilder(field.getName())
          .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
          .returns(ParameterizedTypeName.get(DATA_FETCHER, fieldType(field)))
          .build()
      );
    }
    return builder.build();
  }

  private TypeName fieldType(FieldDefinition field) {
    String baseName = CodegenContext.baseTypeName(field.getType());
    TypeName base;
    if (context.isScalar(baseName)) {
      base = context.scalarType(baseName);
    } else if (context.output().mappers().containsKey(baseName)) {
      base = context.parseType(mapperType(context.output().mappers().get(baseName)));
    } else {
      base = context.defaultType();
    }
    return context.wrapWithLists(base, field.getType());
  }

  /**
   * The Java type of a mapper. A mapper is either a type expression, or an import followed by
   * {@code #} and a type expression that uses the imported name, like
   * {@code graphql.relay.Edge#Edge<TransitAlert>}.
   */
  static String mapperType(String mapper) {
    int hash = mapper.indexOf('#');
    return hash < 0 ? mapper : mapper.substring(hash + 1);
  }

  /** The import of a mapper, or {@code null} if the mapper is a plain type expression. */
  static String mapperImport(String mapper) {
    int hash = mapper.indexOf('#');
    return hash < 0 ? null : mapper.substring(0, hash);
  }
}
