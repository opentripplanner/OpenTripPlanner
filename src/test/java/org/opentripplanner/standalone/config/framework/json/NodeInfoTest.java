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
  private final NodeInfo subject = NodeInfo
    .of()
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
    assertFalse(createBuilderStringNode().build().printDetails());
    assertTrue(createBuilderStringNode().withDescription("D").build().printDetails());
    assertTrue(createBuilderStringNode().withArray(ConfigType.STRING).build().printDetails());
    assertTrue(createBuilderStringNode().withMap(ConfigType.DURATION).build().printDetails());
  }

  @Test
  void testToString() {
    assertEquals("Name : string = \"Default-Value\" Since 2.2", subject.toString());
  }

  @Test
  void compareTo() {}

  private NodeInfoBuilder createBuilderStringNode() {
    return NodeInfo
      .of()
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
