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
    "/inspector/vectortile/geofencingZones/{z}/{x}/{y}.pbf",

  initialize: function (module) {
    this.module = module;

    this.stopsLookup = {};

    this.layer = VectorTileLayer(this.url, {
      style: (feature) => {
        if(feature.properties.type === "business-area-border") {
          return { stroke: true, color: "#f65173" };
        }
        else if(feature.properties.type === "traversal-banned") {
          return { stroke: true, color: "#62081a" };
        }
        else if(feature.properties.type === "drop-off-banned") {
          return { stroke: true, color: "#ecc029" };
        }
      },
    });

    this.module.webapp.map.layer_control.addOverlay(this.layer, "Geofencing Zones");

  }
});
