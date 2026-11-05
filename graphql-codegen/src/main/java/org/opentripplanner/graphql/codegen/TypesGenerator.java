package org.opentripplanner.graphql.codegen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.CodeBlock;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import graphql.language.EnumTypeDefinition;
import graphql.language.EnumValueDefinition;
import graphql.language.FieldDefinition;
import graphql.language.InputObjectTypeDefinition;
import graphql.language.InputValueDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.lang.model.element.Modifier;

/**
 * Generates a class with the Java types of the GraphQL input values:
 * <ul>
 *   <li>an enum for each GraphQL enum</li>
 *   <li>a class for each input type, named with an {@code Input} suffix</li>
 *   <li>a class for the arguments of each object type field that has arguments, named
 *   {@code <Type><Field>Args}</li>
 * </ul>
 * The input and argument classes are constructed from the raw argument map that graphql-java
 * passes to the data fetchers, and are immutable. Enum values are accepted both as enum constants
 * and as strings.
 */
final class TypesGenerator {

  private static final ClassName MAP = ClassName.get(Map.class);
  private static final ClassName LIST = ClassName.get(List.class);
  private static final ClassName COLLECTORS = ClassName.get(Collectors.class);
  private static final TypeName STRING = ClassName.get(String.class);
  private static final TypeName OBJECT = ClassName.get(Object.class);
  private static final TypeName ARGS_MAP = ParameterizedTypeName.get(MAP, STRING, OBJECT);

  private final CodegenContext context;

  TypesGenerator(CodegenContext context) {
    this.context = context;
  }

  JavaFile generate() {
    var outer = TypeSpec.classBuilder(context.outerClass()).addModifiers(Modifier.PUBLIC);
    for (TypeDefinition<?> type : context.types()) {
      switch (type) {
        case EnumTypeDefinition enumType -> outer.addType(enumType(enumType));
        case InputObjectTypeDefinition inputType -> outer.addType(
          inputClass(inputTypeName(inputType.getName()), inputType.getInputValueDefinitions())
        );
        case ObjectTypeDefinition objectType -> {
          for (FieldDefinition field : objectType.getFieldDefinitions()) {
            if (!field.getInputValueDefinitions().isEmpty()) {
              String name =
                context.javaTypeName(objectType.getName()) +
                CodegenContext.capitalize(field.getName()) +
                "Args";
              outer.addType(inputClass(name, field.getInputValueDefinitions()));
            }
          }
        }
        default -> {
        }
      }
    }
    return context.toJavaFile(outer.build());
  }

  private TypeSpec enumType(EnumTypeDefinition enumType) {
    var builder = TypeSpec.enumBuilder(context.javaTypeName(enumType.getName())).addModifiers(
      Modifier.PUBLIC
    );
    CodegenContext.addJavadoc(builder, enumType.getDescription());
    for (EnumValueDefinition value : enumType.getEnumValueDefinitions()) {
      builder.addEnumConstant(CodegenContext.javaMemberName(value.getName()));
    }
    return builder.build();
  }

  private TypeSpec inputClass(String className, List<InputValueDefinition> fields) {
    var builder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC, Modifier.STATIC);
    var constructorBody = CodeBlock.builder();
    var nullAssignments = CodeBlock.builder();
    var getters = new ArrayList<MethodSpec>();

    for (InputValueDefinition field : fields) {
      InputType type = resolve(field);
      String fieldName = CodegenContext.javaMemberName(field.getName());

      builder.addField(
        FieldSpec.builder(type.javaType(), fieldName, Modifier.PRIVATE, Modifier.FINAL).build()
      );
      constructorBody.add(assignFromArgs(field.getName(), fieldName, type));
      nullAssignments.addStatement("this.$N = null", fieldName);
      getters.add(
        MethodSpec.methodBuilder("get" + context.javaTypeName(field.getName()))
          .addModifiers(Modifier.PUBLIC)
          .returns(type.javaType())
          .addStatement("return this.$N", fieldName)
          .build()
      );
    }

