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

package org.opentripplanner.routing.impl;

import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.routing.core.Graph;
import org.springframework.beans.factory.config.AbstractFactoryBean;

public class SerializedGraphFactoryBean extends AbstractFactoryBean<Graph> {

    private GraphBundle _graphBundle;

    public void setGraphBundle(GraphBundle graphBundle) {
        _graphBundle = graphBundle;
    }

    @Override
    public Class<?> getObjectType() {
        return Graph.class;
    }

    @Override
    protected Graph createInstance() throws Exception {
        return Graph.load(_graphBundle.getGraphPath(), Graph.LoadLevel.FULL);
    }

}
