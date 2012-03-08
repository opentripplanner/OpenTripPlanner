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
    ui            : null,
    locale        : null,
    templates     : null,
    linkTemplates : null,

    planner       : null,
    renderer      : null,
    topoRenderer  : null,
    xml           : null,
    request       : null,

    id            : 0,

    m_utils       : otp.planner.Utils,

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
    },


    /** */
    load : function()
    {
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
        var treeNodeDefaults = {cls: this.ITIN_NODE_CSS, iconCls: this.ITIN_ICON_CSS, leaf: true};
        for(var i = 0; i < store.getCount(); i++) 
        {
            var id    = this.m_tripNodePrefix + (i + 1);
            var itin  = store.getAt(i);
            itin.set('id', (i+1)); // for template -- eg: the numerical hyperlink listing itinerary option
            if (itin.data && !otp.util.Modes.isTransit(itin.data.mode)) {
                itin.data.numTransfers = null;  // don't display transfer information on non-transit trips
            }
            var text = this.templates.TP_ITINERARY.applyTemplate(itin.data);
            var treeNodeConfig = Ext.apply({}, {id: id, text: text}, treeNodeDefaults);
            z[i] = otp.util.ExtUtils.makeTreeNode(treeNodeConfig, this.itineraryClick, this);
        }

        // make sure there are nodes (itineraries) to add to the tree
        if(z.length > 0)
        {
            // step D++: add buttons to the tab 
            var r = new Ext.Toolbar.Button({
                text:    this.locale.buttons.reverse,
                iconCls: 'reverse-button',
                tooltip: this.locale.buttons.reverseTip,
                scope:   this,
                handler: this.reverseCB
            });

            var e = new Ext.Toolbar.Button({
                text:    this.locale.buttons.edit,
                iconCls: 'edit-button',
                tooltip: this.locale.buttons.editTip,
                scope:   this,
                handler: this.editCB
            });

            var buttons = [r, e];

            if(this.planner.showPrintButton)
            {
                var p = new Ext.Toolbar.Button({
                    text:    this.locale.buttons.print,
                    iconCls: 'print-button',
                    tooltip: this.locale.buttons.printTip,
                    scope:   this.planner,
                    handler: this.planner.printCB
                });
                buttons.push(p);
            }

            if (this.planner.showLinksButton)
            {
                var l = new Ext.Toolbar.Button({
                    text: this.locale.buttons.link,
                    iconCls: 'link-button',
                    tooltip: this.locale.buttons.linkTip,
                    scope: this,
                    handler: this.linkCB
                });
                buttons.push(l);
            }

            // step D: create UI panel & tree & add tree to Itinerary renderer 
            this.m_itinerariesTree = this.m_utils.makeItinerariesTree(this.id, this.itineraryClick, this);
            this.m_tripDetailsTree = this.m_utils.makeTripDetailsTree(this.id, null, null);
            this.m_panel           = this.m_utils.makeTripTab(this.id, this.m_title, this.m_itinerariesTree, this.m_tripDetailsTree, buttons);

            // step E: add itineraries to tree
            var tree = this.m_itinerariesTree;
            var root = tree.getRootNode();
            root.appendChild(z);

            // step F: click on first itinerary (see callback below for more steps)
            this.activateItinerary(1);

            // step G: highlight the first itinerary (this has to be done in a callback because Ext is insane)
            var firstId = z[0].attributes.id;
            function selectFirstItinerary(node) {
              node.eachChild( function(child) {
                if(child.attributes.id == firstId ) {
                  child.select();
                  tree.un('expandnode', selectFirstItinerary);
                }
              });
            };
            tree.on('expandnode', selectFirstItinerary);
        }

        return this.isValid();
    },

    /** Show alternative routes - hover */
    mouseOverItineraryFn : function(eventObject,elRef) {
        var newItin = eventObject.getItinerary(elRef.id);
        eventObject.renderer.drawItineraryAlternative(newItin);
    },

    /** Show alternative routes - clear  */
    mouseOutItineraryFn : function(eventObject,elRef) {
        var theItin = eventObject.getItinerary(elRef.id);
        eventObject.renderer.clearAlternatives(theItin);
    },

    /** 
     * will display a link (or set of links) to the trip planner for a given itinerary in a dialog
     * 
     * @param {Object} b
     * @param {Object} e
     */
    linkCB : function(b, e)
    {
        if(this.linkTemplates == null || this.linkTemplates.length <= 0) return;


        // link dialog has a message about the trip and is at least 120 pixels in height
        var win_y = 120;
        var html = this.templates.tripFeedbackDetails.apply(this.request) + "<br/>";

        // if there are link templates in the config, then process those templates and add their contents to the dialog (also increase the height of the window)
        for(var i = 0; i < this.linkTemplates.length; i++) {
            win_y+=15;

            // a separator template will add a newline (and optional header string) to the html
            if(this.linkTemplates[i].separator == true) {
                html += '<br/>';
                if(this.linkTemplates[i].name != null) {
                    html += '<h2>' + this.linkTemplates[i].name + '</h2>';
                    win_y+=20;
                }
                continue;
            }

            // create the Ext XTemplate on the first pass through this loop for all URLs 
            if(this.linkTemplates[i].template == null) {
                this.linkTemplates[i].template = new Ext.XTemplate(this.linkTemplates[i].url).compile();
            }

            // add a new url to the link dialog, processing the XTemplate for the URL 
            html += '<a target="#" href="' + this.linkTemplates[i].template.apply(this.request) + '">' + this.linkTemplates[i].name + '</a><br/>';
        }

        // make and show the link dialog
        this.linkDialog = otp.util.ExtUtils.makePopup({'html':html}, this.locale.buttons.link, true, 300, win_y, true, 100, 200);
    },

    /** */
    editCB : function(b, e)
    {
        this.planner.clearForms();
        this.planner.populateFormTab(this.request);

        otp.util.Analytics.gaEvent(otp.util.Analytics.OTP_TRIP_EDIT);
    },

    /** */
    reverseCB : function(b, e)
    {
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

        otp.util.Analytics.gaEvent(otp.util.Analytics.OTP_TRIP_REVERSE);
    },

    /** */
    draw : function() {
        this.renderer.clear();
        
        /* draw topographic map */
        
        var hasBikeWalkLeg = false;
        for(var i=0; i<this.m_activeItinerary.m_legStore.getTotalCount(); i++) {
            if(this.m_activeItinerary.m_legStore.getAt(i).get("mode")=="BICYCLE" ||
               this.m_activeItinerary.m_legStore.getAt(i).get("mode")=="WALK") {
                hasBikeWalkLeg = true;
                break;
            }
        }
        
        if(hasBikeWalkLeg)
        {
            try
            {
                this.ui.innerSouth.getEl().setHeight(180);
                this.ui.innerSouth.show();
                this.ui.viewport.doLayout();

                this.topoRenderer.draw(this.m_activeItinerary, this.m_tripDetailsTree);
            }
            catch(e)
            {
                this.ui.innerSouth.hide();
                this.ui.viewport.doLayout();

                console.log("EXCEPTION in topoRenderer.draw(): " + e);
            }
        }

        this.renderer.draw(this.m_activeItinerary, this.m_tripDetailsTree);
        this.planner.controller.activate(this.CLASS_NAME);
        
        /* Show alternative routes */
        /* ------------------------------------------------- */
        var els = Ext.query('.dir-alt-route-inner');        
        for (var i=0; i < els.length; i++) {
        	var el = Ext.get(els[i]);
        	el.removeAllListeners();
        	el.on('mouseover', this.mouseOverItineraryFn.createCallback(this, el));
        	el.on('mouseout', this.mouseOutItineraryFn.createCallback(this, el));
        }
        /* ------------------------------------------------- */

    },

    /**
     * callback function that will populate the itineraries tree  
     */
    itineraryClick : function(node, event) {
        // node id == 'tp-X-Y', where Y (eg: 1,2,...) is the index into the Ext
        // store
        var tripNum = node.id.substring(node.id.lastIndexOf('-') + 1);

        this.activateItinerary(tripNum);
        this.draw();
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
        if (id == null) {
            id = 1;
        }

        // try to get itinerary object from cache...if not there, create it
        var retVal = this.m_itineraryCache[id];
        if (retVal == null)
        {
            var itin = this.m_itinerariesStore.getAt(id - 1);
            retVal = new otp.planner.Itinerary( {
                map              : this.planner.map,
                triptab          : this,
                locale           : this.locale,
                templates        : this.templates,
                showStopCodes    : this.planner.showStopCodes,
                useRouteLongName : this.planner.useRouteLongName,
                itineraryMessages: this.planner.itineraryMessages,
                xml              : itin,
                from             : this.m_from,
                to               : this.m_to,
                id               : id
            });
            if (retVal != null && retVal.isValid()) {
                this.m_itineraryCache[id] = retVal;
            }
        }

        return retVal;
    },

    /** */
    getId : function() 
    {
        return this.id;
    },

    /** Return the title of this tab */
    getTitle : function () {
        return this.m_title;
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
};

otp.planner.TripTab = new otp.Class(otp.planner.TripTab);

