package org.opentripplanner.service.osminfo.model;

import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.i18n.I18NString;

public record Platform(I18NString name, LineString geometry, Set<String> references) {}
