package org.opentripplanner.api.model.alertpatch;

import java.util.Date;

public class ApiAlert {
    public String alertHeaderText;
    public String alertDescriptionText;
    public String alertUrl;
    /** null means unknown */
    public Date effectiveStartDate;
}
