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

otp.namespace("otp.util");

/**
 * some OpenLayers global configuration
 */
try 
{
    /** 
     * OpenLayers and other OpenLayers classes are used statically in and around the eAPI code.
     * To allow eAPI to run w/out OpenLayers, the following stubs will prevent failures of eAPI
     * creation & loading...
     */
    if(window.OpenLayers == undefined) 
    {
        OpenLayers = {};
        var names = ["Size", "Pixel", "Layer", "Layer.WMS", "Control", "Control.SelectFeature", "Popup", "Popup.FramedCloud"];
        for (var i = 0; i < names.length; ++i) 
            window.OpenLayers[names[i]] = function()
            {
            };
    }
    
    // WMS -- I avoid pink tiles, baby
    OpenLayers.IMAGE_RELOAD_ATTEMPTS = 5;
    OpenLayers.Util.onImageLoadErrorColor = "";
    OpenLayers.ImgPath = "images/map/controls/";
} 
catch (e) 
{
    console.log("no worries if this exception is thrown...just means that OpenLayers is not included in your html file (eg: maybe this is not an OL project)")
}
/**
  * Web Map / TripPlanner
  * @class 
  */
otp.util.OpenLayersUtils = {
    MAP_PANEL       : 'map-panel',

    /**
     * static function to make the OpenLayers map object
     *   example: makeMap();
     */
    makeMap : function(controls, epsg, div, numZoomLevels, units,  maxExtent, maxResolution, displayProjection)
    {
        var map = null;

        var options = {
            displayProjection: displayProjection,
            projection: new OpenLayers.Projection(epsg),
            controls: controls
        };

        if(units != null)
            options.units = units;
        if(maxExtent != null)
            options.maxExtent = maxExtent;
        if(maxResolution != null)
            options.maxResolution = maxResolution;
        if(numZoomLevels && numZoomLevels > 0)
            options.numZoomLevels = numZoomLevels;

        map = new OpenLayers.Map(div, options);

        return map;
    },

    /** */
    makeMapBaseLayer : function(map, urls, layer, tileBuffer, transitionEffect, attribution)
    {
        var layer = new OpenLayers.Layer.WMS("Map", urls, {
                layers : layer,
                format : 'image/png'
            }, {
                buffer : tileBuffer,
                isBaseLayer : true,
                transitionEffect : transitionEffect,
                attribution : attribution,
                size : new OpenLayers.Size(512, 512)
            }
        );
        map.addLayer(layer);

        return layer;
    },

    /**
     * static routine that adds controls to a map 
     */
    defaultControls : function(map, doZoomWheel, doRightClicks, doPermaLink, doAttribution, doHistory)
    {
        var retVal = {
            pan   : new OpenLayers.Control.PanZoomBar({zoomWorldIcon:true}),
            mouse : new OpenLayers.Control.MousePosition({numDigits: 4}),
            scale : new OpenLayers.Control.ScaleLine(),
            arg   : new OpenLayers.Control.ArgParser(),
            nav   : new OpenLayers.Control.Navigation({zoomWheelEnabled : doZoomWheel, handleRightClicks : doRightClicks})
        };

        map.addControl(retVal.pan   );
        map.addControl(retVal.mouse );
        map.addControl(retVal.scale );
        map.addControl(retVal.arg   );
        map.addControl(retVal.nav   );

        if(doPermaLink)
        {
            var p = new OpenLayers.Control.Permalink();
            retVal.perma = p;
            map.addControl(p);
        }

        if(doAttribution)
        {
            var a = new OpenLayers.Control.Attribution();
            retVal.attrib = a;
            map.addControl(a);
        }

        if(doHistory)
        {
            var h = new OpenLayers.Control.NavigationHistory();
            retVal.hist = h;
            map.addControl(h);
        }

        return retVal;
    },



    ///////////// VECTOR UTILS ///////////// VECTOR UTILS ///////////// VECTOR UTILS ///////////// VECTOR UTILS /////////////
    BLACK_STYLE : {
        strokeColor: "#111111",
        strokeOpacity: 0.5,
        strokeWidth: 5,
        pointRadius: 6,
        pointerEvents: "visiblePainted"
    },
    BLACK_DASH_STYLE : {
        strokeColor: "#111111",
        strokeOpacity: 0.8,
        strokeWidth:   3,
        pointRadius:   6,
        strokeDashstyle: "dash",
        pointerEvents: "visiblePainted"
    },
    RED_STYLE : {
        strokeColor: "#FF0000",
        strokeOpacity: 0.7,
        strokeWidth: 6,
        pointRadius: 6,
        pointerEvents: "visiblePainted"
    },
    RED_DASH_STYLE : {
        strokeColor: "#FF0000",
        strokeOpacity: 0.7,
        strokeWidth: 6,
        pointRadius: 6,
        strokeDashstyle: "dash",
        pointerEvents: "visiblePainted"
    },

    /**
     * 
     */
    makeDefaultPointFeatureStyle : function()
    {
        return new OpenLayers.StyleMap({
                "default": new OpenLayers.Style({
                    pointRadius: "6", // old: ${type} sized according to type attribute
                    fillColor:   "#ffcc66",
                    strokeColor: "#66ccDD",
                    strokeWidth: 2
                }),
                "select": new OpenLayers.Style({
                    fillColor:   "#FE7202",
                    strokeColor: "#3B5A95",
                    strokeWidth: 2
                })
            });
    },

    /**
     * Makes a straight line from <code>from</code> to <code>to</code>, returning the resulting Vector Feature
     * 
     * @param {Object} from
     * @param {Object} to
     * @param {Object} style
     * 
     * @return {OpenLayers.Feature.Vector}
     */
    makeStraightLine : function(from, to, style)
    {
        var retVal = null;
        
        try 
        {
            if(style == null)
               style = this.BLACK_DASH_STYLE;
    
            retVal = new OpenLayers.Feature.Vector(
                        new OpenLayers.Geometry.LineString([
                            new OpenLayers.Geometry.Point(from.get('x'), from.get('y')), 
                            new OpenLayers.Geometry.Point(to.get('x'),   to.get('y'))
                        ]),
                    null,
                    style
            );
        }
        catch (err) 
        {
        }

        return retVal;
    },


    /**
     * Makes a line from a list of points, returning the resulting Vector Feature
     * 
     * @param {Object} pointList
     * @param {Object} style
     * 
     * @return {OpenLayers.Feature.Vector}
     */
    makeLineFromPoints : function(pointList, style)
    {
        var retVal = null;
        
        try 
        {
            if (style == null) 
                style = this.RED_STYLE;

            var line = [];
            for (var i in pointList) 
            {
                var x = pointList[i].x;
                var y = pointList[i].y;
                if (x == null || y == null || x <= 0 || y <= 0) 
                    continue;

                var newPoint = new OpenLayers.Geometry.Point(x, y);
                if(newPoint != null)
                    line.push(newPoint);
            }

            if (line != null && line.length > 1) 
            {
                var ls = new OpenLayers.Geometry.LineString(line);
                retVal = new OpenLayers.Feature.Vector(ls, null, style);
            }
        } 
        catch (err) 
        {
        }

        return retVal;
    },

    /**
     * Will grab a line geometry from AJAX
     * 
     * @param {Object} url
     * @param {Object} vLayer OpenLayers.Layer.Vector
     * @param {Object} style
     * @param {Object} itin an object with a method itin.concatVectors(vectors), where the vectors can be given back to the caller
     */
    drawLinesViaAjax : function(url, vLayer, style, itin)
    {
        try
        {
            OpenLayers.Request.GET({
                url: url,
                success: function(xhr) 
                {
                    var vectors = new Array();
                    var resp    = eval('(' + xhr.responseText + ')');
                    for(var i in resp.results) 
                    {
                        try
                        {
                            var points = resp.results[i].points;
                            var line   = otp.util.OpenLayersUtils.makeLineFromPoints(points, style);
                            if(line != null)
                                vectors.push(line);
                        }
                        catch(Exe)
                        {
                        }
                    }
    
                    if(vectors.length > 0)
                    {
                        // adds the vector lines to the Itinerary object (to be displayed
                        if(itin)
                            itin.concatVectors(vectors);
                            
                        // adds the vector lines to the map layer
                        if(vLayer)
                            vLayer.addFeatures(vectors);
                    }          
                },
                failure: function(xhr) {
                    //prompt(xhr.status);
                }
            });
        }
        catch(Exe)
        {
        }
    },

    ///////////// MARKER UTILS ///////////// MARKER UTILS ///////////// MARKER UTILS ///////////// MARKER UTILS /////////////

    RTE_ICON_SIZE   : new OpenLayers.Size(105, 34),
    RTE_ICON_OFFSET : new OpenLayers.Pixel(0, -34),  
    ST_END_SIZE     : new OpenLayers.Size(21, 39),
    ST_END_OFFSET   : new OpenLayers.Pixel(-10, -39),
    DISK_SIZE       : new OpenLayers.Size(10, 10),
    DISK_OFFSET     : new OpenLayers.Pixel(-5, -5),
    
//    useCustomIconsForAgencies : ['MTA NYCT'],
    useCustomIconsForAgencies: [],

    markerGraphicMapping  : {
        walkMarker: 'images/map/trip/mode/walk.png',
        bicycleMarker: 'images/map/trip/mode/bicycle.png',
        fromWalkMarker: 'images/map/trip/start-walk.png',
        fromBicycleMarker: 'images/map/trip/start-bicycle.png',
        toMarker: 'images/map/trip/end.png',
        fromMarker: 'images/map/trip/start.png',
        diskMarker: 'images/map/trip/xferdisk.png',
        routeMarker: function(feature) {
            var imagePathOptions = Ext.apply({}, {imageType: 'marker'}, feature.attributes);
            return otp.util.imagePathManager.imagePath(imagePathOptions);
        }
    },

    getMarkerStyle: function() {
        var template = {
            externalGraphic: "${getExternalGraphic}",
            graphicOpacity: 0.92
        };
        var graphicMapping = this.markerGraphicMapping;
        var olutils = this;
        var context = {
            getExternalGraphic: function(feature) {
                var externalGraphic = graphicMapping[feature.attributes.type];
                return typeof externalGraphic === 'function'
                       ? externalGraphic.call(olutils, feature)
                       : externalGraphic;
            }
        };        
        var style = new OpenLayers.Style(template, {context: context});
        return style;
    },

    getMarkerUniqueValueRules: function() {
        return {
            walkMarker: {
                graphicWidth: this.RTE_ICON_SIZE.w,
                graphicHeight: this.RTE_ICON_SIZE.h,
                graphicXOffset: this.RTE_ICON_OFFSET.x,
                graphicYOffset: this.RTE_ICON_OFFSET.y
            },
            fromWalkMarker: {
                graphicWidth: this.ST_END_SIZE.w,
                graphicHeight: this.ST_END_SIZE.h,
                graphicXOffset: this.ST_END_OFFSET.x,
                graphicYOffset: this.ST_END_OFFSET.y
            },
            fromBicycleMarker: {
                graphicWidth: this.ST_END_SIZE.w,
                graphicHeight: this.ST_END_SIZE.h,
                graphicXOffset: this.ST_END_OFFSET.x,
                graphicYOffset: this.ST_END_OFFSET.y
            },
            toMarker: {
                graphicWidth: this.ST_END_SIZE.w,
                graphicHeight: this.ST_END_SIZE.h,
                graphicXOffset: this.ST_END_OFFSET.x,
                graphicYOffset: this.ST_END_OFFSET.y
            },
            fromMarker: {
                graphicWidth: this.ST_END_SIZE.w,
                graphicHeight: this.ST_END_SIZE.h,
                graphicXOffset: this.ST_END_OFFSET.x,
                graphicYOffset: this.ST_END_OFFSET.y
            },
            diskMarker: {
                graphicWidth: this.DISK_SIZE.w,
                graphicHeight: this.DISK_SIZE.h,
                graphicXOffset: this.DISK_OFFSET.x,
                graphicYOffset: this.DISK_OFFSET.y
            },
            routeMarker: {
                graphicWidth:   this.RTE_ICON_SIZE.w,
                graphicHeight:  this.RTE_ICON_SIZE.h,
                graphicXOffset: this.RTE_ICON_OFFSET.x,
                graphicYOffset: this.RTE_ICON_OFFSET.y
            }
        };
    },


    /**
     * Create a new marker.
     * 
     * @param {Number}
     *            x The horizontal coordinate of the marker.
     * @param {Number}
     *            y The vertical coordinate of the marker.
     * @param {Object}
     *            attributes that will be set on the new marker feature
     */
    makeMarker : function(x, y, attributes)
    {
        var point = new OpenLayers.Geometry.Point(x, y);
        var marker = new OpenLayers.Feature.Vector(point, attributes);
        
        return marker;
    },

    ///////////// ZOOM UTILS ///////////// ZOOM UTILS ///////////// ZOOM UTILS ///////////// ZOOM UTILS /////////////

    /**
     * zoom utility that will zoom into a point at point X & Y, with optional limits to the zoom
     * 
     * @param {Object} map
     * @param {Object} x
     * @param {Object} y
     * @param {Object} z
     * @param {Object} wideZoomLimit
     * @param {Object} closeZoomLimit
     */
    panZoomWithLimits : function(map, x, y, z, wideZoomLimit, closeZoomLimit, prj)
    {
        try 
        {
            if(prj == null)
                prj = otp.core.MapStatic.dataProjection;

            if(z && z >= wideZoomLimit && z <= closeZoomLimit)
            {
                map.zoomTo(z);
            }
            else if (map.getZoom() > closeZoomLimit) 
            {
                map.zoomTo(closeZoomLimit);
            } 
            else if (map.getZoom() < wideZoomLimit)
            {
                map.zoomTo(wideZoomLimit);
            }
            
            if (x && y) 
            {
                map.setCenter((new OpenLayers.LonLat(x, y)).transform(prj, map.getProjectionObject()));
            }
        }
        catch(ex)
        {
        }
    },

    ///////////// MOUSE COORDS ///////////// MOUSE COORDS ///////////// MOUSE COORDS ///////////// MOUSE COORDS /////////////
    
    /** will round the coordinates to just feet -- no decimal part */
    roundCoord : function(coord)
    {
        var retVal = coord;

        try
        {
            if(retVal.lon && retVal.lon > 180)
            {
                retVal.lon = Math.round(retVal.lon);
            }
            if(retVal.lat && retVal.lat > 180)
            {
                retVal.lat = Math.round(retVal.lat);
            }
        }
        catch(e)
        {
            console.log('OpenLayersUtils.getLatLonOfPixel exception ' + e);
           
        }

        return retVal;
    },


    /**
     * given a map, and a pixel coordinate, return back a map coordinate
     */
    getLatLonOfPixel: function(map, pixelX, pixelY, prj)
    {
        try
        {
            if(prj == null)
                prj = otp.core.MapStatic.dataProjection;
        
            var px     = new OpenLayers.Pixel(pixelX, pixelY);
            var lonLat = map.getLonLatFromPixel(px);
            lonLat.transform(map.getProjectionObject(), prj);
            return this.roundCoord(lonLat);
        }
        catch(e)
        {
            console.log('OpenLayersUtils.getLatLonOfPixel exception ' + e)
           
        }
    },

    /**
     * from Wyatt's context menu stuff
     */
    setCenterByPixel: function (center) {
      var px = new OpenLayers.Pixel(center.x, center.y);
      var lon_lat = this.map.getLonLatFromPixel(px);
      this.map.setCenter(lon_lat);
    },

    

    ///////////// MISC UTILS ///////////// MISC UTILS ///////////// MISC UTILS ///////////// MISC UTILS /////////////

    /** */
    setCenter : function(map, x, y, zoom, prj)
    {
        try
        {
            if(prj == null)
                prj = otp.core.MapStatic.dataProjection;

            if (zoom && (zoom < 0 || zoom > 9))
            {
                 zoom = 2;
            }

            if (x && y)
            {
                map.setCenter(new OpenLayers.LonLat(x, y).transform(prj, map.getProjectionObject()), zoom);
            }
            else if (zoom)
            {
                map.zoomTo(zoom);
            }
        }
        catch(e)
        {
            console.log('OpenLayersUtils.setCenter exception ' + e);
        }
    },

   /**
    * Hides the Marker & Feature Layers associated with trip planning
    */
    hideAllLayers : function(layers)
    {
        try
        {
            for(var i in layers)
            {
                if(layers[i] != null && !layers[i].isBaseLayer)
                {
                    layers[i].display(false);
                    layers[i].setVisibility(false);
                }
            }
        }
        catch(Err)
        {
        }
    },

    /**
     * will close (hide) an OL map's popups, and remove it from the map
     *  
     * @param {Object} map
     */
    clearPopups : function(map)
    {
        try
        {
            for (var i = 0; i < map.popups.length; i++) 
            {
                var p = map.popups[i];
                if(p)
                {
                    p.hide();
                    map.removePopup(p);
                }
            }
        }
        catch(e)
        {
            
        }
    },
    
    geo_json_converter: function(n,p) {
    	var formatter = new OpenLayers.Format.GeoJSON();
    	var geoJsonObj = formatter.read(n,'Geometry');
    	return geoJsonObj;
    },
    
    encoded_polyline_converter: function(n,p) {
        
        var lat = 0;
        var lon = 0;

        var strIndex = 0;
        var points = new Array();

        while (strIndex < n.length) {

          var rLat = this.decodeSignedNumberWithIndex(n, strIndex);
          lat = lat + rLat.number * 1e-5;
          strIndex = rLat.index;

          var rLon = this.decodeSignedNumberWithIndex(n, strIndex);
          lon = lon + rLon.number * 1e-5;
          strIndex = rLon.index;

          var p = new OpenLayers.Geometry.Point(lon,lat);
          points.push(p);
        }

        return new OpenLayers.Geometry.LineString(points);
     },
    
    decodeSignedNumber: function(value) {
    	var r = this.decodeSignedNumberWithIndex(value, 0);
        return r.number;
    },
    
    decodeSignedNumberWithIndex: function(value,index) {
        var r = this.decodeNumberWithIndex(value, index);
        var sgn_num = r.number;
        if ((sgn_num & 0x01) > 0) {
          sgn_num = ~(sgn_num);
        }
        r.number = sgn_num >> 1;
        return r;
    },
    
    decodeNumber: function(value) {
        var r = this.decodeNumberWithIndex(value, 0);
        return r.number;
    },

    decodeNumberWithIndex: function(value, index) {

        if (value.length == 0)
        	throw "string is empty";

        var num = 0;
        var v = 0;
        var shift = 0;

        do {
          v1 = value.charCodeAt(index++);
          v = v1 - 63;
          num |= (v & 0x1f) << shift;
          shift += 5;
        } while (v >= 0x20);

        return {"number": num, "index": index};
    },

    CLASS_NAME: "otp.util.OpenLayersUtils"
};
