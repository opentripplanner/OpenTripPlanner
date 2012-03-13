package org.opentripplanner.api.model.analysis;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

public class Annotation {
    @XmlElements(value = @XmlElement(name="object"))
    public List<AnnotationObject> objects;

    @XmlAttribute
    public String annotation;

    public void addObject(AnnotationObject annotationObj) {
        if (objects == null) {
            objects = new ArrayList<AnnotationObject>();
        }
        objects.add(annotationObj);
    }
}
