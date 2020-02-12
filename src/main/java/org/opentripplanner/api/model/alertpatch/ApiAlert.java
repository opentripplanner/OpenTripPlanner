package org.opentripplanner.api.model.alertpatch;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Date;

@XmlRootElement(name = "Alert")
public class ApiAlert {

    @XmlAttribute
    @JsonSerialize
    public String alertHeaderText;

    @XmlAttribute
    @JsonSerialize
    public String alertDescriptionText;

    @XmlAttribute
    @JsonSerialize
    public String alertUrl;

    //null means unknown
    @XmlElement
    @JsonSerialize
    public Date effectiveStartDate;

}
