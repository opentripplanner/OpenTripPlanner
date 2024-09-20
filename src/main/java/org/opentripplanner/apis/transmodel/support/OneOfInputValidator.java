package org.opentripplanner.apis.transmodel.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Validate @oneOf directive, this validation is NOT done by the Java GraphQL library at the
 * moment(v22.1). Remove this when enforced by the library. The {@code @oneOf} is an experimental
 * feature in this version of the library.
 * <p>
 * See {@link graphql.Directives#OneOfDirective}
 */
public class OneOfInputValidator {

  /**
   * Validate that the {@code parent} {@code map} only has one entry.
   *
   * @return the field with a value set.
   */
  public static String validateOneOf(
    Map<String, Object> map,
    String parent,
    String... definedFields
  ) {
    var fieldsInInput = Arrays
      .stream(definedFields)
      .map(k -> map.containsKey(k) ? k : null)
      .filter(Objects::nonNull)
      .toList();

    if (fieldsInInput.isEmpty()) {
      throw new IllegalArgumentException(
        "No entries in '%s @oneOf'. One of '%s' must be set.".formatted(
            parent,
            String.join("', '", definedFields)
          )
      );
    }
    if (fieldsInInput.size() > 1) {
      throw new IllegalArgumentException(
        "Only one entry in '%s @oneOf' is allowed. Set: '%s'".formatted(
            parent,
            String.join("', '", fieldsInInput)
          )
      );
    }

    var field = fieldsInInput.getFirst();
    if (map.get(field) instanceof Collection<?> c) {
      if (c.isEmpty()) {
        throw new IllegalArgumentException(
          "'%s' can not be empty in '%s @oneOf'.".formatted(field, parent)
        );
      }
    }
    return field;
  }
}
