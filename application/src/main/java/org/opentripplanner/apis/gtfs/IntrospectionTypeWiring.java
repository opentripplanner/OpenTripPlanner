package org.opentripplanner.apis.gtfs;

import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.AsyncDataFetcher;
import graphql.schema.DataFetcher;
import graphql.schema.idl.TypeDefinitionRegistry;
import graphql.schema.idl.TypeRuntimeWiring;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.opentripplanner.framework.application.OTPFeature;

class IntrospectionTypeWiring {

  private static final Predicate<Method> isMethodPublic = method ->
    Modifier.isPublic(method.getModifiers());
  private static final Predicate<Method> isMethodReturnTypeDataFetcher =
    ((Predicate<Method>) method -> method.getReturnType().equals(DataFetcher.class)).or(method ->
        Arrays.asList(method.getReturnType().getInterfaces()).contains(DataFetcher.class)
      );

  private final TypeDefinitionRegistry typeRegistry;

  public IntrospectionTypeWiring(TypeDefinitionRegistry typeRegistry) {
    this.typeRegistry = typeRegistry;
  }

  <T> TypeRuntimeWiring build(Class<T> clazz) throws Exception {
    T instance = clazz.getConstructor().newInstance();

    String typeName = clazz.getSimpleName().replaceAll("Impl$", "");

    TypeDefinition type = typeRegistry
      .getType(typeName)
      .orElseThrow(() ->
        new IllegalArgumentException("Type %s not found in schema".formatted(typeName))
      );

    if (!(type instanceof ObjectTypeDefinition objectType)) {
      throw new IllegalArgumentException("Type %s is not object type".formatted(type.getName()));
    }

    return TypeRuntimeWiring.newTypeWiring(clazz.getSimpleName().replaceAll("Impl$", ""))
      .dataFetchers(
        Arrays.stream(clazz.getDeclaredMethods())
          .filter(isMethodPublic)
          .filter(isMethodReturnTypeDataFetcher)
          .collect(
            Collectors.toMap(Method::getName, method -> {
              String fieldName = method.getName();
              try {
                DataFetcher dataFetcher = (DataFetcher) method.invoke(instance);
                if (dataFetcher == null) {
                  throw new RuntimeException(
                    String.format(
                      "Data fetcher %s for type %s is null",
                      fieldName,
                      clazz.getSimpleName()
                    )
                  );
                }
                if (
                  OTPFeature.AsyncGraphQLFetchers.isOn() &&
                  objectType
                    .getFieldDefinitions()
                    .stream()
                    .filter(fieldDefinition -> fieldDefinition.getName().equals(fieldName))
                    .anyMatch(fieldDefinition ->
                      fieldDefinition
                        .getDirectives()
                        .stream()
                        .anyMatch(directive -> directive.getName().equals("async"))
                    )
                ) {
                  return AsyncDataFetcher.async(dataFetcher);
                }

                return dataFetcher;
              } catch (IllegalAccessException | InvocationTargetException error) {
                throw new RuntimeException(
                  String.format(
                    "Data fetcher %s for type %s threw error",
                    fieldName,
                    clazz.getSimpleName()
                  ),
                  error
                );
              }
            })
          )
      )
      .build();
  }
}
