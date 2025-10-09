package org.opentripplanner.ext.carpooling.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Combines multiple insertion validators using AND logic.
 * <p>
 * All validators must pass for the insertion to be considered valid.
 * Evaluation stops at the first failure (short-circuit).
 */
public class CompositeValidator implements InsertionValidator {

  private final List<InsertionValidator> validators;

  public CompositeValidator(List<InsertionValidator> validators) {
    this.validators = new ArrayList<>(validators);
  }

  public CompositeValidator(InsertionValidator... validators) {
    this(Arrays.asList(validators));
  }

  /**
   * Creates a standard validator with capacity and directional checks.
   */
  public static CompositeValidator standard() {
    return new CompositeValidator(new CapacityValidator(), new DirectionalValidator());
  }

  @Override
  public ValidationResult validate(ValidationContext context) {
    for (InsertionValidator validator : validators) {
      ValidationResult result = validator.validate(context);
      if (!result.isValid()) {
        return result; // Short-circuit: return first failure
      }
    }
    return ValidationResult.valid(); // All validators passed
  }

  /**
   * Adds a validator to the composite.
   */
  public CompositeValidator add(InsertionValidator validator) {
    validators.add(validator);
    return this;
  }

  /**
   * Gets the number of validators.
   */
  public int size() {
    return validators.size();
  }
}
