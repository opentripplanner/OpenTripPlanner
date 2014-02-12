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

package org.opentripplanner.api.parameter;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

public class StyleList {
    List<Style> styles = new ArrayList<Style>(); 

    public StyleList(String v) {
        for (String s : v.split(",")) {
            if (s.isEmpty())
                s = "COLOR30";
            if (s.toUpperCase().equals("GREY"))
                s = "GRAY";
            try {
                styles.add(Style.valueOf(s.toUpperCase()));
            } catch (Exception e) {
                throw new WebApplicationException(Response
                    .status(Status.BAD_REQUEST)
                    .entity("unknown layer style: " + s)
                    .build());
            }
        }
    }

    public Style get(int index) {
        return styles.get(index);
    }
}
