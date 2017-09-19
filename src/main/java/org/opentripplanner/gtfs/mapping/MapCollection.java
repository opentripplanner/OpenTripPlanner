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

package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

class MapCollection {
    static <S, T> Collection<T> mapCollection(Collection<S> entities, Function<S, T> mapper) {
        return entities == null ? null : entities.stream().map(mapper).collect(Collectors.toList());
    }
}
