package org.opentripplanner.standalone.config.framework.json;

import java.util.Arrays;
import java.util.Optional;
import org.opentripplanner.framework.doc.DocumentedEnum;
import org.opentripplanner.framework.lang.StringUtils;

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
    return StringUtils.kebabCase(en.name());
  }

  /**
   * Used to create a list of all values with description of each value which can be included
   * in documentation. The list will look like this:
   * ```
   *  - `on` Turn on.
   *  - `off` Turn off.
   * ```
   */
  @SuppressWarnings("rawtypes")
  public static <T extends DocumentedEnum> String docEnumValueList(T[] enumValues) {
    var buf = new StringBuilder();
    for (T it : enumValues) {
      buf
        .append(" - `")
        .append(toString((Enum) it))
        .append("` ")
        .append(it.enumValueDescription().replace("\n", "\n   ").trim())
        .append("\n");
    }
    return buf.toString();
  }
}
