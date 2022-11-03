package org.opentripplanner.generate.doc.framework;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Define a few constants used by most of the doc generating tests.
 */
public interface DocsTestConstants {
  Logger LOG = LoggerFactory.getLogger(DocsTestConstants.class);
  File TEMPLATE_ROOT = new File("doc-templates");
  File DOCS_ROOT = new File("docs");

  /**
   * This method return {@code true} if the /docs directory is available. If not, a warning is
   * logged and the method returns {@code false}. This is used by the {@link OnlyIfDocsExist}
   * annotation.
   */
  static boolean docsExistOrWarn() {
    if (DOCS_ROOT.exists()) {
      return true;
    }
    LOG.warn(
      """
      SKIP TEST - '/docs' NOT FOUND
      
          The docs/doc-templates directory might not be available if you run the tests outside the
          root of the projects. This may happen if the project root is not the working directory,
          if you run tests using jar files or in a Maven multi-module project.
          
      """
    );
    return false;
  }
}
