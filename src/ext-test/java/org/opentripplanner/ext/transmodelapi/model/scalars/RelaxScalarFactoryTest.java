package org.opentripplanner.ext.transmodelapi.model.scalars;

import static org.junit.jupiter.api.Assertions.assertEquals;

import graphql.schema.GraphQLScalarType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.api.request.preference.Relax;

class RelaxScalarFactoryTest {

  public static final Relax RELAX = new Relax(1.05, 300);
  public static final String SER = "1.05 * x + 300";
  private static GraphQLScalarType subject;

  @BeforeAll
  static void setup() {
    subject = RelaxScalarFactory.createRelaxFunctionScalar(1.2, 3600);
  }

  @Test
  void serialize() {
    var result = subject.getCoercing().serialize(RELAX);
    assertEquals(SER, result);
  }

  @Test
  void parseValue() {
    var result = subject.getCoercing().parseValue(SER);
    assertEquals(RELAX, result);
  }
}
