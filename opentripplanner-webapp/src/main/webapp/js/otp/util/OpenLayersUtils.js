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
    makeMap : function(controls, epsg, div, numZoomLevels, units,  maxExtent, maxResolution)
    {
        var map = null;

        var options = {
            controls:      controls,
            projection:    epsg
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
        var layer  = new OpenLayers.Layer.WMS("Map", urls, {layers: layer, format: 'image/png',  EXCEPTIONS: ''}, {buffer: tileBuffer, isBaseLayer: true, transitionEffect: transitionEffect, attribution: attribution});
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
        }

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
     * makes a straight line from X to Y, returing the resulting Vecotor Feature
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
     * makes a line from a list of points, returing the resulting Vecotor Feature
     * 
     * @param {Object} pointLise
     * @param {Object} 
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
     * will grab a line geom from AJAX
     * 
     * vLayer - OpenLayers Vector Layer
     * itin   - an object with a method itin.concatVectors(vectors), where the vectors can be given back to the caller
     * 
     * @param {Object} url
     * @param {Object} vectorLayer
     * @param {Object} itin
     * @param {Object} style
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

    FROM_WALK_ICON  : 'images/map/trip/start-walk.png',
    FROM_ICON       : 'images/map/trip/start.png',
    TO_ICON         : 'images/map/trip/end.png',
    DISK_ICON       : 'images/map/trip/xferdisk.png',
    WALK_ICON       : 'images/map/trip/mode/walk.png',
    ROUTE_DIR       : 'images/map/trip/rte/',

    ST_END_SIZE     : new OpenLayers.Size(20, 34),
    ST_END_OFFSET   : new OpenLayers.Pixel(-10, -34),
    DISK_SIZE       : new OpenLayers.Size(10, 10),
    DISK_OFFSET     : new OpenLayers.Pixel(-5, -5),


    /** */
    makeFromWalkingMarker : function(x, y, mArray)
    {
        var icon = new OpenLayers.Icon(this.FROM_WALK_ICON, this.ST_END_SIZE, this.ST_END_OFFSET);
        var marker = this.makeMarker(x, y, icon);
        if(mArray)
            mArray.push(marker);
            
        return marker;
    },

    /** */
    makeFromMarker : function(x, y, mArray)
    {
        var icon = new OpenLayers.Icon(this.FROM_ICON, this.ST_END_SIZE, this.ST_END_OFFSET);
        var marker = this.makeMarker(x, y, icon);
        if(mArray)
            mArray.push(marker);
            
        return marker;
    },

    /** */
    makeToMarker : function(x, y, mArray)
    {
        var icon = new OpenLayers.Icon(this.TO_ICON, this.ST_END_SIZE, this.ST_END_OFFSET);
        var marker = this.makeMarker(x, y, icon);
        if(mArray)
            mArray.push(marker);
            
        return marker;
    },


    /** */
    makeWalkMarker : function(x, y, mArray)
    {
        var walk   = new OpenLayers.Icon(this.WALK_ICON, this.RTE_ICON_SIZE, this.RTE_ICON_OFFSET);
        var marker = this.makeMarker(x, y, walk);
        if(mArray)
            mArray.push(marker);
            
        return marker;
    },


    /** */
    makeDiskMarker : function(x, y, mArray)
    {
        var disk   = new OpenLayers.Icon(this.DISK_ICON, this.DISK_SIZE, this.DISK_OFFSET);
        var circle = this.makeMarker(x, y, disk);
        if(mArray)
            mArray.push(circle);
            
        return circle;
    },

    /** */
    makeRouteMarker : function(x, y, rtNum, mArray)
    {
        var route = null;
        if(rtNum != null && rtNum.length >= 1)
        {
            var icon  = new OpenLayers.Icon(this.ROUTE_DIR + rtNum + '.png', this.RTE_ICON_SIZE, this.RTE_ICON_OFFSET);
            route = this.makeMarker(x, y, icon);
            if(mArray)
                mArray.push(route);
        }
            
        return route;
    },

    /**
     * create a new marker & add it to a given layer
     * 
     * @param {Object} markerLayer
     * @param {Object} x
     * @param {Object} y
     * @param {Object} icon
     */
    makeMarker : function(x, y, icon)
    {
        var ll   = new OpenLayers.LonLat(x, y);
        var mark = new OpenLayers.Marker(ll, icon)
        
        return mark;
    },

    /** */
    clearVectorLayer : function(vLayer, vectors)
    {
        for(var x = 0; x < 5; x++)
        {
            try
            {
            	vLayer.removeFeatures(vectors);
            }
            catch(Ez)
            {
            }
        }
        vLayer.redraw();
    },
    
    /** */
    clearMarkerLayer : function(mLayer, markers)
    {
        for(var x = 0; x < 5; x++)
        {
            try
            {
                for(var i = 0; i < markers.length; i++)
                {
                    mLayer.removeMarker(markers[i]);
                }
            }
            catch(Ez)
            {
            }
        }
        mLayer.redraw();
    },

    /** */
    clearMarkerLayerZ : function(layer)
    {
        try 
        {
            for(var i in layer.markers)
            {
                var m = layer.markers[i];
                m.destory();
            }
            layer.clearMarkers();
        } 
        catch(err) 
        {
        }
    },

    /** */
    drawMarkers : function(mLayer, markers)
    {
        for(var i = 0; i < markers.length; i++)
        {
            mLayer.addMarker(markers[i]);
        }
    },
        
    /**
     * for marker layers...provides a extent to zoom in on all the markers in the layer 
     *  
     * NOTE:  implementation of a routine that should (but does not appear to be) in OpenLayers
     * @param {Object} layer
     */
    getMarkerLayerDataExtent : function(layer)
    {
        var maxExtent = layer.maxExtent;
        var markers   = layer.markers; 
        var length    = layer.markers.length; 
        if(markers != null && length > 0) 
        { 
            var m = markers[0];
            maxExtent = new OpenLayers.Bounds(m.lonlat.lon, m.lonlat.lat, m.lonlat.lon, m.lonlat.lat);
            for(var i=0; i < length; i++) 
            { 
                maxExtent.extend(markers[i].lonlat); 
            }
        }
        return maxExtent; 
    }, 

    ///////////// ZOOM UTILS ///////////// ZOOM UTILS ///////////// ZOOM UTILS ///////////// ZOOM UTILS /////////////

    /** */
    zoomToBBox : function(map, bbox)
    {
        if(bbox == null || map == null) return;
        try
        {
            var xy  = bbox.split(",");
            var ext = new OpenLayers.Bounds(xy[0], xy[1], xy[2], xy[3]);
            map.zoomToExtent(ext);
        }
        catch(e)
        {
            console.log("zoomToBBox exception: " + e);
        }
    },

    /** */
    growBBox : function(destBBox, srcBBox)
    {
        // might be sending in an empty dest...in that case, return the source
        if(destBBox == null)
            return srcBBox;

        var retVal = destBBox;
        try
        {
            // Bounds(left, bottom, top, right);
            var d = destBBox.split(",");
            var s = srcBBox.split(",");

            if(d && d.length >= 4 && s && s.length >= 4)
            {
                retVal  = (d[0] * 1.0) < (s[0] * 1.0) ? d[0] : s[0]; retVal += ",";
                retVal += (d[1] * 1.0) < (s[1] * 1.0) ? d[1] : s[1]; retVal += ",";
                retVal += (d[2] * 1.0) > (s[2] * 1.0) ? d[2] : s[2]; retVal += ",";
                retVal += (d[3] * 1.0) > (s[3] * 1.0) ? d[3] : s[3];
            }

            console.log("growBBox destBBox: " + srcBBox);
            console.log("growBBox srcBBox:  " + destBBox);
            console.log("growBBox retVal: "   + retVal);
        }
        catch(e)
        {
            console.log("growBBox exception: " + e);
        }

        return retVal;
    },

    /**
     * will calculate the extent of the marker layer, and zoom into that point on the passed in map
     *  
     * @param {Object} map
     * @param {Object} layer
     */
    zoomToMarkerLayerExtent : function(map, layer)
    {
        try
        {
            var bounds = this.getMarkerLayerDataExtent(layer);
            map.zoomToExtent(bounds);
        }
        catch(exp)
        {
        }
    },


    /** */
    zoomToGeometry : function(map, geometry, minZoom, maxZoom)
    {
        try 
        {
            var z = undefined;
            if(minZoom || maxZoom)
            {
                var cz = map.getZoom();
                if(minZoom && minZoom > cz) 
                    z = minZoom;
                else if(maxZoom && maxZoom < cz)
                    z = maxZoom;
            }

            var c = geometry.getCentroid();
            map.setCenter(new OpenLayers.LonLat(c.x, c.y), z);
        }
        catch(ex)
        {
        }
    },

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
    panZoomWithLimits : function(map, x, y, z, wideZoomLimit, closeZoomLimit)
    {
        try 
        {
            if(z && z >= wideZoomLimit && z <= closeZoomLimit)
            {
                map.zoomTo(z);
            }
            // TODO -- working OpenLayer??? http://trac.openlayers.org/ticket/719
            //m_map.setCenter( new OpenLayers.LonLat(x, y), zoom, true, true);
            // TODO: work around for the zoom bug #719
            else if (map.getZoom() > closeZoomLimit) 
                map.zoomTo(closeZoomLimit);
            else if(map.getZoom() < wideZoomLimit) 
                map.zoomTo(wideZoomLimit);
            
            if(x && y) 
                map.setCenter(new OpenLayers.LonLat(x, y));
        }
        catch(ex)
        {
        }
    },

    ///////////// MOUSE COORDS ///////////// MOUSE COORDS ///////////// MOUSE COORDS ///////////// MOUSE COORDS /////////////
    
    /** will round the coordinates to just feet -- no decimal part */
    roundCoord : function(coord)
    {
        var retVal = coord

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
            console.log('OpenLayersUtils.getLatLonOfPixel exception ' + e)
           
        }

        return retVal;
    },


    /**
     * given a map, and a pixel coordinate, return back a map coordinate
     */
    getLatLonOfPixel: function(map, pixelX, pixelY)
    {
        try
        {
            var px     = new OpenLayers.Pixel(pixelX, pixelY);
            var lonLat = map.getLonLatFromPixel(px);
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
    setCenter : function(map, x, y, zoom)
    {
        try
        {
            if(zoom && (zoom < 0 || zoom > 9))
                 zoom = 2;

            if(x && y)
                map.setCenter(new OpenLayers.LonLat(x, y), zoom);
            else if(zoom)
                map.zoomTo(zoom);
        }
        catch(e)
        {
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
