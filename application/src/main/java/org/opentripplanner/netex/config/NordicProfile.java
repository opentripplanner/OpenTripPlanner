package org.opentripplanner.netex.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// A marker annotation for code specific to the
/// [Nordic NeTEx profile](https://entur.atlassian.net/wiki/spaces/PUBLIC/pages/728891481/Nordic%2BNeTEx%2BProfile)
/// (or data).
@Target({ ElementType.TYPE_USE, ElementType.TYPE, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.SOURCE)
public @interface NordicProfile {}
