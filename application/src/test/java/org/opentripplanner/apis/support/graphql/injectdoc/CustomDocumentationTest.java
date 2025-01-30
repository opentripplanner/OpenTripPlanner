package org.opentripplanner.apis.support.graphql.injectdoc;

import static java.util.Optional.empty;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CustomDocumentationTest {

  private static final String ORIGINAL_DOC = "Original";

  // We use a HashMap to allow inserting 'null' values
  private static final Map<String, String> PROPERTIES = new HashMap<>(Map.ofEntries());

  static {
    PROPERTIES.put("Type1.description", "Doc 1");
    PROPERTIES.put("Type2.description.append", "Doc 2");
    PROPERTIES.put("Type3.description", null);
    PROPERTIES.put("Type.field1.description", "Doc f1");
    PROPERTIES.put("Type.field2.deprecated", "Deprecated f2");
    PROPERTIES.put("Type.field3.description.append", "Doc f3");
    PROPERTIES.put("Type.field4.deprecated.append", "Deprecated f4");
    PROPERTIES.put("Type.field5.description", null);
  }

  private final CustomDocumentation subject = new CustomDocumentation(PROPERTIES);

  @Test
  void testCreate() {
    var defaultDoc = CustomDocumentation.of(ApiDocumentationProfile.DEFAULT);
    assertTrue(defaultDoc.isEmpty());

    var enturDoc = CustomDocumentation.of(ApiDocumentationProfile.ENTUR);
    assertFalse(enturDoc.isEmpty());
  }

  @Test
  void testTypeDescriptionWithUnknownKey() {
    assertEquals(empty(), subject.typeDescription("", ORIGINAL_DOC));
    assertEquals(empty(), subject.typeDescription("ANY_KEY", ORIGINAL_DOC));
    assertEquals(empty(), subject.typeDescription("ANY_KEY", null));
  }

  @Test
  void testTypeDescription() {
    assertEquals(Optional.of("Doc 1"), subject.typeDescription("Type1", ORIGINAL_DOC));
    assertEquals(
      Optional.of(ORIGINAL_DOC + "\n\nDoc 2"),
      subject.typeDescription("Type2", ORIGINAL_DOC)
    );
    assertEquals(Optional.empty(), subject.typeDescription("Type3", ORIGINAL_DOC));
  }

  @Test
  void testFieldDescription() {
    assertEquals(Optional.of("Doc f1"), subject.fieldDescription("Type", "field1", ORIGINAL_DOC));
    assertEquals(
      Optional.of("Deprecated f2"),
      subject.fieldDeprecatedReason("Type", "field2", ORIGINAL_DOC)
    );
    assertEquals(
      Optional.of("Original\n\nDoc f3"),
      subject.fieldDescription("Type", "field3", ORIGINAL_DOC)
    );
    assertEquals(
      Optional.of("Original\n\nDeprecated f4"),
      subject.fieldDeprecatedReason("Type", "field4", ORIGINAL_DOC)
    );
    assertEquals(Optional.empty(), subject.fieldDeprecatedReason("Type", "field5", ORIGINAL_DOC));
  }
}
