/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.analyst.request;

import org.opentripplanner.api.parameter.Layer;
import org.opentripplanner.api.parameter.MIMEImageFormat;
import org.opentripplanner.api.parameter.Style;

public class RenderRequest {

    public final MIMEImageFormat format; 
    public final Layer layer; 
    public final Style style; 
    public final boolean transparent;
    public final boolean timestamp;
    
    public RenderRequest (MIMEImageFormat format, 
        Layer layer, Style style, boolean transparent, boolean timestamp) {
        this.format = format;
        this.layer = layer;
        this.style = style;
        this.transparent = transparent;
        this.timestamp = timestamp;
    }
    
    public String toString() {
        return String.format("<render request format=%s layer=%s style=%s>", 
                format, layer, style);
    }

}
