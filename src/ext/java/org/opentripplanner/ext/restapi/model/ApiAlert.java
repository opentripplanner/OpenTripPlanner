package org.opentripplanner.ext.restapi.model;

import java.util.Date;

public class ApiAlert {

  public String alertHeaderText;
  public String alertDescriptionText;
  public String alertUrl;
  /** null means unknown */
  public Date effectiveStartDate;
  public Date effectiveEndDate;
}
