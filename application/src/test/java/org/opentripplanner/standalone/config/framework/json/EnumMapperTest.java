package org.opentripplanner.standalone.config.framework.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.doc.DocumentedEnum;

class EnumMapperTest {

  public static final String DESCRIPTION_OF_THE_TYPE = "Description of the type";

  @Test
  void mapToEnum() {
    assertEquals(Foo.Bar, EnumMapper.mapToEnum("BAR", Foo.class).orElseThrow());
    assertEquals(Foo.BOO_BOO, EnumMapper.mapToEnum("boo-boo", Foo.class).orElseThrow());
    assertEquals(Foo.BOO_BOO, EnumMapper.mapToEnum("BOO_BOO", Foo.class).orElseThrow());
    assertTrue(EnumMapper.mapToEnum(null, Foo.class).isEmpty());
    assertTrue(EnumMapper.mapToEnum("not_valid_enum_value", Foo.class).isEmpty());
  }

  @Test
  void mapToEnum2() {
    assertEquals(Foo.Bar, EnumMapper.mapToEnum2("BAR", Foo.class).orElseThrow());
    assertEquals(Foo.BOO_BOO, EnumMapper.mapToEnum2("boo-boo", Foo.class).orElseThrow());
    assertEquals(Foo.BOO_BOO, EnumMapper.mapToEnum2("BOO_BOO", Foo.class).orElseThrow());
    assertTrue(EnumMapper.mapToEnum(null, Foo.class).isEmpty());
    assertTrue(EnumMapper.mapToEnum("not_valid_enum_value", Foo.class).isEmpty());
  }

  @Test
  void testToString() {
    assertEquals("bar", EnumMapper.toString(Foo.Bar));
    assertEquals("boo-boo", EnumMapper.toString(Foo.BOO_BOO));
  }

  @Test
  void docEnumValueList() {
    assertEquals(
      """
       - `bar` This is Bar
       - `boo-boo` This is Boo
         Boo
      """,
      EnumMapper.docEnumValueList(Foo.values())
    );
  }

  enum Foo implements DocumentedEnum<Foo> {
    Bar("This is Bar"),
    BOO_BOO(
      """
      This is Boo
      Boo"""
    );

    private final String doc;

    Foo(String doc) {
      this.doc = doc;
    }

    @Override
    public String typeDescription() {
      return DESCRIPTION_OF_THE_TYPE;
    }

    @Override
    public String enumValueDescription() {
      return doc;
    }
  }
}
