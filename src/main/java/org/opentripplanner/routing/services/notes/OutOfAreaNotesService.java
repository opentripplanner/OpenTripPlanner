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
package org.opentripplanner.routing.services.notes;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.RoutingRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class OutOfAreaNotesService implements Serializable {

    private static final long serialVersionUID = 1;

    private Geometry area;

    private String outOfAreaMessage;

    public void setArea(Geometry area) {
        this.area = area;
    }

    public void setOutOfAreaMessage(String outOfAreaMessage) {
        this.outOfAreaMessage = outOfAreaMessage;
    }

    public Collection<Alert> getAlerts(RoutingRequest request) {
        Point fromPoint = GeometryUtils.getGeometryFactory().createPoint(request.rctx.fromVertex.getCoordinate());
        Point toPoint = GeometryUtils.getGeometryFactory().createPoint(request.rctx.toVertex.getCoordinate());
        Collection<Alert> alerts = new ArrayList<>();
        if (!area.contains(fromPoint) || !area.contains(toPoint)) {
            alerts.add(Alert.createSimpleAlerts(outOfAreaMessage));
        }
        return alerts;
    }
}
