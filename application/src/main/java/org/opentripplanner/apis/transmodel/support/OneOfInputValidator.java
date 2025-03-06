package org.opentripplanner.apis.transmodel.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;

/**
 * Validate @oneOf directive, this validation is NOT done by the Java GraphQL library at the
 * moment(v22.1). Remove this when enforced by the library. The {@code @oneOf} is an experimental
 * feature in this version of the library. This applies to the code-first approach, not the
 * schema-first approach.
 * <p>
 * See {@link graphql.Directives#OneOfDirective}
 */
public class OneOfInputValidator {

  /**
   * Validate that the {@code parent} {@code map} only has one entry.
   *
   * @param map           The input to validate.
   * @param inputTypeName The name of the type annotated with @oneOf. The name is used in
   *                      the error message only, in case the validation fails.
   * @param definedFields The name of the fields the @oneOf directive apply to.
   *
   * @return the field with a value set.
   */
  public static String validateOneOf(
    Map<String, Object> map,
    String inputTypeName,
    String... definedFields
  ) {
    var fieldsInInput = Arrays.stream(definedFields)
      .map(k -> map.containsKey(k) ? k : null)
      .filter(Objects::nonNull)
      .toList();

    if (fieldsInInput.isEmpty()) {
      throw new IllegalArgumentException(
        "No entries in '%s @oneOf'. One of '%s' must be set.".formatted(
            inputTypeName,
            String.join("', '", definedFields)
          )
      );
    }
    if (fieldsInInput.size() > 1) {
      throw new IllegalArgumentException(
        "Only one entry in '%s @oneOf' is allowed. Set: '%s'".formatted(
            inputTypeName,
            String.join("', '", fieldsInInput)
          )
      );
    }

    // This is not done in the "standard" validator, so if this is replaced by another validator
    // we should consider adding this validation.
    var field = fieldsInInput.getFirst();
    if (map.get(field) instanceof Collection<?> c) {
      if (c.isEmpty()) {
        throw new IllegalArgumentException(
          "'%s' can not be empty in '%s @oneOf'.".formatted(field, inputTypeName)
        );
      }
    }
    return field;
  }
}
