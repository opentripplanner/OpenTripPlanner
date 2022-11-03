package org.opentripplanner.generate.doc.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * Use this annotation on tests that access the /docs folder outside the source/resource.
 * Accessing files that are not on class-path is error prune and should be avoided. This
 * annotation is used to prevent the test from failing and log a WARNING when the test are run
 * in a different environment.
 * <p>
 * See {@link DocsTestConstants#docsExistOrWarn}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnabledIf("org.opentripplanner.generate.doc.framework.DocsTestConstants#docsExistOrWarn")
public @interface OnlyIfDocsExist {
}