    builder.addMethod(
      MethodSpec.constructorBuilder()
        .addModifiers(Modifier.PUBLIC)
        .addParameter(ARGS_MAP, "args")
        .beginControlFlow("if (args != null)")
        .addCode(constructorBody.build())
        .nextControlFlow("else")
        .addCode(nullAssignments.build())
        .endControlFlow()
        .build()
    );
    builder.addMethods(getters);
    return builder.build();
  }

  private CodeBlock assignFromArgs(String argName, String fieldName, InputType type) {
    var code = CodeBlock.builder();
    if (type.kind() == Kind.SCALAR) {
      return code
        .addStatement("this.$N = ($T) args.get($S)", fieldName, type.javaType(), argName)
        .build();
    }
    if (type.listDepth() > 1) {
      throw new IllegalArgumentException(
        "Nested lists of enums or input types are not supported: " + argName
      );
    }
    TypeName base = type.baseType();
    if (type.listDepth() == 1) {
      code.beginControlFlow("if (args.get($S) != null)", argName);
      if (type.kind() == Kind.ENUM) {
        code.addStatement(
          "this.$N = (($T<$T>) args.get($S)).stream()" +
            ".map(item -> item instanceof $T ? item : $T.valueOf(($T) item))" +
            ".map($T.class::cast)" +
            ".collect($T.toList())",
          fieldName,
          LIST,
          OBJECT,
          argName,
          base,
          base,
          STRING,
          base,
          COLLECTORS
        );
      } else {
        code.addStatement(
          "this.$N = (($T<$T>) args.get($S)).stream()" +
            ".map(o -> o == null ? null : new $T(o))" +
            ".collect($T.toList())",
          fieldName,
          context.listType(),
          ARGS_MAP,
          argName,
          base,
          COLLECTORS
        );
      }
      return code
        .nextControlFlow("else")
        .addStatement("this.$N = null", fieldName)
        .endControlFlow()
        .build();
    }
    if (type.kind() == Kind.ENUM) {
      return code
        .beginControlFlow("if (args.get($S) instanceof $T)", argName, base)
        .addStatement("this.$N = ($T) args.get($S)", fieldName, base, argName)
        .nextControlFlow("else if (args.get($S) != null)", argName)
        .addStatement("this.$N = $T.valueOf(($T) args.get($S))", fieldName, base, STRING, argName)
        .nextControlFlow("else")
        .addStatement("this.$N = null", fieldName)
        .endControlFlow()
        .build();
    }
    // Input objects are always created, also when the argument is missing.
    return code
      .addStatement("this.$N = new $T(($T) args.get($S))", fieldName, base, ARGS_MAP, argName)
      .build();
  }

  private InputType resolve(InputValueDefinition field) {
    String baseName = CodegenContext.baseTypeName(field.getType());
    TypeDefinition<?> definition = context.typeDefinition(baseName);

    Kind kind;
    TypeName base;
    if (definition instanceof InputObjectTypeDefinition) {
      kind = Kind.INPUT;
      base = context.outerClass().nestedClass(inputTypeName(baseName));
    } else if (definition instanceof EnumTypeDefinition) {
      kind = Kind.ENUM;
      base = context.outerClass().nestedClass(context.javaTypeName(baseName));
    } else if (context.isScalar(baseName)) {
      kind = Kind.SCALAR;
      base = context.scalarType(baseName);
    } else {
      kind = Kind.SCALAR;
      base = context.defaultType();
    }
    return new InputType(
      kind,
      base,
      context.wrapWithLists(base, field.getType()),
      CodegenContext.listDepth(field.getType())
    );
  }

  private String inputTypeName(String graphQLName) {
    String name = context.javaTypeName(graphQLName);
    return name.endsWith("Input") ? name : name + "Input";
  }

  private enum Kind {
    SCALAR,
    ENUM,
    INPUT,
  }

  private record InputType(Kind kind, TypeName baseType, TypeName javaType, int listDepth) {}
}
