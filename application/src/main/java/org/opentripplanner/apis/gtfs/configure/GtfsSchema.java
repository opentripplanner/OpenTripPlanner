package org.opentripplanner.apis.gtfs.configure;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import javax.inject.Qualifier;

/**
 * This is used for dagger injection. Since we have multiple GraphQL APIs and therefore also
 * multiple GraphQL schemas, a qualifier is needed to tell dagger wich schema to inject where.
 */
@Qualifier
@Retention(RetentionPolicy.RUNTIME)
public @interface GtfsSchema {
}
