package org.opentripplanner.standalone.config.framework.json;

import java.util.Arrays;
import java.util.Optional;

public class EnumMapper {

  @SuppressWarnings("unchecked")
  public static <E extends Enum<E>> Optional<E> mapToEnum(String text, Class<E> type) {
    return (Optional<E>) mapToEnum2(text, type);
  }

  public static Optional<? extends Enum<?>> mapToEnum2(String text, Class<? extends Enum<?>> type) {
    if (text == null) {
      return Optional.empty();
    }
    var name = text.toUpperCase().replace('-', '_');
    return Arrays
      .stream(type.getEnumConstants())
      .filter(it -> it.name().toUpperCase().equals(name))
      .findFirst();
  }

  public static String toString(Enum<?> en) {
    return en.name().toLowerCase().replace('_', '-');
  }
}
