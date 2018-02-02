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

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class SingleOptionStrategy<T> implements ConsequencesStrategy {

    private Consumer<T> setter;

    private T oldValue;

    public SingleOptionStrategy(Supplier<T> getter, Consumer<T> setter, T valueToSet){
        this.setter = setter;
        oldValue = getter.get();
        setter.accept(valueToSet);
    }

    @Override
    public boolean hasAnotherStrategy() {
        return false;
    }

    @Override
    public void postprocess() {
        setter.accept(oldValue);
    }

    public T getOldValue() {
        return oldValue;
    }
}
