otp.namespace("otp.planner");

/**
  * Web Map / TripPlanner
  */
try
{
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
                  {name: 'date',         mapping: 'timeDistance/date'},
                  {name: 'duration',     mapping: 'timeDistance/duration'},
                  {name: 'startTime',    mapping: 'timeDistance/start'},
                  {name: 'endTime',      mapping: 'timeDistance/end'},
                  {name: 'numTransfers', mapping: 'time-distance/numberOfTransfers'},
                  {name: 'numLegs',      mapping: 'time-distance/numberOfTripLegs'},
                  {name: 'walkTime',     mapping: 'time-distance/walkingTime'},
                  {name: 'walkDistance', mapping: 'time-distance/distance'},
                  {name: 'transitTime',  mapping: 'time-distance/transitTime'},
                  {name: 'waitingTime',  mapping: 'time-distance/waitingTime'}
    ]),
    
    LEG_RECORD : new Ext.data.Record.create([
                  {name: 'id',               mapping: '@id'},
                  {name: 'mode',             mapping: '@mode'},
                  {name: 'order',            mapping: '@order'},
                  {name: 'startTime',        mapping: 'time-distance/startTime'},
                  {name: 'endTime',          mapping: 'time-distance/endTime'},
                  {name: 'duration',         mapping: 'time-distance/duration'},
                  {name: 'distance',         mapping: 'time-distance/distance'},
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
                  {name: 'routeName',        mapping: 'route/name'},
                  {name: 'routeNumber',      mapping: 'route/number'},
                  {name: 'urlParam',         mapping: 'lineURL/@param'},
                  {name: 'fromDescription',  mapping: 'from/description'},
                  {name: 'fromStopId',       mapping: 'from/stopId'},
                  {name: 'fromCity',         mapping: 'from/@areaValue'},
                  {name: 'toDescription',    mapping: 'to/description'},
                  {name: 'toStopId',         mapping: 'to/stopId'},
                  {name: 'toCity',           mapping: 'to/@areaValue'},
                  {name: 'leg-geometry-raw', mapping: 'legGeometry'},
                  {name: 'leg-geometry',         mapping: 'legGeometry/points',
                	                         convert: function(n,p) {
                	  							return otp.util.OpenLayersUtils.encoded_polyline_converter(n,p);
                	 					     } },
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
}
}
catch(e)
{
    console.log("planner.Utils Ext exception can be ignored -- just means you aren't including Ext.js in your app, which is OK");
}