package org.opentripplanner._support.arch;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Collection;

public interface ArchComponent {
  /**
   * ArchUnit cached set of classes in OTP. It takes a bit of time to build the set of
   * classes, so it is nice to avoid this for every test. ArchUnit also support JUnit5
   * with a @ArchTest annotation which cache and inject the classes, but the test become
   * slightly more complex by using it and chasing it here works fine.
   */
  JavaClasses OTP_CLASSES = new ClassFileImporter()
    .withImportOption(new ImportOption.DoNotIncludeTests())
    .importPackages("org.opentripplanner");

  String LOG_FRAMEWORK_NAME = "org.slf4j";

  /**
   * All Java packages in {@code java.*} and {@code javax.*}
   */
  Module JAVA_PACKAGES = Module.of(
    Package.of("java.."),
    Package.of("javax.(*).."),
    Package.of("jakarta.(*)..")
  );
  Module LOG_FRAMEWORK = Module.of(Package.of(LOG_FRAMEWORK_NAME));

  Collection<Package> packages();

  default Collection<String> packageIdentifiers() {
    return packages().stream().map(Package::packageIdentifier).toList();
  }
}
