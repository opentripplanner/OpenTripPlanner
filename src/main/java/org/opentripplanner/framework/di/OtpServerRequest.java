package org.opentripplanner.framework.di;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.inject.Scope;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

/**
 * Tag your Resource @SubComponent (Factory) and the Servies it provides with this annotation.
 * The annotation is used by dagger to create ONE instance per request(per sub-component).
 */
@Documented
@Retention(RUNTIME)
@Scope
public @interface OtpServerRequest {
}
