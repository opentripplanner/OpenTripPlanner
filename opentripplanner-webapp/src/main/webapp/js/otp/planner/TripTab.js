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

/** global to hold the print window pointer */
otp.planner.PrintTripWin = null;

/**
  * Web TripPlanner
  * 
  * otp.planner.TripTab's purpose is to act as a controller for a given trip request.   This object will retain the state of the
  * given set of itineraries (eg: which is selected, etc).
  */
otp.planner.TripTab = {

    // config
    map        : null,
    locale     : otp.locale.English,

    planner    : null,
    renderer   : null,
    xml        : null,
    request    : null,

    id         : 0,

    m_utils    : otp.planner.Utils,

    // constants
    XML_ITINERARIES_NODE : 'itineraries',
    ITIN_NODE_CSS        : 'itiny', 
    ITIN_ICON_CSS        : 'itinys-icon', 

    // raw trip plan data
    m_itinerariesXML   : null,
    m_itinerariesStore : null,

    m_valid            : true,
    m_activeItinerary  : null,
    m_itineraryCache   : null,

    m_title            : '',
    m_from             : null,
    m_to               : null,

    m_panel            : null,
    m_itinerariesTree  : null,
    m_tripDetailsTree  : null,
    m_tripNodePrefix   : 'tn-',

    /** */
    initialize : function(config)
    {
        console.log("enter planner.TripTab constructor");
        otp.configure(this, config);

        // step 1: save off raw data
        this.m_tripNodePrefix   ='tn-' + this.id + '-';
        this.m_itinerariesXML   = this.m_utils.domSelect(this.XML_ITINERARIES_NODE, this.xml); 
        this.m_itinerariesStore = this.m_utils.makeItinerariesStore();
        this.m_itineraryCache   = new Array();

        // step 2: load the data
        if(this.load())
        {
        }

        console.log("exit planner.TripTab constructor");
    },


    /** */
    load : function()
    {
        console.log("enter TripTab.load");

        // step A: load the stores
        var store = this.m_itinerariesStore;
        store.loadData(this.m_itinerariesXML);

        // step B: from & to nodes
        this.m_from = otp.util.ExtUtils.loadPointRecord('from', this.xml);
        this.m_to   = otp.util.ExtUtils.loadPointRecord('to',   this.xml);
        if(this.m_from && this.m_from.get('name'))
            this.m_title += this.m_from.get('name');
        if(this.m_to && this.m_to.get('name'))
            this.m_title += " to " + this.m_to.get('name');

        // step C: create itinerary nodes from the store (to be placed into trees ... see below)
        var z = new Array();
        for(var i = 0; i < store.getCount(); i++) 
        {
            var id    = this.m_tripNodePrefix + (i + 1);
            var itin  = store.getAt(i);
            itin.set('id', (i+1)); // for template -- eg: the numerical hyperlink listing itineary option
            var text = otp.planner.Templates.TP_ITINERARY.applyTemplate(itin.data);
            z[i] = otp.util.ExtUtils.makeTreeNode(id, text, this.ITIN_NODE_CSS, this.ITIN_ICON_CSS, true, this.itineraryClick, this);
        }

        // make sure there are nodes (itineraries) to add to the tree
        if(z.length > 0)
        {
            // step D++: add buttons to the tab 
            var r = new Ext.Toolbar.Button({
                text:    this.locale.buttons.reverse,
                id:      'trip-reverse',
                iconCls: 'reverse-button',
                tooltip: this.locale.buttons.reverseTip,
                scope:   this,
                handler: this.reverseCB
            });

            var e = new Ext.Toolbar.Button({
                text:    this.locale.buttons.edit,
                id:      'trip-edit',
                iconCls: 'edit-button',
                tooltip: this.locale.buttons.editTip,
                scope:   this,
                handler: this.editCB
            });

            var p = new Ext.Toolbar.Button({
                text:    this.locale.buttons.print,
                id:      'trip-print',
                iconCls: 'print-button',
                tooltip: this.locale.buttons.printTip,
                scope:   this,
                handler: this.printCB
            });

            // step D: create UI panel & tree & add tree to Itineary renderer 
            this.m_itinerariesTree = this.m_utils.makeItinerariesTree(this.id, this.itineraryClick, this);
            this.m_tripDetailsTree = this.m_utils.makeTripDetailsTree(this.id, null, null);
            this.m_panel           = this.m_utils.makeTripTab(this.id, this.m_title, this.m_itinerariesTree, this.m_tripDetailsTree, [r, e, p]);

            // step E: add itineraries to tree
            var tree = this.m_itinerariesTree;
            var root = tree.getRootNode();
            root.appendChild(z);

            // step F: click on first itinerary (see callback below for more steps)
            this.activateItinerary(1);

            // THIS LINE breaks new UI (not sure it's really needed -- just to highlight the first itinerary in tree)
            // var path = z[0].getPath();
            // tree.selectPath(path);
        }

        console.log("exit TripTab.load");
        return this.isValid();
    },

    /** */
    printCB : function(b, e)
    {
        console.log("enter TripTab.print");

        console.log("TripTab.print: close request object");
        var req    = otp.clone(this.request);
        req.url    = this.printUrl;

        // get the itin
        if(this.m_activeItinerary && this.m_activeItinerary.id)
            req.itinID = this.m_activeItinerary.id;

        var url    = otp.planner.Templates.tripPrintTemplate.apply(req);
        console.log("TripTab.print: url " + req.url);

        console.log("TripTab.print: open window");
        otp.planner.PrintTripWin = window.open(url,'WORKING','width=800,height=600,resizable=1,scrollbars=1,left=100,top=100,screenX=100,screenY=100');
        console.log("TripTab.print: window focus");
        otp.planner.PrintTripWin.focus();

        otp.util.AnalyticsUtils.gaEvent(otp.util.AnalyticsUtils.TRIP_PRINT);

        console.log("exit TripTab.print");
    },

    /** */
    editCB : function(b, e)
    {
        console.log("enter TripTab.edit");
        this.planner.clearForms();
        this.planner.populateFormTab(this.request);

        otp.util.AnalyticsUtils.gaEvent(otp.util.AnalyticsUtils.TRIP_EDIT);

        console.log("exit TripTab.edit");
    },

    /** */
    reverseCB : function(b, e)
    {
        console.log("enter TripTab.reverse");

        this.planner.clearForms();

        var rev = otp.clone(this.request);

        // reverse from/to
        var tmp  = rev.from;
        rev.from = rev.to;
        rev.to   = tmp;

        // reverse fromPlace/toPlace (more important than from/to, as *Place has precedence)
        tmp  = rev.fromPlace;
        rev.fromPlace = rev.toPlace;
        rev.toPlace   = tmp;

        // reverse any x,y positions...
        tmp  = rev.fromCoord;
        rev.fromCoord = rev.toCoord;
        rev.toCoord   = tmp;

        if(this.m_activeItinerary && this.m_activeItinerary.m_endTime)
        {
            rev.time = this.m_activeItinerary.m_endTime.format("g:i a");
            rev.arriveBy  = false; // when setting time for a reverse trip, always set plan
        }

        this.planner.populateFormTab(rev);

        otp.util.AnalyticsUtils.gaEvent(otp.util.AnalyticsUtils.TRIP_REVERSE);

        console.log("exit TripTab.reverse");
    },

    /** */
    draw : function()
    {
        console.log("enter TripTab.draw");
        this.renderer.clear();
        this.renderer.draw(this.m_activeItinerary, this.m_tripDetailsTree);
        this.planner.controller.activate(this.CLASS_NAME);
        console.log("exit TripTab.draw");
    },

    /**
     * callback function that will populate the itineraries tree  
     */
    itineraryClick : function(node, event)
    {
        console.log("enter TripTab.itineraryClick");
        try
        {
            // node id == 'tp-X-Y', where Y (eg: 1,2,...) is the index into the Ext store
            var tripNum = node.id.substring(node.id.lastIndexOf('-') + 1);
            console.log("TripTab.itineraryClick: activate itinerary number " + tripNum);

            this.activateItinerary(tripNum);
            this.draw();
        }
        catch(e)
        {
            console.log("exception TripTab.itineraryClick " + e);
        }
        console.log("exit TripTab.itineraryClick");
    },

    /**
     * make this itin active (create if necessary)
     */
    activateItinerary : function(id)
    {
        var newItin = this.getItinerary(id);
        if(newItin != null && newItin.isValid())
        {
            this.m_activeItinerary = newItin;
        }
    },


    /**
     * return a {otp.planner.Itinerary} object, either from the cache, or created new
     *
     * @param {Object} id
     */
    getItinerary : function(id)
    {
        var retVal = null;
        try
        {
            if(id == null)
                id = 1;

            // try to get itinerary object from cache...if not there, create it
            var retVal = this.m_itineraryCache[id];
            if(retVal == null)
            {
                var itin = this.m_itinerariesStore.getAt(id - 1);
                retVal = new otp.planner.Itinerary({map:this.planner.map, xml:itin, from:this.m_from, to:this.m_to, id:id})
                if(retVal != null && retVal.isValid())
                    this.m_itineraryCache[id] = retVal;
            }
        }
        catch(e)
        {
            console.log("exception TripTab.getItinerary (id = " + id + ") " + e);
        }
        
        return retVal;
    },

    /** */
    getId : function() 
    {
        return this.id;
    },

    /** */
    getActiveItinerary : function() 
    {
        return this.m_activeItinerary;
    },

    /** */
    getPanel : function() 
    {
        return this.m_panel;
    },

    /** */
    isValid : function()
    {
        if(this.m_itinerariesStore == undefined || this.m_itinerariesStore.getCount() <= 0)
            this.m_valid = true;
            
        return this.m_valid;
    },

    CLASS_NAME: "otp.planner.TripTab"
}

otp.planner.TripTab = new otp.Class(otp.planner.TripTab);

