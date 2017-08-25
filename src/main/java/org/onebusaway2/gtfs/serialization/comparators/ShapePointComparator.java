/**
 * Copyright (C) 2011 Brian Ferris <bdferris@onebusaway.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onebusaway.gtfs.serialization.comparators;

import java.util.Comparator;

import org.onebusaway.gtfs.model.ShapePoint;

public class ShapePointComparator implements Comparator<ShapePoint> {

  @Override
  public int compare(ShapePoint o1, ShapePoint o2) {
    
    int c = o1.getShapeId().compareTo(o2.getShapeId());

    if (c == 0)
      c = o1.getSequence() - o2.getSequence();

    return c;
  }

}
