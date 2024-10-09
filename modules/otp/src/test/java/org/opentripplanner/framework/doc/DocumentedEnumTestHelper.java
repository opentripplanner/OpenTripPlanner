package org.opentripplanner.framework.doc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class DocumentedEnumTestHelper {

  public static void verifyHasDocumentation(DocumentedEnum[] allDocumentedValues) {
    var typeDoc = allDocumentedValues[0].typeDescription();
    assertFalse(typeDoc.isBlank(), "Type documentation is missing!");

    for (DocumentedEnum it : allDocumentedValues) {
      assertEquals(typeDoc, it.typeDescription());
      assertFalse(it.enumValueDescription().isBlank(), "Documentation for value is missing: " + it);
    }
  }
}
