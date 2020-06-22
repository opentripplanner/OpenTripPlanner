package org.opentripplanner.ext.legacygraphqlapi;

import graphql.schema.DataFetcher;
import graphql.schema.idl.TypeRuntimeWiring;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class IntrospectionTypeWiring {

  static <T> TypeRuntimeWiring build(Class<T> clazz) throws Exception {
    T instance = clazz.getConstructor().newInstance();

    return TypeRuntimeWiring
        .newTypeWiring(clazz
            .getSimpleName()
            .replaceFirst("LegacyGraphQL", "")
            .replaceAll("Impl$", ""))
        .dataFetchers(Arrays
            .stream(clazz.getDeclaredMethods())
            .filter(isMethodPublic)
            .filter(isMethodReturnTypeDataFetcher)
            .collect(Collectors.toMap(Method::getName, method -> {
              try {
                DataFetcher dataFetcher = (DataFetcher) method.invoke(instance);
                if (dataFetcher == null) {
                  throw new RuntimeException(String.format(
                      "Data fetcher %s for type %s is null",
                      method.getName(),
                      clazz.getSimpleName()
                  ));
                }
                return dataFetcher;
              } catch (IllegalAccessException | InvocationTargetException error) {
                throw new RuntimeException(String.format(
                    "Data fetcher %s for type %s threw error",
                    method.getName(),
                    clazz.getSimpleName()
                ), error);
              }
            })))
        .build();
  }

  private static final Predicate<Method> isMethodPublic = method -> Modifier.isPublic(method.getModifiers());

  private static final Predicate<Method> isMethodReturnTypeDataFetcher = (
      (Predicate<Method>) method -> method.getReturnType().equals(DataFetcher.class)
  ).or(method -> Arrays.asList(method.getReturnType().getInterfaces()).contains(DataFetcher.class));
}