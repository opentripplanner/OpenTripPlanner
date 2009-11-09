otp.namespace("otp.planner");

/**
  * otp/planner/Forms.js 
  * 
  * Purpose is to manage the Ext input forms for the trip planner.
  * 
  */
otp.planner.StaticForms = {

    FIELD_ANCHOR     : '95%',

    // external (config) objects
    locale           : otp.locale.English,
    planner          : null,
    contextMenu      : null,
    poi              : null,

    url              : '/plan',

    // forms & stores
    m_panel          : null,

    m_fromErrorStore : null,
    m_toErrorStore   : null,
    m_geoErrorPopup  : null,

    m_fromForm       : null,
    m_toForm         : null,
    m_date           : null,
    m_time           : null,
    m_arrStore       : null,
    m_arrForm        : null,

    m_walkStore      : null,
    m_walkForm       : null,
    m_optimizeStore  : null,
    m_optimizeForm   : null,
    m_modeStore      : null,
    m_modeForm       : null,

    // buttons
    m_submitButton   : null,

    m_fromCoord      : '0.0,0.0',
    m_toCoord        : '0.0,0.0',

    m_xmlRespRecord  : null, 

    /**
     * @consturctor
     * @param {Object} config
     */
    initialize : function(config)
    {
        console.log("enter Forms() constructor");

        // step 1: bit of init (before configure override happens)
        otp.configure(this, config);

        // step 2: setup
        if(this.m_xmlRespRecord == null)
        this.m_xmlRespRecord  = new Ext.data.XmlReader({
                record: 'response',
                success: '@success'
            },
            ['date', 'time', 'from', 'to', 'locations', 'fromList', 'toList']
        );

        this.makeMainPanel();

        // step 3: set the singleton & statis stuff to this 
        otp.planner.StaticForms = this;

        console.log("exit Forms() constructor");
    },

    /**
     * 
     */
    getPanel : function()
    {
        return this.m_panel;
    },

    /**
     *
     */
    submit : function()
    {
        console.log("enter planner.Forms.submit()");
        try
        {
            // hide stuff that might be open
            this.collapseComboBoxes();
            this.hideErrorDialogs();

            otp.util.ExtUtils.setTripPlannerCookie();

            this.m_panel.form.submit({
                    method:  'GET',
                    url:     this.url,
                    waitMsg: this.locale.tripPlanner.labels.submitMsg
            });

            // analytics 
            otp.util.AnalyticsUtils.gaEvent(otp.util.AnalyticsUtils.TRIP_SUBMIT);
        }
        catch(e)
        {
            console.log('exception Forms.submit exception ' + e)
        }

        console.log("exit planner.Forms.submit()");
    },

    /**
     * pre submit does some necessary bookkeeping for the form.  the real work happens in the (overridden) submit method
     */
    preSubmit : function(form, action)
    {
        console.log("enter planner.Forms.preSubmit()");

        // step 1: save off the state of the from & to text forms (eg: remember input into these fields)
        this.m_fromForm.persist();
        this.m_toForm.persist();

         // step 2: hide stuff that might be open
        this.collapseComboBoxes();
        this.hideErrorDialogs();

        // step 3: fixe up some of the form values before sending onto the trip planner web service
        var toPlaceVal   = this.m_toForm.getRawValue();
        var fromPlaceVal = this.m_fromForm.getRawValue();
        var toCoordVal   = otp.util.ObjUtils.getCoordinate(this.m_toCoord);
        var fromCoordVal = otp.util.ObjUtils.getCoordinate(this.m_fromCoord);
        form.setValues({
            fromCoord: fromCoordVal,
            fromPlace: fromPlaceVal,
            toCoord:   toCoordVal,
            toPlace:   toPlaceVal
        });

        console.log("exit planner.Forms.preSubmit()");
    },

    /** */
    submitSuccess : function(form, action)
    {
        console.log('enter Forms.submitSuccess');
        this.planner.newTripPlan(action.response.responseXML, this.getFormData());
        if(this.poi) this.poi.clearTrip();
        otp.util.AnalyticsUtils.gaEvent(otp.util.AnalyticsUtils.TRIP_SUCCESS);
        console.log('exit Forms.submitSuccess');

    },

    /** */
    submitFailure : function(form, action)
    {
        console.log('enter Forms.submitFailure');
        this.m_submitButton.focus();
        this.tripRequestError(action.response.responseXML);
        console.log('exit Forms.submitFailure');
    },


    /** error handler */
    tripRequestError : function(xml)
    {
        var message  = null;
        var code     = -111;
        var options  = null;
        var fromGrid = null;
        var toGrid   = null;

        // load xml to see what errors we have
        try
        {
            var gridCols = [
                {
                    header:    'Name',
                    width:     .75,
                    sortable:   true,
                    dataIndex: 'description'
                }
                ,
                {
                    header:    'City',
                    width:     .25,
                    sortable:true,
                    dataIndex: 'areaValue'
                }
            ]
            
            // try to populate the from & to form stores (in case of ambiguous results of geocoding)
            var to = Ext.DomQuery.selectNode('toList', xml);
            if(to != null)
            {
                var toStore = otp.util.ExtUtils.makeLocationStore();
                toStore.loadData(to);
                toGrid   = otp.util.ExtUtils.makeGridView(toStore, gridCols, {title:this.locale.tripPlanner.error.geoToMsg, iconCls:'end-icon'});
                toGrid.on(
                        'rowclick', function(g, i, e)
                        {
                            var n = otp.util.ExtUtils.gridClick(g, i, {description:'', x:'', y:''});
                            this.setTo(n.description, n.x, n.y, true);
                        },
                        this);
                toGrid.on(
                        'rowdblclick', function(g, i, e)
                        {
                            var n = otp.util.ExtUtils.gridClick(g, i, {description:'', x:'', y:''});
                            this.setTo(n.description, n.x, n.y, true);
                            this.m_geoErrorPopup.close();
                        },
                        this);
                options = true;
            }

            var from = Ext.DomQuery.selectNode('fromList', xml);
            if(from != null)
            {
                var fStore = otp.util.ExtUtils.makeLocationStore();
                fStore.loadData(from);
                fromGrid = otp.util.ExtUtils.makeGridView(fStore, gridCols, {title:this.locale.tripPlanner.error.geoFromMsg, iconCls:'start-icon'});
                fromGrid.on(
                        'rowclick', function(g, i, e)
                        {
                            var n = otp.util.ExtUtils.gridClick(g, i, {description:'', x:'', y:''});
                            this.setFrom(n.description, n.x, n.y, true);
                        },
                        this);
                fromGrid.on(
                        'rowdblclick', function(g, i, e)
                        {
                            var n = otp.util.ExtUtils.gridClick(g, i, {description:'', x:'', y:''});
                            this.setFrom(n.description, n.x, n.y, true);
                            this.m_geoErrorPopup.close();
                        },
                        this);
                options = true;
            }

            // if not ambiguous results, then show a dialog
            if(options)
            {
                // put the panel(s) into an array for the parent panel
                var errorWindowHeight = 0;
                var e  = new Array();
                if(fromGrid)
                {
                    errorWindowHeight += 185;
                    e.push(fromGrid);
                } 
                if(toGrid) 
                {
                    errorWindowHeight += 185;
                    e.push(toGrid);
                }
                if(errorWindowHeight < 200) errorWindowHeight = 220;

                // create the geo error popup
                var zz = new Ext.Panel({layout:'anchor', items:e});
                this.m_geoErrorPopup = otp.util.ExtUtils.makePopup({layout:'anchor',items:[zz]}, this.locale.tripPlanner.error.title, true, 360, errorWindowHeight, true, false, 50, 170);
                message = null;
                otp.util.AnalyticsUtils.gaEvent(otp.util.AnalyticsUtils.TRIP_GEO_ERROR + "/from/" + (from != null) + "/to/" + (to != null));
            }
            else
            {
                var err  = Ext.DomQuery.selectNode('error',    xml);
                message  = Ext.DomQuery.selectValue('message', err);
                code     = Ext.DomQuery.selectValue('@id',     err);
                otp.util.AnalyticsUtils.gaEvent(otp.util.AnalyticsUtils.TRIP_ERROR + "/" + code);
            }
        } 
        catch(e) 
        {
            // TODO - localize
            if(message == null || message == '')
                message = this.locale.tripPlanner.error.deadMsg;
        }

        if(message != null && message.length > 0)
        {
            // show the error
            Ext.MessageBox.show({
                title:    this.locale.tripPlanner.error.title,
                msg:      message,
                buttons:  Ext.Msg.OK,
                icon:     Ext.MessageBox.ERROR,
                animEl:   'tp-error-message',
                minWidth: 170,
                maxWidth: 270
            });

            // kill the message box after 5 seconds
            setTimeout(function()
            {
                try {
                    Ext.MessageBox.hide()
                } catch(e) {}
            }, 5000);
        }
    },


    /**
     * 
     */
    clear : function()
    {
        this.collapseComboBoxes();
        this.hideErrorDialogs();
        this.clearFrom(true);
        this.clearTo(true);
        this.m_submitButton.enable();
        this.m_submitButton.focus();
    },

    /** */
    collapseComboBoxes : function()
    {
        try
        {
            this.m_fromForm.collapse();
            this.m_toForm.collapse();
        }
        catch(e)
        {
            console.log("Forms.collapseComboBox " + e)
        }
    },

    /** */
    hideErrorDialogs : function()
    {
        // hide the default message box
        try {Ext.MessageBox.hide()         } catch(e){}
        try{this.m_geoErrorPopup.close();  } catch(e){}
    },

    /** wrapper (with try / catch) for planner focus() */
    focus : function()
    {
        try
        {
            this.planner.focus();
        }
        catch(e)
        {
            console.log("exception Forms.forms " + e);
        }
    },

    /**
     * set from form with either/both a string and X/Y
     */
    setFrom : function(fString, x, y, moveMap, noPoi)
    {
        this.focus();
        this.m_fromForm.setRawValue(fString);
        if(x && x > -180.1 && y && y > -90.1) 
        {
            this.m_fromCoord = x + ',' + y;
            if(this.poi && !noPoi) this.poi.setFrom(x, y, fString, moveMap);
        }
    },

    /**
     * set to form with either/both a string and X/Y
     */
    setTo : function(tString, x, y, moveMap, noPoi)
    {
        this.focus();
        this.m_toForm.setRawValue(tString);
        if(x && x > -180.1 && y && y > -90.1) 
        {
            this.m_toCoord = x + ',' + y;
            if(this.poi && !noPoi) this.poi.setTo(x, y, tString, moveMap);
        }
    },

    setRawInput : function(p, f)
    {
        var retVal = false;

        // since we're 
        if(p != null 
        && p !== true 
        && p != "true" 
        && p.match('Address, .*Stop ID') == null
        )
        {
            f.setRawValue(p);
            retVal = true;
        }

        return retVal;
    },

    /** */
    populate : function(params)
    {
        var forms = this;
        if(params)
        {
            this.clearFrom();
            this.clearTo();

            this.setRawInput(params.Orig, forms.m_fromForm);
            this.setRawInput(params.Dest, forms.m_toForm);

            this.setRawInput(params.from, forms.m_fromForm);
            this.setRawInput(params.to,   forms.m_toForm);
            this.setRawInput(params.fromPlace, forms.m_fromForm);
            this.setRawInput(params.toPlace,   forms.m_toForm);

            if(params.fromCoord && params.fromCoord.indexOf('0.0') != 0)
                this.m_fromCoord = params.fromCoord;
            if(params.toCoord && params.toCoord.indexOf('0.0') != 0)
                this.m_toCoord = params.toCoord;


            // IMPORTANT NOTE: the order of the operations below is important to making reverse directions
            // work properly. Note especially how xxxParam comes after the xxx varient (this precedence sets 
            // these forms up for success)

            // TODO - should find a way to make the xxx (String value, vs coded value) work propertly in submit
            var time=false;
            var date=false;

            if(params.date)
            {
                forms.m_date.setRawValue(params.date);
                date = true;
            }
            else if(params.on)
            {
                forms.m_date.setRawValue(params.on);
                date = true;
            }
            if(params.arrParam)
                forms.m_arrForm.setValue(params.arrParam);
            if(params.arr)
                forms.m_arrForm.setValue(params.arr);
            if(params.Arr)
                forms.m_arrForm.setValue(params.Arr);
            if(params.after)
            {
                time = params.after.replace(/\./g, "");
                forms.m_time.setRawValue(time);
                forms.m_arrForm.setValue('D');
                time = true;
            }
            else if(params.by)
            {
                time = params.by.replace(/\./g, "");
                forms.m_time.setRawValue(time);
                forms.m_arrForm.setValue('A');
                time = true;
            }

            if(params.time)
            {
                time = params.time.replace(/\./g, "");
                forms.m_time.setRawValue(time);
                time = true;
            }
            if(params.opt)
                forms.m_optimizeForm.setValue(params.opt);
            if(params.optParam)
                forms.m_optimizeForm.setValue(params.optParam);
            if(params.min)
                forms.m_optimizeForm.setValue(params.min);
            if(params.minParam)
                forms.m_optimizeForm.setValue(params.minParam);
            if(params.walk)
                forms.m_walkForm.setValue(params.walk);
            if(params.Walk)
                forms.m_walkForm.setValue(params.Walk);
            if(params.walkParam)
                forms.m_walkForm.setValue(params.walkParam);
            if(params.mode)
                forms.m_modeForm.setValue(params.mode);
            if(params.modeParam)
                forms.m_modeForm.setValue(params.modeParam);

            // stupid trip planner form processing...

            // Hour=7&Minute=02&AmPm=pm
            if(!time && params.Hour && params.Minute && params.AmPm)
            {
                time = params.Hour + ":" +  params.Minute + " " + params.AmPm.toLowerCase();
                time = time.replace(/\./g, "");
                forms.m_time.setRawValue(time);
                time = true;
            }

            // &month=Oct&day=7
            if(!date && params.month && params.day)
            {
                var d = new Date();
                var y = d.getFullYear();
                var n = d.getMonth();
                var m = otp.util.DateUtils.getMonthAsInt(params.month);
                if(m < n) // assume a month that is prior to now means next year
                    y++;

                if(params.day.length == 1) 
                    params.day = "0" + params.day;

                date = m + "/" + params.day + "/" + y;
                forms.m_date.setRawValue(date);
                date = true;
            }
        }
    },


    /**
     * returns current state (values) of the trip planner forms
     * used for creating a URL populated with values to the trip planner
     */
    getFormData : function(url)
    {
        var retVal = {};
        retVal.url       = url;
        retVal.itinID    = "1";
        retVal.from      = this.m_fromForm.getRawValue();
        retVal.fromPlace = this.m_fromForm.getRawValue();
        retVal.fromCoord = this.m_fromCoord;
        retVal.to        = this.m_toForm.getRawValue();
        retVal.toPlace   = this.m_toForm.getRawValue();
        retVal.toCoord   = this.m_toCoord;
        retVal.date      = this.m_date.getRawValue();
        retVal.time      = this.m_time.getRawValue();
        retVal.arr       = this.m_arrForm.getRawValue();
        retVal.arrParam  = this.m_arrForm.getValue();
        retVal.opt       = this.m_optimizeForm.getRawValue();
        retVal.optParam  = this.m_optimizeForm.getValue(); 
        retVal.walk      = this.m_walkForm.getRawValue();
        retVal.walkParam = this.m_walkForm.getValue();
        retVal.mode      = this.m_modeForm.getRawValue();
        retVal.modeParam = this.m_modeForm.getValue();

        try
        {
            retVal.time = retVal.time.replace(/\./g, "");
        }
        catch(e)
        {
        }

        return retVal;
    },

    /**
     * 
     */
    clearFrom : function(formToo)
    {
        this.m_fromCoord = '0.0,0.0'; 

        if(formToo && formToo === true)
        {
            this.m_fromForm.clear();
        }
    },

    /** 
     */
    clearTo : function(formToo)
    {
        this.m_toCoord = '0.0,0.0'; 

        if(formToo && formToo === true)
        {
            this.m_toForm.clear();
        }
    },

    /** */
    selectFrom : function(combo, record, num)
    {
         this.m_fromCoord = otp.util.ExtUtils.getCoordinate(record);
    },

    /** */
    selectTo : function(combo, record, num)
    {
        this.m_toCoord = otp.util.ExtUtils.getCoordinate(record);
    },

    /** */
    getContextMenu : function(cm)
    {
        if(cm != null)
            this.contextMenu = cm;

        var retVal = [
        {
            text    : this.locale.contextMenu.fromHere,
            iconCls : 'cFromHere',
            scope   : this,
            handler : function () {
                var ll = this.contextMenu.getMapCoordinate();
                this.setFrom(ll.lon + "," + ll.lat, ll.lon, ll.lat);
            }
        },
        {
            text    : this.locale.contextMenu.toHere,
            iconCls : 'cToHere',
            scope   : this,
            handler : function () {
                var ll = this.contextMenu.getMapCoordinate();
                this.setTo(ll.lon + "," + ll.lat, ll.lon, ll.lat);
            }
        }
        ];
        return retVal;
    },


    /**
     * from & to form creation
     */
    makeMainPanel : function()
    {
        console.log("enter Forms.makeMainPanel()");

        var fromToForms = this.makeFromToForms();
        var fromToFP = new Ext.form.FieldSet({
            labelWidth:  40,
            border:      false,
            items:       fromToForms
        });

        var optForms = this.makeOptionForms();
        var optFP = new Ext.form.FieldSet({
            labelWidth:  110,
            border:      false,
            items:       optForms
        });

        this.m_submitButton = new Ext.Toolbar.Button({
            text:    this.locale.tripPlanner.labels.planTrip,
            id:      'trip-submit',
            scope:   this,
            style:'background-color:FF0000;',
            handler: this.submit
        });

        console.log("make the m_panel FormPanel");
        var conf = {
            title:       this.locale.tripPlanner.labels.tabTitle,
            id:          'form-tab',
            buttonAlign: 'center',
            border:      false,
            keys:        {key: [10, 13], scope: this, handler: this.submit},
            items:       [  fromToFP,
                            optFP,
                            new Ext.form.Hidden({name: 'toCoord',   value: ''}),
                            new Ext.form.Hidden({name: 'fromCoord', value: ''})
                         ],
            buttons:     [this.m_submitButton],

            // configure how to read the XML Data
            reader:      this.m_xmlRespRecord,

            // reusable error reader class defined at the end of this file
            errorReader: new Ext.form.XmlErrorReader()
        };
        this.m_panel = new Ext.FormPanel(conf);

        console.log("set callbacks on the m_panel FormPanel");
        this.m_panel.on({
                scope:           this,
                beforeaction:    this.preSubmit,
                actionfailed:    this.submitFailure,
                actioncomplete:  this.submitSuccess
        });

        console.log("exit Forms.makeMainPanel()");
    },

    /**
     * from & to form creation
     */
    makeFromToForms : function()
    {
        console.log("enter Forms.makeFromToForms()");
        
        // step 1: these give the from & to forms 'memory' -- all submitted strings are saved off in the cookie for latter use
        Ext.state.Manager.setProvider(new Ext.state.CookieProvider());
        Ext.state.Manager.getProvider();

        // step 2: create the forms
        this.m_fromForm = new otp.core.ComboBox({layout: 'anchor', id: 'from.id', name: 'fromPlace', label: this.locale.tripPlanner.labels.from});
        this.m_toForm   = new otp.core.ComboBox({layout: 'anchor', id: 'to.id',   name: 'toPlace',   label: this.locale.tripPlanner.labels.to});
        var rev  = new Ext.Button({
            tooltip:   this.locale.buttons.reverseMiniTip,
            iconCls:   "reverse-button",
            hideLabel: true,
            scope:     this,
            tabIndex:  -1,
            handler : function(obj)
            {
                this.m_fromForm.reverse(this.m_toForm);
                otp.util.AnalyticsUtils.gaEvent(otp.util.AnalyticsUtils.TRIP_FORM_REVERSE);
            }
        });

        // change events will clear out the from/to coordinate forms (don't want coords from old places getting confused with new requests)
        this.m_fromForm.getComboBox().on({scope: this, focus  : this.clearFrom  });
        this.m_fromForm.getComboBox().on({scope: this, select : this.selectFrom });
        this.m_toForm.getComboBox().on(  {scope: this, focus  : this.clearTo    });
        this.m_toForm.getComboBox().on(  {scope: this, select : this.selectTo   });

        this.m_date = new Ext.form.DateField({
            id:         'trip-date-form',
            fieldLabel: this.locale.tripPlanner.labels.date,
            name:       'date',
            format:     'm/d/Y',
            allowBlank: false,
            anchor:     "95%",
            value:      new Date().format('m/d/Y')
        });

        this.m_arrStore = otp.util.ExtUtils.makeStaticPullDownStore(this.locale.tripPlanner.arriveDepart);
        this.m_arrForm  = new Ext.form.ComboBox({
            id:             'trip-arrive-form',
                name:           'Arr',
                hiddenName:     'Arr',
                fieldLabel:     this.locale.tripPlanner.labels.when,
                store:          this.m_arrStore,
                value:          this.m_arrStore.getAt(0).get('opt'),
                displayField:   'text',
                valueField:     'opt',
                anchor:         this.FIELD_ANCHOR,
                mode:           'local',
                triggerAction:  'all',
                editable:       false,
                allowBlank:     false,
                lazyRender:     false,
                typeAhead:      true,
                forceSelection: true,
                selectOnFocus:  true
        });

        this.m_time = new Ext.ux.form.Spinner({
                id         : 'trip-time-form',
                fieldLabel : this.locale.tripPlanner.labels.when,
                accelerate : true,
                width      : 85,
                value      : new Date().format('g:i a'),
                strategy   : new Ext.ux.form.Spinner.TimeStrategy({format:'g:i a'}),
                name       : 'time'
        });

        var timePanel = {
            xtype:    'panel',
            border:    false,
            layout:    'column',
            autoWidth: true,
            items: [
                {
                    columnWidth: 0.37,
                    layout: 'form',
                    border: false,
                    items: [this.m_arrForm]
                }
                ,
                {
                    columnWidth: 0.33,
                    layout: 'anchor',
                    border: false,
                    //labelWidth: 5,
                    items: [this.m_date]
                }
                ,
                {
                    columnWidth: 0.30,
                    layout: 'anchor',
                    border: false,
                    //labelWidth: 5,
                    items: [this.m_time]
                }
            ]
        };

        console.log("exit Forms.makeFromToForms()");

        return [this.m_fromForm.getComboBox(), rev, this.m_toForm.getComboBox(), timePanel]; 
    },


    /**
     * makes the forms for things like walk distance and mode (bus / train / both) 
     */
    makeOptionForms : function()
    {
        console.log("enter Forms.makeOptionsForms()");
        
        this.m_walkStore     = otp.util.ExtUtils.makeStaticPullDownStore(this.locale.tripPlanner.walkDistance);
        this.m_optimizeStore = otp.util.ExtUtils.makeStaticPullDownStore(this.locale.tripPlanner.options);
        this.m_modeStore     = otp.util.ExtUtils.makeStaticPullDownStore(this.locale.tripPlanner.mode);

        this.m_optimizeForm = new Ext.form.ComboBox({
            id:             'trip-optimize-form',
            name:           'Min',
            hiddenName:     'Min',
            fieldLabel:     this.locale.tripPlanner.labels.minimize,
            store:          this.m_optimizeStore,
            value:          this.m_optimizeStore.getAt(1).get('opt'),
            displayField:   'text',
            valueField:     'opt',
            anchor:         this.FIELD_ANCHOR,
            mode:           'local',
            triggerAction:  'all',
            editable:       false,
            allowBlank:     false,
            lazyRender:     false,
            typeAhead:      true,
            forceSelection: true,
            selectOnFocus:  true
        });

        this.m_walkForm = new Ext.form.ComboBox({
            id:             'trip-walking-form',
            name:           'Walk',
            hiddenName:     'Walk',
            fieldLabel:     this.locale.tripPlanner.labels.walk,
            store:          this.m_walkStore,
            value:          this.m_walkStore.getAt(2).get('opt'),
            displayField:   'text',
            valueField:     'opt',
            anchor:         this.FIELD_ANCHOR,
            mode:           'local',
            triggerAction:  'all',
            editable:       false,
            allowBlank:     false,
            lazyRender:     false,
            typeAhead:      true,
            forceSelection: true,
            selectOnFocus:  true
        });

        this.m_modeForm  = new Ext.form.ComboBox({
            id:             'trip-mode-form',
            name:           'Mode',
            hiddenName:     'Mode',
            fieldLabel:     this.locale.tripPlanner.labels.mode,
            store:          this.m_modeStore,
            value:          this.m_modeStore.getAt(0).get('opt'),
            displayField:   'text',
            valueField:     'opt',
            anchor:         this.FIELD_ANCHOR,
            mode:           'local',
            triggerAction:  'all',
            editable:       false,
            allowBlank:     false,
            lazyRender:     false,
            typeAhead:      true,
            forceSelection: true,
            selectOnFocus:  true
        });

        console.log("exit Forms.makeOptionsForms()");

        return [this.m_optimizeForm, this.m_walkForm, this.m_modeForm];
    },

    CLASS_NAME: "otp.planner.Forms"
};

otp.planner.Forms = new otp.Class(otp.planner.StaticForms);
