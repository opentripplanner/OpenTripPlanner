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

package org.opentripplanner.routing.services;

import java.io.Serializable;
import java.util.Properties;

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.routing.graph.Graph;

/**
 * Optional service that can be attached to a graph. It contains embedded configuration data
 * (Properties) that can be bundled inside the graph itself during building stage. That allow to
 * decorate a graph with pre-configured real-time connectors when using it.
 * 
 */
public class EmbeddedConfigService implements Serializable {
    private static final long serialVersionUID = 7732654140151463606L;

    /** The graph this service is attached to. */
    @Setter
    private Graph graph;

    @Getter
    @Setter
    private Properties properties;
    
}
