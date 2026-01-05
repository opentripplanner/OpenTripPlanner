package org.opentripplanner.model.fare;

import static com.google.common.truth.Truth.assertThat;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ext.fares.model.FareTransferRule;
import org.opentripplanner.ext.fares.model.TimeLimit;
import org.opentripplanner.ext.fares.model.Timeframe;

class SerializationTest {

  public static final List<Class<?>> CLASSES = List.of(
    FareProduct.class,
    FareTransferRule.class,
    FareMedium.class,
    RiderCategory.class,
    TimeLimit.class,
    Timeframe.class
  );

  static Stream<Class<?>> cases() {
    var fields = CLASSES.stream()
      .flatMap(c ->
        Arrays.stream(c.getDeclaredFields()).map(Field::getType).filter(type -> !type.isPrimitive())
      );

    return Stream.concat(CLASSES.stream(), fields)
      .filter(c -> !c.isRecord())
      .filter(c -> !c.equals(Collection.class))
      .distinct();
  }

  @ParameterizedTest
  @MethodSource("cases")
  void serializable(Class<?> clazz) {
    assertThat(clazz).isAssignableTo(Serializable.class);
  }
}
