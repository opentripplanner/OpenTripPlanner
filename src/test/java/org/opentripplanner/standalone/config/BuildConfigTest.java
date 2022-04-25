package org.opentripplanner.standalone.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.JsonSupport.jsonNodeForTest;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Set;
import org.junit.jupiter.api.Test;

public class BuildConfigTest {

  @Test
  public void testParsePeriodDate() {
    // Given
    JsonNode node = jsonNodeForTest("{ 'parentStopLinking' : true }");

    BuildConfig subject = new BuildConfig(node, "Test", false);

    // Then
    assertTrue(subject.parentStopLinking);
  }

  @Test
  public void boardingLocationRefs() {
    var node = jsonNodeForTest("{ 'boardingLocationTags' : ['a-ha', 'royksopp'] }");

    var subject = new BuildConfig(node, "Test", false);

    assertEquals(Set.of("a-ha", "royksopp"), subject.boardingLocationTags);
  }
}
