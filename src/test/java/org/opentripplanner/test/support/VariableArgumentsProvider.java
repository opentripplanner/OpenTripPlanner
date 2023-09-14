package org.opentripplanner.test.support;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

/**
 * This annotation processor allows you to provide a variable as the input for a JUnit {@link
 * org.junit.jupiter.params.ParameterizedTest}.
 *
 * Check the usages of {@link VariableSource} to see examples for how to use.
 */
class VariableArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<VariableSource> {

  private String variableName;

  @Override
  public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
    return context
      .getTestClass()
      .map(this::getField)
      .map(this::getValue)
      .orElseThrow(() -> new IllegalArgumentException("Failed to load test arguments"));
  }

  @Override
  public void accept(VariableSource variableSource) {
    variableName = variableSource.value();
  }

  private Field getField(Class<?> clazz) {
    try {
      return clazz.getDeclaredField(variableName);
    } catch (Exception e) {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  private Stream<Arguments> getValue(Field field) {
    Object value = null;
    var accessible = field.isAccessible();
    try {
      field.setAccessible(true);
      value = field.get(null);
    } catch (Exception ignored) {}

    field.setAccessible(accessible);

    if (value == null) {
      return null;
    } else if (value instanceof Collection<?> collection) {
      return (Stream<Arguments>) collection.stream();
    } else if (value instanceof Stream stream) {
      return stream;
    } else {
      throw new IllegalArgumentException(
        "Cannot convert %s to stream.".formatted(value.getClass())
      );
    }
  }
}
