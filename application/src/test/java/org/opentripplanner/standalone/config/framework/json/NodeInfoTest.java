package org.opentripplanner.standalone.config.framework.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NodeInfoTest {

  private static final String NAME = "Name";
  private static final ConfigType TYPE = ConfigType.STRING;
  private static final String DEFAULT_VALUE = "Default-Value";
  private static final OtpVersion SINCE = OtpVersion.V2_2;
  private static final String SUMMARY = "Summary";
  private static final String DESCRIPTION = "Description";
  private final NodeInfo subject = NodeInfo.of()
    .withName(NAME)
    .withType(TYPE)
    .withOptional(DEFAULT_VALUE)
    .withSince(SINCE)
    .withSummary(SUMMARY)
    .withDescription(DESCRIPTION)
    .build();

  @Test
  void ofSkipChild() {
    var subject = NodeInfo.ofSkipChild("Name");
    assertFalse(subject.printDetails());
    assertEquals("Name", subject.name());
    assertTrue(subject.skipChild());
    assertEquals("Name : object Optional Since na", subject.toString());
  }

  @Test
  void printDetails() {
    assertFalse(createBuilder().build().printDetails());
    assertTrue(createBuilder().withDescription("D").build().printDetails());
    assertTrue(createBuilder().withArray(ConfigType.STRING).build().printDetails());
    assertTrue(createBuilder().withMap(ConfigType.DURATION).build().printDetails());
  }

  @Test
  void since() {
    assertEquals(OtpVersion.V2_2, subject.since());
  }

  @Test
  void testToString() {
    assertEquals("Name : string = \"Default-Value\" Since 2.2", subject.toString());
  }

  @Test
  void typeDescription() {
    for (ConfigType configType : ConfigType.values()) {
      if (configType.isSimple() && ConfigType.ENUM != configType) {
        assertEquals(
          configType.docName(),
          createBuilder().withType(configType).build().typeDescription()
        );
      }
    }
    assertEquals("enum", createBuilder().withEnum(AnEnum.class).build().typeDescription());
    assertEquals("object", createBuilder().withType(ConfigType.OBJECT).build().typeDescription());
    assertEquals(
      "string[]",
      createBuilder().withArray(ConfigType.STRING).build().typeDescription()
    );
    assertEquals(
      "map of duration",
      createBuilder().withMap(ConfigType.DURATION).build().typeDescription()
    );
    assertEquals(
      "enum map of object",
      createBuilder().withEnumMap(AnEnum.class, ConfigType.OBJECT).build().typeDescription()
    );
    assertEquals("enum set", createBuilder().withEnumSet(AnEnum.class).build().typeDescription());
  }

  @Test
  void experimentalFeature() {
    var subject = createBuilder().withExperimentalFeature().build();
    assertEquals(NodeInfo.EXPERIMENTAL_FEATURE, subject.description());

    subject = createBuilder().withDescription("Description").withExperimentalFeature().build();
    assertEquals("Description\n\n" + NodeInfo.EXPERIMENTAL_FEATURE, subject.description());
  }

  private NodeInfoBuilder createBuilder() {
    return NodeInfo.of()
      .withType(ConfigType.STRING)
      .withName(NAME)
      .withSummary(SUMMARY)
      .withOptional()
      .withSince(SINCE);
  }

  enum AnEnum {
    A,
    B,
  }
}
