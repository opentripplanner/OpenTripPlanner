package org.opentripplanner.model.fare;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ext.fares.model.FareTransferRule;

class SerializationTest {

  public static final List<Class<?>> CLASSES = List.of(
    FareProduct.class,
    FareTransferRule.class,
    FareMedium.class,
    RiderCategory.class
  );

  static Stream<Class<?>> cases() {
    var fields = CLASSES.stream()
      .flatMap(c ->
        Arrays.stream(c.getDeclaredFields()).map(Field::getType).filter(type -> !type.isPrimitive())
      );

    return Stream.concat(CLASSES.stream(), fields).distinct();
  }

  @ParameterizedTest
  @MethodSource("cases")
  void serializable(Class<?> clazz) {
    assertInstanceOf(Serializable.class, clazz);
  }
}
