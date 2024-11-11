package org.opentripplanner.street.model.edge;

import java.io.Serializable;
import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;

public record LinearPlatform(I18NString name, LineString geometry, Set<String> references)
  implements Serializable {}
