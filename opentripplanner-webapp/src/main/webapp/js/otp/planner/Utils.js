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
	return miles.toFixed(2);
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
                  {name: 'regularFare',  mapping: 'fare/regular'},
                  {name: 'honoredFare',  mapping: 'fare/special[@id=honored]'},
                  {name: 'youthFare',    mapping: 'fare/special[@id=youth]'},
                  {name: 'tramFare',     mapping: 'fare/special[@id=tram]'},
                  {name: 'duration',     mapping: 'duration', convert: millisToMinutes},
                  {name: 'startTime',    mapping: 'startTime', convert: isoDateStringToDate},
                  {name: 'endTime',      mapping: 'endTime', convert: isoDateStringToDate},
                  {name: 'startTimeDisplay', mapping: 'startTime', convert: prettyDateTime},
                  {name: 'endTimeDisplay', mapping: 'endTime', convert: prettyDateTime},
                  {name: 'numTransfers', mapping: 'transfers'},
                  {name: 'numLegs',      mapping: 'legs', convert : function (n, p) { return p.length; }},
                  {name: 'walkTime',     mapping: 'walkTime', convert: millisToMinutes},
                  {name: 'walkDistance', mapping: 'walkDistance', convert: metersToMiles},
                  {name: 'transitTime',  mapping: 'transitTime', convert: millisToMinutes},
                  {name: 'waitingTime',  mapping: 'waitingTime', convert: millisToMinutes}
    ]),
    
    LEG_RECORD : new Ext.data.Record.create([
                  {name: 'id',               mapping: '@id'},
                  {name: 'mode',             mapping: '@mode'},
                  {name: 'headsign',         mapping: '@headsign'},
                  {name: 'order',            mapping: '@order'},
                  {name: 'startTime',        mapping: 'startTime', convert: isoDateStringToDate},
                  {name: 'endTime',          mapping: 'endTime', convert: isoDateStringToDate},
                  {name: 'startTimeDisplayShort', mapping: 'startTime', convert: prettyTime},
                  {name: 'duration',         mapping: 'duration', convert: millisToMinutes},
                  {name: 'distance',         mapping: 'distance', convert: metersToMiles},
                  {name: 'direction',        mapping: 'direction'},
                  {name: 'key',              mapping: 'key'},
                  {name: 'alerts',           mapping: 'route', convert: function(n, p)
                  {
                      var nodes = Ext.DomQuery.select('route/alert', p);
                      var alerts = [];
                      for(var i = 0; i < nodes.length; i++)
                      {
                          var node = nodes[i];
                          var x = Ext.DomQuery.selectValue('description', node);
                          if(x)
                          {
                              alerts.push(x);
                          }
                      }
                      return alerts;
                  }},
                  {name: 'routeName',        mapping: '@route'},
                  {name: 'routeNumber',      mapping: 'route/number'},
                  {name: 'url',         mapping: 'lineURL/@param'},
                  {name: 'fromName',         mapping: 'from/name'},
                  {name: 'fromDescription',  mapping: 'from/description'},
                  {name: 'fromStopId',       mapping: 'from/stopId'},
                  {name: 'fromCity',         mapping: 'from/@areaValue'},
                  {name: 'toName',           mapping: 'to/name'},
                  {name: 'toDescription',    mapping: 'to/description'},
                  {name: 'toStopId',         mapping: 'to/stopId'},
                  {name: 'toCity',           mapping: 'to/@areaValue'},
                  {name: 'legGeometry',         mapping: 'legGeometry/points',
                	                         convert: function(n,p) {
                	  							return otp.util.OpenLayersUtils.encoded_polyline_converter(n,p);
                	 					     } }
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

    /** */
    makeItinerariesTree : function(id, clickCallback, scope)
    {
        var thisID = this.ITINERARIES_TREE + id;
        var retVal = new Ext.tree.TreePanel({
            //title: 'trip options',
            root       : otp.util.ExtUtils.makeTreeNode('root-' + thisID, '<B>' + id + '</B>',  this.ITIN_CLS, this.ITIN_CLS, false, clickCallback, scope),
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
        var retVal = new Ext.tree.TreePanel({
            root       : otp.util.ExtUtils.makeTreeNode('root-' + thisID, '<B>' + id + '</B>', this.DETAIL_CLS, this.DETAIL_CLS, false, clickCallback, scope),
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

    CLASS_NAME: "otp.planner.Utils"
};
}
catch(e)
{
    console.log("planner.Utils Ext exception can be ignored -- just means you aren't including Ext.js in your app, which is OK:" + e);
}