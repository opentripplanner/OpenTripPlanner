/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General private License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General private License for more details.

 You should have received a copy of the GNU General private License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import org.springframework.stereotype.Component;

/**
 * A trip planning request configurable via Spring and intended for cloning
 */
@Component
public class PrototypeRoutingRequest extends RoutingRequest {
    private static final long serialVersionUID = 1L;

    private PrototypeRoutingRequest() {
        super();
    }

    private PrototypeRoutingRequest(TraverseModeSet modes) {
        super(modes);
    }

    private PrototypeRoutingRequest(TraverseMode mode) {
        super(mode);
    }

    private PrototypeRoutingRequest(TraverseMode mode, OptimizeType optimize) {
        super(mode, optimize);
    }

    private PrototypeRoutingRequest(TraverseModeSet modeSet, OptimizeType optimize) {
        super(modeSet, optimize);
    }

    public static PrototypeRoutingRequest getInstance() {
        return new PrototypeRoutingRequest();
    }
}
