package org.opentripplanner.generate.doc.framework;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.condition.EnabledIf;

/**
 * See {@link DocsTestConstants#docsExistOrWarn}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnabledIf("org.opentripplanner.generate.doc.framework.DocsTestConstants#docsExistOrWarn")
public @interface OnlyIfDocsExist {
}
