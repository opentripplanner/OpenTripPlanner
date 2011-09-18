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

otp.namespace("otp.planner");

/**
  * Web Map / TripPlanner
  */
try
{
function millisToMinutes (n, p) {
    return parseInt(n / 60000);
}

function metersToMiles (n, p) {
    var miles = n / 1609.344;
    return miles.toFixed(1);
}

function metersToFeet(meters) {
    return parseInt(meters * 3.2808);
}

// TODO: make sure otp.util.DateUtils is available so we don't have to wrap these functions
function isoDateStringToDate(dateStr) {
    return otp.util.DateUtils.isoDateStringToDate(dateStr);
}

function prettyDateTime(date) {
    return otp.util.DateUtils.prettyDateTime(date);
}

function prettyTime(date) {
    return otp.util.DateUtils.prettyTime(date);
}

function prettyDistance(distance) {
    return otp.planner.Utils.prettyDistance(distance);
}

otp.planner.Utils = {

    // constants
    TRIPDETAILS_TREE   : 'tripdetails-tree-',
    ITINERARIES_TREE   : 'itineraries-tree-',
    TRIP_TAB           : 'trip-tab-',
    DETAILS_CLS        : 'itinys-node',
    ITIN_CLS           : 'itinys-node',

    FROM_ID            : 'from',
    TRIP_ID            : 'trip',
    TO_ID              : 'to',
    
    ITIN_RECORD : new Ext.data.Record.create([
                  'id',
                  'description',
                  {name: 'viaRoute',     mapping: '@viaRoute'},
                  {name: 'regularFare',  mapping: 'fare', convert: function(val, rec) { return otp.planner.Utils.getFare(rec, 'regular'); }},
                  {name: 'seniorFare',   mapping: 'fare', convert: function(val, rec) { return otp.planner.Utils.getFare(rec, 'senior'); }},
                  {name: 'studentFare',  mapping: 'fare', convert: function(val, rec) { return otp.planner.Utils.getFare(rec, 'student'); }},
                  {name: 'duration',     mapping: 'duration', convert: millisToMinutes},
                  {name: 'startTime',    mapping: 'startTime', convert: isoDateStringToDate},
                  {name: 'endTime',      mapping: 'endTime', convert: isoDateStringToDate},
                  {name: 'startTimeDisplay', mapping: 'startTime', convert: prettyDateTime},
                  {name: 'endTimeDisplay', mapping: 'endTime', convert: prettyDateTime},
                  {name: 'numTransfers', mapping: 'transfers'},
                  {name: 'numLegs',      mapping: 'legs', convert : function (n, p) { return p.length; }},
                  {name: 'walkTime',     mapping: 'walkTime', convert: millisToMinutes},
                  {name: 'walkDistance', mapping: 'walkDistance', convert: prettyDistance},
                  {name: 'transitTime',  mapping: 'transitTime', convert: millisToMinutes},
                  {name: 'waitingTime',  mapping: 'waitingTime', convert: millisToMinutes}
    ]),
    
    LEG_RECORD : new Ext.data.Record.create([
                  {name: 'id',               mapping: '@id'},
                  {name: 'mode',             mapping: '@mode'},
                  {name: 'agencyId',         mapping: '@agencyId'},
                  {name: 'headsign',         mapping: '@headsign'},
                  {name: 'order',            mapping: '@order'},
                  {name: 'interline',        mapping: '@interlineWithPreviousLeg'},
                  {name: 'startTime',        mapping: 'startTime', convert: isoDateStringToDate},
                  {name: 'endTime',          mapping: 'endTime', convert: isoDateStringToDate},
                  {name: 'startTimeDisplayShort', mapping: 'startTime', convert: prettyTime},
                  {name: 'endTimeDisplayShort',   mapping: 'endTime',   convert: prettyTime},
                  {name: 'duration',         mapping: 'duration', convert: millisToMinutes},
                  {name: 'distance',         mapping: 'distance', convert: prettyDistance},
                  {name: 'direction',        mapping: 'direction'},
                  {name: 'key',              mapping: 'key'},
                  {name: 'alerts',           mapping: 'leg', 
                  convert: function(n, p)
                  {
                      // TODO: we're using the DEPRICATED notes/text fields.
                      //       need to rewrite this with alerts/.../note
                      var nodes = Ext.DomQuery.select('notes', p);
                      var alerts = [];
                      for(var i = 0; i < nodes.length; i++)
                      {
                          var node = nodes[i];
                          var x = Ext.DomQuery.selectValue('text', node);
                          if(x)
                          {
                              alerts.push(x);
                          }
                      }
                      return alerts;
                  }},
                  {name: 'routeShortName',   mapping: '@route'},
                  {name: 'routeLongName',    mapping: '@routeLongName'},
                  {name: 'fromName',         mapping: 'from/name'},
                  {name: 'fromDescription',  mapping: 'from/description'},
                  {name: 'fromStopId',       mapping: 'from/stopId/id'},
                  {name: 'toName',           mapping: 'to/name'},
                  {name: 'toDescription',    mapping: 'to/description'},
                  {name: 'toStopId',         mapping: 'to/stopId/id'},

                  {name: 'steps',            mapping: 'steps', 
                                             convert: function(val, rec) {
                                                return otp.planner.Utils.makeWalkSteps(val, rec);
                                             }
                  },
                  {name: 'legGeometry',      mapping: 'legGeometry/points',
                                             convert: function(n,p) {
                                                return otp.util.OpenLayersUtils.encoded_polyline_converter(n,p);
                                             } 
                  }
    ]),
    /**
     * utility to select a dom element
     * 
     * @param {Object} name of node
     * @param {Object} xml object to select from
     */     
    domSelect : function(nodeName, xml)
    {
        return Ext.DomQuery.select(nodeName, xml);
    },

    /**
     * parse the <steps></steps> element into a JavaScript
     */
    makeWalkSteps : function(val, rec)
    {
        var nodes = Ext.DomQuery.select('steps/walkSteps', rec);
        var steps = [];
        for (var i = 0; i < nodes.length; i++)
        {
            var node = nodes[i];
            var step = {};
            // TODO: Parse these more efficiently?
            step.distance = Ext.DomQuery.selectNumber('distance', node);
            step.streetName = Ext.DomQuery.selectValue('streetName', node);
            if (!step.streetName)
            {
                step.streetName = "unnamed street";
            }
            step.absoluteDirection = Ext.DomQuery.selectValue('absoluteDirection', node);
            step.relativeDirection = Ext.DomQuery.selectValue('relativeDirection', node);
            step.stayOn = (Ext.DomQuery.selectValue('stayOn', node).toLowerCase() === 'true');
            step.lon = Ext.DomQuery.selectNumber('lon', node);
            step.lat = Ext.DomQuery.selectNumber('lat', node);
            step.elevation = Ext.DomQuery.selectValue('elevation', node); 
            step.exit = Ext.DomQuery.selectValue('exit', node); 
            steps.push(step);
        }
        return steps;
    },
    
    /**
     * parse the <fare> tag in the response document, extracting the data
     * associated with fareType.
     * 
     * @param {Object}
     *            rec XML object to parse
     * @param {String}
     *            fareType type of fare to parse
     * 
     * @returns {String} formatted string representation of the fare (e.g.,
     *          $2.25) or null if fare can't be parsed.  
     *          
     * 
     */
    getFare : function(rec, fareType) {
        var nodes = Ext.DomQuery.select('fare/entry', rec);
        var fare = null;
        for (var i = 0; i < nodes.length; i++) {
            if (Ext.DomQuery.selectValue('key', rec) === fareType) {
                var cents = parseInt(Ext.DomQuery.selectValue('value/cents', rec));
                //TODO Use currency in value/currency once available
                fare = this.formatMoney(cents);
            }
        }
        return fare;
    },

    /** 
     * TODO ... neeeds more work  ... trying to get â¬ and à¤°à¥à¤ªà¤¯à¤¾ to work...
     * IMPORTANT: this routine depends on otp.config.locale having the proper value
     * NOTE: this was borrowed from Extjs format library http://dev.sencha.com/deploy/dev/docs/source/Format.html#method-Ext.util.Format-usMoney
     */
    formatMoney : function(cents) {
        var retVal = cents;

        var v = cents / 100;
        v = (Math.round((v-0)*100))/100;
        v = (v == Math.floor(v)) ? v + ".00" : ((v*10 == Math.floor(v*10)) ? v + "0" : v);
        v = String(v);
        var ps = v.split('.'),
            whole = ps[0],
            sub = ps[1] ? '.'+ ps[1] : '.00',
            r = /(\d+)(\d{3})/;
        while (r.test(whole)) {
            whole = whole.replace(r, '$1' + ',' + '$2');
        }
        v = whole + sub;
        if(v.charAt(0) == '-') {
            retVal = '-' + otp.config.locale.labels.fare_symbol + v.substr(1);
        }
        else {
            retVal = otp.config.locale.labels.fare_symbol + v;
        }

        return retVal;
    },

    /** */
    makeItinerariesTree : function(id, clickCallback, scope)
    {
        var thisID = this.ITINERARIES_TREE + id;
        var root = otp.util.ExtUtils.makeTreeNode({
                id: 'root-' + thisID,
                text: '<strong>' + id + '</strong>',
                cls: this.ITIN_CLS,
                iconCls: this.ITIN_CLS,
                leaf: false
        }, clickCallback, scope);
        var retVal = new Ext.tree.TreePanel({
            //title: 'trip options',
            root       : root,
            id         : thisID,
            lines      : false,
            collapsible: false,
            rootVisible: false,
            margins : '0 0 0 0',
            cmargins: '0 0 0 0'
        });
        
        return retVal;
    },

    /** */
    makeTripDetailsTree : function(id, clickCallback, scope)
    {
        var thisID = this.TRIPDETAILS_TREE + id;
        var root = otp.util.ExtUtils.makeTreeNode({
                id: 'root-' + thisID,
                text: '<strong>' + id + '</strong>',
                cls: this.DETAIL_CLS,
                iconCls: this.DETAIL_CLS,
                leaf: false
        }, clickCallback, scope);
        var retVal = new Ext.tree.TreePanel({
            plugins    : new Ext.tree.NodeMouseoverPlugin(),
            root       : root,
            id         : thisID,
            lines      : false,
            //autoScroll:true,
            collapsible:   false,
            rootVisible:   false,
            collapseFirst: false,
            margins:  '0 0 0 0',
            cmargins: '0 0 0 0'
        });
        
        return retVal;
    },

    /** */
    makeTripTab: function(id, title, itinTree, detailsTree, buttons)
    {
        // step 3: create the panel
        var retVal = new Ext.Panel({
            id:           this.TRIP_TAB + id,
            title:        title,
            tabTip:       title,
            headerAsText: false,
            cls:          'preview single-preview',
            closable:     true,
            autoScroll:   true,
            border:       false,
            buttonAlign: 'center',
            buttons:      buttons,
            items:        [itinTree, detailsTree]
        });
        
        return retVal;
    },

    /** */
    makeFromStore     : function() { return otp.util.ExtUtils.makePointStore('leg',      'from' ); },
    /** */
    makeToStore       : function() { return otp.util.ExtUtils.makePointStore('leg',      'to'   ); },


    /** */
    makeLegStore: function()
    {
        return new Ext.data.Store({
            fields: this.LEG_RECORD,
            reader: new Ext.data.XmlReader({record: 'leg'}, this.LEG_RECORD)
        });
    },

    /** */
    makeItinerariesStore: function()
    {
        return new Ext.data.Store({
            fields: this.ITIN_RECORD,
            reader: new Ext.data.XmlReader({record: 'itinerary'}, this.ITIN_RECORD)
        });
    },

    /**
     * Takes a distance in meters and returns a pretty string representation, including the units. For example:
     * 
     * 582.2 --> "0.36 mi"
     * 20.62 --> "68 ft"
     * 
     * @param {number} meters The distance in meters
     * 
     * @returns {string} A formatted string, with units. If the input is null undefined, an empty string will be returned.
     * 
     * TODO: Make this method depend upon an app-wide config option specifying the system of units to use.
     */
    prettyDistance : function(meters) {
        var retVal = "";

        if (meters == null || typeof meters == 'undefined') {
            retVal = "";
        }
        else if (otp.config.metricsSystem == 'english') {
            var miles = metersToMiles(meters);
            // Display distances < 0.1 miles in feet
            if (miles < 0.1) {
                retVal = metersToFeet(meters) + " ft";
            } else {
                retVal = miles + " mi";
            }
        }
        else // default is metric system
        {
            var km = meters / 1000.0;
            if (km < 1) {
                retVal = parseInt(meters) + " m";
            } else {
                retVal = km.toFixed(2) + " km";
            }
        }

        return retVal;
    },

    /*
     * Determines whether or not the browser supports the HTML5 Canvas element.
     * Includes check for ExplorerCanvas for IE support.
     */
    supportsCanvas : function() {
        var c = document.createElement('canvas');
        if (typeof G_vmlCanvasManager != "undefined") {
            c = G_vmlCanvasManager.initElement(c);
        }
        return !!c.getContext;
    },
    
    CLASS_NAME: "otp.planner.Utils"
};
}
catch(e)
{
    console.log("planner.Utils Ext exception can be ignored -- just means you aren't including Ext.js in your app, which is OK:" + e);
}
