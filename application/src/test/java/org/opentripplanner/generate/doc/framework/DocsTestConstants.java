package org.opentripplanner.generate.doc.framework;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define a few constants used by most of the doc generating tests.
 */
public interface DocsTestConstants {
  Logger LOG = LoggerFactory.getLogger(DocsTestConstants.class);
  File DOC_ROOT = new File("../doc");
  File TEMPLATE_PATH = new File(DOC_ROOT, "templates");
  File USER_DOC_PATH = new File(DOC_ROOT, "user");
  File SANDBOX_USER_DOC_PATH = new File(USER_DOC_PATH, "sandbox");

  /**
   * This method return {@code true} if both the /doc/user and /doc/templates directories are available. If not, a warning is
   * logged and the method returns {@code false}. This is used by the {@link GeneratesDocumentation}
   * annotation.
   */
  static boolean docsExistOrWarn() {
    if (USER_DOC_PATH.exists() && TEMPLATE_PATH.exists()) {
      return true;
    }

    LOG.warn(
      """
      SKIP TEST - '/doc/user' or '/doc/templates' NOT FOUND

          The /doc/user and /doc/templates directories might not be available if you run the tests outside the
          root of the projects. This may happen if the project root is not the working directory,
          if you run tests using jar files or in a Maven multi-module project.

      """
    );
    return false;
  }
}
