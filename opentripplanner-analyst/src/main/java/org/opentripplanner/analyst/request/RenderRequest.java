package org.opentripplanner.analyst.request;

import org.opentripplanner.analyst.parameter.Layer;
import org.opentripplanner.analyst.parameter.MIMEImageFormat;
import org.opentripplanner.analyst.parameter.Style;

public class RenderRequest {

    public final MIMEImageFormat format; 
    public final Layer layer; 
    public final Style style; 
    public final boolean transparent;

    public RenderRequest (MIMEImageFormat format, 
        Layer layer, Style style, boolean transparent) {
        this.format = format;
        this.layer = layer;
        this.style = style;
        this.transparent = transparent;
    }
    
    public String toString() {
        return String.format("<render request format=%s layer=%s style=%s>", 
                format, layer, style);
    }

}
