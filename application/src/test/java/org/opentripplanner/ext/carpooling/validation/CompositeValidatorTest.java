package org.opentripplanner.ext.carpooling.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.util.PassengerCountTimeline;
import org.opentripplanner.ext.carpooling.validation.InsertionValidator.ValidationContext;
import org.opentripplanner.ext.carpooling.validation.InsertionValidator.ValidationResult;

class CompositeValidatorTest {

  @Test
  void validate_allValidatorsPass_returnsValid() {
    var validator1 = mock(InsertionValidator.class);
    var validator2 = mock(InsertionValidator.class);

    when(validator1.validate(any())).thenReturn(ValidationResult.valid());
    when(validator2.validate(any())).thenReturn(ValidationResult.valid());

    var composite = new CompositeValidator(List.of(validator1, validator2));
    var context = createDummyContext();

    var result = composite.validate(context);
    assertTrue(result.isValid());
  }

  @Test
  void validate_oneValidatorFails_returnsInvalid() {
    var validator1 = mock(InsertionValidator.class);
    var validator2 = mock(InsertionValidator.class);

    when(validator1.validate(any())).thenReturn(ValidationResult.valid());
    when(validator2.validate(any())).thenReturn(ValidationResult.invalid("Test failure"));

    var composite = new CompositeValidator(List.of(validator1, validator2));
    var context = createDummyContext();

    var result = composite.validate(context);
    assertFalse(result.isValid());
    assertEquals("Test failure", result.reason());
  }

  @Test
  void validate_shortCircuits_afterFirstFailure() {
    var validator1 = mock(InsertionValidator.class);
    var validator2 = mock(InsertionValidator.class);
    var validator3 = mock(InsertionValidator.class);

    when(validator1.validate(any())).thenReturn(ValidationResult.valid());
    when(validator2.validate(any())).thenReturn(ValidationResult.invalid("Fail"));

    var composite = new CompositeValidator(List.of(validator1, validator2, validator3));
    var context = createDummyContext();

    composite.validate(context);

    verify(validator1).validate(any());
    verify(validator2).validate(any());
    verify(validator3, never()).validate(any()); // Should not be called
  }

  @Test
  void validate_firstValidatorFails_doesNotCallOthers() {
    var validator1 = mock(InsertionValidator.class);
    var validator2 = mock(InsertionValidator.class);

    when(validator1.validate(any())).thenReturn(ValidationResult.invalid("First fail"));

    var composite = new CompositeValidator(List.of(validator1, validator2));
    var context = createDummyContext();

    composite.validate(context);

    verify(validator1).validate(any());
    verify(validator2, never()).validate(any());
  }

  @Test
  void standard_includesAllStandardValidators() {
    var composite = CompositeValidator.standard();

    // Test with scenario that should fail capacity validation
    var trip = createTripWithCapacity(1, OSLO_CENTER, List.of(createStop(0, +1)), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);
    var context = new ValidationContext(
      2,
      3,
      OSLO_EAST,
      OSLO_WEST,
      List.of(OSLO_CENTER, OSLO_EAST, OSLO_NORTH),
      timeline
    );

    var result = composite.validate(context);
    assertFalse(result.isValid());
  }

  @Test
  void standard_checksDirectionalConstraints() {
    var composite = CompositeValidator.standard();

    // Test with scenario that should fail directional validation
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);
    var context = new ValidationContext(
      1,
      2,
      OSLO_SOUTH, // Backtracking
      OSLO_NORTH,
      List.of(OSLO_CENTER, OSLO_NORTH),
      timeline
    );

    var result = composite.validate(context);
    assertFalse(result.isValid());
  }

  @Test
  void emptyValidator_acceptsAll() {
    var composite = new CompositeValidator(List.of());
    var context = createDummyContext();

    var result = composite.validate(context);
    assertTrue(result.isValid());
  }

  @Test
  void singleValidator_behavesCorrectly() {
    var validator = mock(InsertionValidator.class);
    when(validator.validate(any())).thenReturn(ValidationResult.valid());

    var composite = new CompositeValidator(List.of(validator));
    var context = createDummyContext();

    var result = composite.validate(context);
    assertTrue(result.isValid());
    verify(validator).validate(context);
  }

  private ValidationContext createDummyContext() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);
    return new ValidationContext(
      1,
      2,
      OSLO_EAST,
      OSLO_WEST,
      List.of(OSLO_CENTER, OSLO_NORTH),
      timeline
    );
  }
}
