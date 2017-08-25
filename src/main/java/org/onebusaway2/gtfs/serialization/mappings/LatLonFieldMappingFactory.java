/**
 * Copyright (C) 2012 Google, Inc.
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
package org.onebusaway.gtfs.serialization.mappings;

import java.util.Locale;

import org.onebusaway.csv_entities.schema.DecimalFieldMappingFactory;

public class LatLonFieldMappingFactory extends DecimalFieldMappingFactory {
  public LatLonFieldMappingFactory() {
    /**
     * We override the default locale to en_US so that we always use "." as the
     * decimal separator, even in locales that default to using "," insteaad.
     */
    super("0.000000", Locale.US);
  }
}
