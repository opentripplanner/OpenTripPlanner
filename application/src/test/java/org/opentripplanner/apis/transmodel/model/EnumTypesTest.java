package org.opentripplanner.apis.transmodel.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.ROUTING_ERROR_CODE;
import static org.opentripplanner.apis.transmodel.model.EnumTypes.map;

import java.util.EnumSet;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.doc.DocumentedEnum;
import org.opentripplanner.routing.api.response.RoutingErrorCode;

class EnumTypesTest {

  private static final String HI_DESCRIPTION = "Hi description";
  private static final String BAR_DESCRIPTION = "Bar description";
  private static final String FOO_TYPE_DESCRIPTION = "Foo description";

  @Test
  void createEnum() {
    var res = EnumTypes.createEnum("oof", Foo.values(), it -> it.name().toUpperCase());
    assertEquals("oof", res.getName());
    var bar = res.getValue("BAR");
    assertEquals("BAR", bar.getName());
  }

  @Test
  void createFromDocumentedEnum() {
    var res = EnumTypes.createFromDocumentedEnum(
      "oof",
      List.of(EnumTypes.map("Ih", Foo.Hi), EnumTypes.map("Rab", Foo.Bar))
    );

    assertEquals("oof", res.getName());
    assertEquals(FOO_TYPE_DESCRIPTION, res.getDescription());

    var bar = res.getValue("Rab");
    assertEquals("Rab", bar.getName());
    assertEquals(BAR_DESCRIPTION, bar.getDescription());

    var hi = res.getValue("Ih");
    assertEquals("Ih", hi.getName());
    assertEquals(HI_DESCRIPTION, hi.getDescription());
  }

  @Test
  void createFromDocumentedEnumMissingValueThrowsException() {
    assertThrows(
      IllegalStateException.class,
      () -> EnumTypes.createFromDocumentedEnum("oof", List.of(EnumTypes.map("Rab", Foo.Bar)))
    );
  }

  @Test
  void createFromDocumentedEnumDuplicateThrowsException() {
    assertThrows(
      IllegalStateException.class,
      () ->
        EnumTypes.createFromDocumentedEnum(
          "oof",
          List.of(
            EnumTypes.map("Rab", Foo.Bar),
            EnumTypes.map("Hi", Foo.Hi),
            // Duplicate: Bar -> throw exception
            EnumTypes.map("Bar", Foo.Bar)
          )
        )
    );
  }

  @Test
  void testMap() {
    Object mapping = map("iH", Foo.Hi);
    assertEquals("DocumentedEnumMapping[apiName=iH, internal=Hi]", mapping.toString());
  }

  @Test
  void assertAllRoutingErrorCodesAreMapped() {
    var expected = EnumSet.allOf(RoutingErrorCode.class);
    var values = EnumSet.copyOf(
      ROUTING_ERROR_CODE.getValues().stream().map(it -> (RoutingErrorCode) it.getValue()).toList()
    );
    assertEquals(expected, values);
  }

  private enum Foo implements DocumentedEnum<Foo> {
    Hi(HI_DESCRIPTION),
    Bar(BAR_DESCRIPTION);

    private final String description;

    Foo(String description) {
      this.description = description;
    }

    @Override
    public String typeDescription() {
      return FOO_TYPE_DESCRIPTION;
    }

    @Override
    public String enumValueDescription() {
      return description;
    }
  }
}
