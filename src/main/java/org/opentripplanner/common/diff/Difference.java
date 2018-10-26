/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.opentripplanner.common.diff;

public class Difference {

    public DiffType diffType;
    public String property;
    public Object oldValue;
    public Object newValue;


    public Difference(String property, Object oldValue, Object newValue) {
        this.diffType = DiffType.VALUE_CHANGE;
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public Difference(DiffType diffType, String property, Object oldValue, Object newValue) {
        this.diffType = diffType;
        this.property = property;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }


    public String toString() {

        if (DiffType.COLLECTION_ADD.equals(diffType)) {
            return property + ": added " + newValue;
        }

        if (DiffType.COLLECTION_REMOVE.equals(diffType)) {
            return property + ": removed " + oldValue;
        }


        return property + ": " + oldValue + " => " + newValue;
    }

}
