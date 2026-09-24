package org.opentripplanner.netex.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/// A marker annotation for code specific to the
/// [EPIP NeTEx profile](http://netex.uk/netex/doc/2019.05.07-v1.1_FinalDraft/prCEN_TS_16614-PI_Profile_FV_%28E%29-2019-Final-Draft-v3.pdf)
/// (or data).
@Target({ ElementType.TYPE_USE, ElementType.TYPE, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.SOURCE)
public @interface EPIP {}
