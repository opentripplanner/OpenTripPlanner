package org.opentripplanner.generate.doc.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Use this annotation on tests that generate(access) the /doc directory outside the
 * source/resource.
 * <p>
 * All tests annotated with this annotation is tagged with "docs". You may include or exclude
 * these when running the tests. To only run doc generation use:
 * <pre>
 *   mvn test -Dgroups=docs
 * </pre>
 * <p>
 * Accessing files that are not on class-path is error prune and should be avoided. This
 * annotation will prevent the test from failing and only log a WARNING if the test is run
 * in a different environment.
 * <p>
 * See {@link DocsTestConstants#docsExistOrWarn}
 */
@Tag("docs")
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnabledIf("org.opentripplanner.generate.doc.framework.DocsTestConstants#docsExistOrWarn")
public @interface GeneratesDocumentation {
}
