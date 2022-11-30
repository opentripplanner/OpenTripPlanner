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

otp.layers.AreaStopsLayer = otp.Class({
  module: null,

  minimumZoomForStops: 15,

  url:
    otp.config.hostname +
    "/" +
    otp.config.restService +
    "/inspector/vectortile/areaStops/{z}/{x}/{y}.pbf",

  initialize: function (module) {
    this.module = module;

    this.stopsLookup = {};

    this.layer = VectorTileLayer(this.url, {
      style: { stroke: true, color: "blue" },
    });

    this.module.webapp.map.layer_control.addOverlay(this.layer, "Area Stops");

    this.layer.bindPopup("");

    this.layer.on("click", (e) => {
      e.originalEvent.preventDefault();
      this.layer.setPopupContent(
        this.getPopupContent({
          ...e.latlng,
          ...e.layer.feature.properties,
        })
      );
    });
  },

  getPopupContent: function (stop) {
    var this_ = this;

    var stop_viewer_trans = _tr("Stop Viewer");
    //TRANSLATORS: Plan Trip [From Stop| To Stop] Used in stoplayer popup
    var plan_trip_trans = _tr("Plan Trip");
    //TRANSLATORS: Plan Trip [From Stop| To Stop] Used in stoplayer popup
    var from_stop_trans = _tr("From Stop");
    //TRANSLATORS: Plan Trip [From Stop| To Stop] Used in stoplayer popup
    var to_stop_trans = _tr("To Stop");
    var routes_stop_trans = _tr("Routes Serving Stop");

    var context = _.clone(stop);
    context.agencyStopLinkText =
      otp.config.agencyStopLinkText || "Agency Stop URL";
    context.stop_viewer = stop_viewer_trans;
    context.routes_on_stop = routes_stop_trans;
    context.plan_trip = plan_trip_trans;
    context.from_stop = from_stop_trans;
    context.to_stop = to_stop_trans;
    var popupContent = ich["otp-stopsLayer-popup"](context);

    popupContent
      .find(".stopViewerLink")
      .data("stop", stop)
      .click(function () {
        var thisStop = $(this).data("stop");
        this_.module.stopViewerWidget.show();
        this_.module.stopViewerWidget.setActiveTime(
          moment().add("hours", -otp.config.timeOffset).unix() * 1000
        );
        this_.module.stopViewerWidget.setStop(thisStop.id, thisStop.name);
        this_.module.stopViewerWidget.bringToFront();
      });

    popupContent
      .find(".planFromLink")
      .data("stop", stop)
      .click(function () {
        var thisStop = $(this).data("stop");
        this_.module.setStartPoint(
          new L.LatLng(thisStop.lat, thisStop.lng),
          false,
          thisStop.name
        );
        this_.module.webapp.map.lmap.closePopup();
      });

    popupContent
      .find(".planToLink")
      .data("stop", stop)
      .click(function () {
        var thisStop = $(this).data("stop");
        this_.module.setEndPoint(
          new L.LatLng(thisStop.lat, thisStop.lng),
          false,
          thisStop.name
        );
        this_.module.webapp.map.lmap.closePopup();
      });

    return popupContent.get(0);
  },
});
