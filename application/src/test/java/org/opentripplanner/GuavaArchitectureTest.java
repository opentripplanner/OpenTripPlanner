package org.opentripplanner;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.arch.ArchComponent;

/**
 * Restrict the use of the Guava library to an explicit white-list of classes. Guava is a huge
 * library and we want to keep the usage of it to a minimum - prefer the JDK or the OTP utils
 * where they provide an equivalent. If you need another Guava class, add it to the white-list
 * deliberately - do not work around this test.
 */
public class GuavaArchitectureTest {

  private static final String GUAVA_ROOT_PACKAGE = "com.google.common";

  private static final Set<String> WHITE_LISTED_GUAVA_CLASSES = Set.of(
    "com.google.common.annotations.VisibleForTesting",
    "com.google.common.cache.Cache",
    "com.google.common.cache.CacheBuilder",
    "com.google.common.cache.CacheLoader",
    "com.google.common.cache.LoadingCache",
    "com.google.common.collect.ArrayListMultimap",
    "com.google.common.collect.HashMultimap",
    "com.google.common.collect.ImmutableListMultimap",
    "com.google.common.collect.ImmutableMultimap",
    "com.google.common.collect.ImmutableSetMultimap",
    "com.google.common.collect.ImmutableSortedSet",
    "com.google.common.collect.Iterables",
    "com.google.common.collect.LinkedHashMultimap",
    "com.google.common.collect.ListMultimap",
    "com.google.common.collect.MinMaxPriorityQueue",
    "com.google.common.collect.Multimap",
    "com.google.common.collect.MultimapBuilder",
    "com.google.common.collect.Multimaps",
    "com.google.common.collect.Multiset",
    "com.google.common.collect.SetMultimap",
    "com.google.common.collect.Sets",
    "com.google.common.collect.TreeMultiset",
    "com.google.common.hash.HashCode",
    "com.google.common.hash.HashFunction",
    "com.google.common.hash.Hasher",
    "com.google.common.hash.Hashing",
    "com.google.common.html.HtmlEscapers",
    "com.google.common.util.concurrent.MoreExecutors",
    // The classes below are never imported, but the byte-code references them because they are
    // the concrete return types of the white-listed API above (e.g. multimap views and
    // HtmlEscapers.htmlEscaper()).
    "com.google.common.collect.ImmutableCollection",
    "com.google.common.collect.ImmutableList",
    "com.google.common.collect.ImmutableMultiset",
    "com.google.common.collect.ImmutableSet",
    "com.google.common.escape.Escaper"
  );

  private static final DescribedPredicate<JavaClass> A_GUAVA_CLASS_NOT_IN_THE_WHITE_LIST =
    new DescribedPredicate<>("a Guava class not in the white-list") {
      @Override
      public boolean test(JavaClass javaClass) {
        return (
          isInGuava(javaClass) && !WHITE_LISTED_GUAVA_CLASSES.contains(topLevelClassName(javaClass))
        );
      }
    };

  @Test
  void enforceGuavaClassWhiteList() {
    noClasses()
      .should()
      .dependOnClassesThat(A_GUAVA_CLASS_NOT_IN_THE_WHITE_LIST)
      .check(ArchComponent.OTP_CLASSES);
  }

  private static boolean isInGuava(JavaClass javaClass) {
    var packageName = javaClass.getPackageName();
    return (
      packageName.equals(GUAVA_ROOT_PACKAGE) || packageName.startsWith(GUAVA_ROOT_PACKAGE + ".")
    );
  }

  /**
   * Map nested classes (like {@code ImmutableMultimap.Builder}) to their top-level class, so
   * white-listing a class includes its nested types.
   */
  private static String topLevelClassName(JavaClass javaClass) {
    var name = javaClass.getName();
    int index = name.indexOf('$');
    return index < 0 ? name : name.substring(0, index);
  }
}
