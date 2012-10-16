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

otp.namespace("otp.locale");

/**
  * @class
  */
otp.locale.Gaelic = {

    config :
    {
// TODO
        metricsSystem : "international",
        rightClickMsg : "TODO - localize me and tripPlanner.link below - Right-click on the map to designate the start and end of your trip.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"
        }
    },

    contextMenu : 
    {
        fromHere         : "Tosaigh turas anseo",
        toHere           : "Críochnaigh turas anseo",
        intermediateHere : "Add intermediate point",  // TODO localize

        centerHere       : "Cur lár an léarscáil anseo",
        zoomInHere       : "Zúmáil amach anseo",
        zoomOutHere      : "Zúmáil isteach anseo",
        previous         : "Áit ar an léarscáil níos déanaí",
        next             : "Céad áit eile ar an léarscáil"
    },

    // TODO Localize Me
    bikeTriangle : 
    {
        safeName : "Bike friendly",
        safeSym  : "B",

        hillName : "Flat",
        hillSym  : "F",

        timeName : "Quick",
        timeSym  : "Q"
    },

    service : 
    {
        weekdays:  "I rith na seachtaine",
        saturday:  "Satharn",
        sunday:    "Domhnach",
        schedule:  "Sceideal"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Dáta",
        loading    : "Ag luchtach",
        searching  : "Ag cuardach....",
        qEmptyText : "Seoladh, crosbhealach, sainchomhartha, nó Stop ID..."
    },

    buttons: 
    {
        reverse       : "Ar ais",
        reverseTip    : "<b>treoracha ar ais</b><br/>Plean turas fillte ag aisiompú na bpointí tús agus críche an turais seo, agus ag choigeartú an am seo ar aghaidh.",
        reverseMiniTip: "treoracha ar ais",

        edit          : "Aithrigh",
        editTip       : "<b>Aithrigh turas</b><br/>Fill ar ais go dtí príomhfoirm pleanálaí turais le sonraí an turas seo.",

        clear         : "Glan",
        clearTip      : "<b>Glan</b><br/>Glan an léarscáil agus na huirlisí uile atá gníomhach.",

        fullScreen    : "Scáileán Iomlán",
        fullScreenTip : "<b>Scáileán Iomlán</b><br/>Teaspáin -nó- cur i bhfolach painéil uirlis.",

        print         : "Priontáil",
        printTip      : "<b>Priontáil</b><br/>Leagan an turasphlean réidh le phriontáil (gan léarscáil).",

        link          : "Nasc",
        linkTip      : "<b>Nasc</b><br/>Teaspain URL an nasc don turasphlean seo.",

        feedback      : "Aiseolas",
        feedbackTip   : "<b>Aiseolas</b><br/>Seol do thuaraim nó faoin léarscáil.",

        submit       : "Cur",
        clearButton  : "Glan",
        ok           : "OK",
        cancel       : "Dúiltaigh",
        yes          : "Ar aghaidh",
        no           : "Ní",
// TODO
        showDetails  : "Show details...",
        hideDetails  : "Hide details..."
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "oirdheas",
        southwest:      "iardheas",
        northeast:      "oirthuaidh",
        northwest:      "iarthuaidh",
        north:          "thuaidh",
        west:           "thiar",
        south:          "theas",
        east:           "thoir",
        bound:          "faoi cheangal",
        left:           "Ar chlé",
        right:          "Ar dheis",
        slightly_left:  "beagán ar chlé",
        slightly_right: "beagán ar dheis",
        hard_left:      "lán ar chlé",
        hard_right:     "lán ar dheis",
        'continue':     "lean ar aghaidh",
        to_continue:    "chun leanúint ar",
        becomes:        "go dti do n-aithríonn sé go",
        at:             "ag",
// TODO
        on:             "on",
        to:             "to",
        via:            "via",
        circle_counterclockwise: "take roundabout counterclockwise",
        circle_clockwise:        "take roundabout clockwise"
    },


    // see otp.planner.Templates for use
    instructions :
    {
// TODO
        walk         : "Walk",
        walk_toward  : "Walk",
        walk_verb    : "Walk",
        bike         : "Bike",
        bike_toward  : "Bike",
        bike_verb    : "Bike",
        drive        : "Drive",
        drive_toward : "Drive",
        drive_verb   : "Drive",
        move         : "Proceed",
        move_toward  : "Proceed",

        transfer     : "transfer",
        transfers    : "transfers",

        continue_as  : "Continues as",
        stay_aboard  : "stay on board",

        depart       : "Depart",
        arrive       : "Arrive",

        start_at     : "Start at",
        end_at       : "End at"
    },

    // see otp.planner.Templates for use
    labels : 
    {
// TODO
        agency_msg   : "Service run by", // TODO
        about        : "About",
        stop_id      : "Stop ID",
        trip_details : "Weee Trip Details",
        fare         : "Fare",
        fare_symbol  : "\u20ac",

        // TODO  -- used in the Trip Details summary to describe different fares 
        regular_fare : "",
        student_fare : "",
        senior_fare  : "",

        travel       : "Travel",
        valid        : "Valid",
        trip_length  : "Time",
        with_a_walk  : "with a walk of",
        alert_for_rt : "Alert for route"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes : 
    {
// TODO
        WALK:           "WALK",
        BICYCLE:        "BICYCLE",
        CAR:            "CAR",
        TRAM:           "TRAM",
        SUBWAY:         "SUBWAY",
        RAIL:           "RAIL",
        BUS:            "BUS",
        FERRY:          "FERRY",
        CABLE_CAR:      "CABLE CAR",
        GONDOLA:        "GONDOLA",
        FUNICULAR:      "FUNICULAR"
    },

    ordinal_exit:
    {
// TODO
        1:  "to first exit",
        2:  "to second exit",
        3:  "to third exit",
        4:  "to fourth exit",
        5:  "to fifth exit",
        6:  "to sixth exit",
        7:  "to seventh exit",
        8:  "to eighth exit",
        9:  "to ninth exit",
        10: "to tenth exit"
    },

    time:
    {
        // TODO
        hour_abbrev    : "hour",
        hours_abbrev   : "hours",
        hour           : "hour",
        hours          : "hours",

// TODO
        format        : "D, j M H:i",
        date_format   : "d-m-Y",
        time_format   : "H:i",
        minute        : "nóiméad",
        minutes       : "nóiméid",
        minute_abbrev : "nóiméad",
        minutes_abbrev: "nóiméid",
        second_abbrev : "seacaind",
        seconds_abbrev: "seacaind",
        months:         ['Ean', 'Fea', 'Máirt', 'Aib', 'Beal', 'Meith', 'Iúil', 'Lún', 'M.Fr', 'D.Fr', 'Sam', 'Nollaig']
    },
    
    systemmap :
    {
        labels :
        {
            panelTitle : "Sisteam"
        }
    },

    tripPlanner :
    {
        labels : 
        {
            panelTitle    : "Pleanálaí Turais",
            tabTitle      : "Pleanáil turas",
            inputTitle    : "Sonraí turais",
            optTitle      : "roghanna turais (roghnach)",
            submitMsg     : "Ag pleanáil do thuras...",
            optionalTitle : "",
            date          : "Dáta",
            time          : "Am",
            when          : "Cathain",
            from          : "Ó",
            fromHere      : "Ón áit seo",
            to            : "Go",
            toHere        : "Go dtí seo",
            intermediate  : "Intermediate Place",          // TODO
            minimize      : "Teaspáin dom an",
            maxWalkDistance: "Súil is faide",
            walkSpeed     : "Siúl luas",
            maxBikeDistance: "Súil is bike",              // TODO
            bikeSpeed     : "luas rothar",                // TODO

            arriveDepart  : "Sroic roimh/Imigh ag",
            mode          : "Iompair ar",
            wheelchair    : "Turas oiriúnach do chathaoireacha rothaí", 
            go            : "Imigh",
            planTrip      : "Pleanáil do thuras",
            newTrip       : "Turas nua"
        },

        // see otp/config.js for where these values are used
        link : 
        {
// TODO
            text           : "Link to this trip (OTP)",
            trip_separator : "This trip on other transit planners",
            bike_separator : "On other bike trip planners",
            walk_separator : "On other walking direction planners",
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.ie"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
//TODO
            working      : "Looking up address ....",
            error        : "Did not receive any results",
            msg_title    : "Wee review trip plan",
            msg_content  : "Wee correct errors before planning your trip",
            select_result_title : "Please select a result",
            address_header : "Address"
        },

        error:
        {
            title        : "Thárla earráid sa phleanálaí turais",
            deadMsg      : "Tá pleanálaí léarscáil an dturais gan freagra a thabhairt faoi láthair. Fan ar feadh cúpla nóiméad chun iarracht eile a dhéanamh, nó déan iarracht as an phleanálaí turais téacs (féach an nasc thíos).",
            geoFromMsg   : "Roghnaigh an suíomh 'Ó' do do thurais: ",
            geoToMsg     : "Roghnaigh an suíomh 'Go dtí' do do thurais:"
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Plean OK",
            500: "Earráid Fhreastalaí",
            400: "Turas as teorainn",
            404: "Ní bhfuarthas an tslí",
            406: "Nil amanna idirthurais ar fáil",
            408: "Iarratas imithe thar am",
            413: "Paraiméadar neamhbhailí",
            440: "Ní bhfuarthas an geocode 'Ó'",
            450: "Ní bhfuarthas an geocode 'Go dtí'",
            460: "Ní bhfuarthas na geocodes 'Ó' nó 'Go dtí'",
            409: "Ró-chóngarach",
            340: "Geocode 'Ó' débhríoch",
            350: "Geocode 'Go dtí' débhríoch",
            360: "Geocode 'Ó' agus geocode 'Go dtí' débhríoch"
        },

        options: 
        [
          ['TRANSFERS', 'Aistrithe is lú'],
          ['QUICK',     'Turas is tapúla'],
          ['SAFE',      'Turas is sabháilte'],
          ['TRIANGLE',  'Custom trip...']  // TODO localize
        ],
    
        arriveDepart: 
        [
          ['false', 'Imeacht'], 
          ['true',  'Teacht']
        ],
    
        maxWalkDistance : 
        [
            ['200',   '200 méadar'],
            ['500',   '500 méadar'],
            ['1000',  '1 ciliméadar'],
            ['1500',  '1.5 ciliméadar'],
            ['5000',  '5 chiliméadar'],
            ['10000', '10 chiliméadar']
        ],
            
        walkSpeed :
        [
         ['0.447',  '0.5 km/u'],
         ['0.894',  '0.9 km/u'],
         ['1.341',  '1.3 km/u'],
         ['1.788',  '1.8 km/u'],
         ['2.235',  '2.3 km/u']
         ],

        mode : 
        [
            ['TRANSIT,WALK', 'Idirthurais'],
            ['BUSISH,TRAINISH,WALK', 'Bus & traen'],
            ['BUSISH,WALK', 'Bus amháin'],
            ['TRAINISH,WALK', 'Traen amháin'],
            ['WALK', 'Súil amháin'],
            ['BICYCLE', 'Rothar'],
            ['TRANSIT,BICYCLE', 'Idirthurais & Rothar']
        ],

        wheelchair :
        [
            ['false', 'Ní gá'],
            ['true', 'Riachtanach']
        ]
    },

    CLASS_NAME : "otp.locale.Gaelic"
};
