/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.

   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.

   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

otp.namespace("otp.layers");

otp.layers.GeofencingZonesLayer = otp.Class({
  module: null,

  url:
      otp.config.hostname +
      "/" +
      otp.config.restService +
      "/inspector/vectortile/geofencingZones/tilejson.json",

  initialize: function (module) {
    this.module = module;

    this.stopsLookup = {};

    this.layer = L.maplibreGL({
      style: {
        "version": 8,
        "name": "Geofencing zones",
        "metadata": {},
        "sources": {
          "vertices": {
            "type": "vector",
            "url": this.url
          }
        },
        "layers": [
          {
            "id": "business-area-border",
            "type": "circle",
            "source": "vertices",
            "source-layer": "geofencingZones",
            "filter": ["==", "type", "business-area-border"],
            "paint": {
              "circle-radius": {
                "base": 1.1,
                "stops": [
                  [
                    6,
                    0.5
                  ],
                  [
                    17,
                    4
                  ]
                ]
              },
              "circle-color": "#f65173",
              "circle-opacity": 0.9
            }
          },
          {
            "id": "drop-off-banned",
            "type": "circle",
            "source": "vertices",
            "source-layer": "geofencingZones",
            "filter": ["==", "type", "drop-off-banned"],
            "paint": {
              "circle-radius": {
                "base": 1.1,
                "stops": [
                  [
                    6,
                    0.5
                  ],
                  [
                    17,
                    4
                  ]
                ]
              },
              "circle-color": "#cbaf3c",
              "circle-opacity": 0.9
            }
          },
          {
            "id": "traversal-banned",
            "type": "circle",
            "source": "vertices",
            "source-layer": "geofencingZones",
            "filter": ["==", "type", "traversal-banned"],
            "paint": {
              "circle-radius": {
                "base": 1.1,
                "stops": [
                  [
                    6,
                    0.5
                  ],
                  [
                    17,
                    4
                  ]
                ]
              },
              "circle-color": "#62081a",
              "circle-opacity": 0.9
            }
          },
        ]
      }
    });

    this.module.webapp.map.layer_control.addOverlay(this.layer, "Geofencing Zones");

  }
});
