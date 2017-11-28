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
package org.opentripplanner.routing.consequences;

import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.spt.GraphPath;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MultipleConsequencesStrategy implements ConsequencesStrategy {

    private Iterator<ConsequencesStrategy> strategies;
    private ConsequencesStrategy current;

    public MultipleConsequencesStrategy(RoutingRequest options, List<ConsequencesStrategyFactory> factories) {
        List<ConsequencesStrategy> strategyList = new ArrayList<>();
        for (ConsequencesStrategyFactory factory : factories) {
            ConsequencesStrategy strategy = factory.create(options);
            strategyList.add(strategy);
        }
        this.strategies = strategyList.iterator();
        this.current = strategies.next();
    }

    @Override
    public boolean hasAnotherStrategy() {
        return strategies.hasNext();
    }

    @Override
    public void postprocess() {
        current.postprocess();
        if (strategies.hasNext()) {
            current = strategies.next();
        }
    }

    @Override
    public List<Alert> getConsequences(List<GraphPath> paths) {
        return current.getConsequences(paths);
    }
}
