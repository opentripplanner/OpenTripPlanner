package org.opentripplanner._support.arch;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.lang.ArchRule;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("UnusedReturnValue")
public class Package implements ArchComponent {

  private final String packageIdentifier;
  private final Set<Package> allowedPackages = new HashSet<>();

  private Package(String packageIdentifier) {
    this.packageIdentifier = packageIdentifier;
  }

  public static Package of(String packageIdentifier) {
    return new Package(packageIdentifier);
  }

  public Package subPackage(String packageIdentifier) {
    return new Package(this.packageIdentifier + "." + packageIdentifier);
  }

  public String packageIdentifier() {
    return packageIdentifier;
  }

  public String packageIdentifierAllSubPackages() {
    return packageIdentifier + ".(**)";
  }

  public Package dependsOn(ArchComponent... allowedDependencies) {
    for (ArchComponent it : allowedDependencies) {
      this.allowedPackages.addAll(it.packages());
    }
    return this;
  }

  @Override
  public Set<Package> packages() {
    return Set.of(this);
  }

  public Package verify() {
    ArchRule rule = classes()
      .that()
      .resideInAPackage(packageIdentifier)
      .and()
      .doNotImplement(dagger.internal.Factory.class)
      .should()
      .onlyDependOnClassesThat()
      .resideInAnyPackage(allAllowedPackages());

    rule.check(OTP_CLASSES);
    return this;
  }

  /**
   * This include the allowed packages plus all globally allowed packages like:
   * <ul>
   *   <li>Java framework</li>
   *   <li>Log framework</li>
   *   <li>Arrays (in package "")</li>
   * </ul>
   */
  private String[] allAllowedPackages() {
    List<String> all = new ArrayList<>();

    for (Package p : allowedPackages) {
      all.add(p.packageIdentifier());
    }
    // Allow all packages to depend on Java
    for (Package p : JAVA_PACKAGES.packages()) {
      all.add(p.packageIdentifier());
    }
    // Allow all packages to depend on the log framework
    for (Package p : LOG_FRAMEWORK.packages()) {
      all.add(p.packageIdentifier());
    }
    // Allow all packages to depend on itself
    all.add(packageIdentifier);

    // Allow all packages to depend on array types in package "" (empty)
    all.add("");

    return all.toArray(String[]::new);
  }
}
