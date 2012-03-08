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
        toHere           : "CrÃ­ochnaigh turas anseo",
        intermediateHere : "Add intermediate point",  // TODO localize

        centerHere       : "Cur lÃ¡r an lÃ©arscÃ¡il anseo",
        zoomInHere       : "ZÃºmÃ¡il amach anseo",
        zoomOutHere      : "ZÃºmÃ¡il isteach anseo",
        previous         : "Ã�it ar an lÃ©arscÃ¡il nÃ­os dÃ©anaÃ­",
        next             : "CÃ©ad Ã¡it eile ar an lÃ©arscÃ¡il"
    },

    // TODO Localize Me
    bikeTriangle : 
    {
        safeName : "Safest",
        safeSym  : "S",

        hillName : "Flattest",
        hillSym  : "F",

        timeName : "Quickest",
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
        date       : "DÃ¡ta",
        loading    : "Ag luchtach",
        searching  : "Ag cuardach....",
        qEmptyText : "Seoladh, crosbhealach, sainchomhartha, nÃ³ Stop ID..."
    },

    buttons: 
    {
        reverse       : "Ar ais",
        reverseTip    : "<b>treoracha ar ais</b><br/>Plean turas fillte ag aisiompÃº na bpointÃ­ tÃºs agus crÃ­che an turais seo, agus ag choigeartÃº an am seo ar aghaidh.",
        reverseMiniTip: "treoracha ar ais",

        edit          : "Aithrigh",
        editTip       : "<b>Aithrigh turas</b><br/>Fill ar ais go dtÃ­ prÃ­omhfoirm pleanÃ¡laÃ­ turais le sonraÃ­ an turas seo.",

        clear         : "Glan",
        clearTip      : "<b>Glan</b><br/>Glan an lÃ©arscÃ¡il agus na huirlisÃ­ uile atÃ¡ gnÃ­omhach.",

        fullScreen    : "ScÃ¡ileÃ¡n IomlÃ¡n",
        fullScreenTip : "<b>ScÃ¡ileÃ¡n IomlÃ¡n</b><br/>TeaspÃ¡in -nÃ³- cur i bhfolach painÃ©il uirlis.",

        print         : "PriontÃ¡il",
        printTip      : "<b>PriontÃ¡il</b><br/>Leagan an turasphlean rÃ©idh le phriontÃ¡il (gan lÃ©arscÃ¡il).",

        link          : "Nasc",
        linkTip      : "<b>Nasc</b><br/>Teaspain URL an nasc don turasphlean seo.",

        feedback      : "Aiseolas",
        feedbackTip   : "<b>Aiseolas</b><br/>Seol do thuaraim nÃ³ faoin lÃ©arscÃ¡il.",

        submit       : "Cur",
        clearButton  : "Glan",
        ok           : "OK",
        cancel       : "DÃºiltaigh",
        yes          : "Ar aghaidh",
        no           : "NÃ­",
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
        left:           "Ar chlÃ©",
        right:          "Ar dheis",
        slightly_left:  "beagÃ¡n ar chlÃ©",
        slightly_right: "beagÃ¡n ar dheis",
        hard_left:      "lÃ¡n ar chlÃ©",
        hard_right:     "lÃ¡n ar dheis",
        'continue':     "lean ar aghaidh",
        to_continue:    "chun leanÃºint ar",
        becomes:        "go dti do n-aithrÃ­onn sÃ© go",
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
        format        : "D, j M H:i",
        minute        : "nÃ³imÃ©ad",
        minutes       : "nÃ³imÃ©id",
        minute_abbrev : "nÃ³imÃ©ad",
        minutes_abbrev: "nÃ³imÃ©id",
        second_abbrev : "seacaind",
        seconds_abbrev: "seacaind",
        months:         ['Ean', 'Fea', 'MÃ¡irt', 'Aib', 'Beal', 'Meith', 'IÃºil', 'LÃºn', 'M.Fr', 'D.Fr', 'Sam', 'Nollaig']
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
            panelTitle    : "PleanÃ¡laÃ­ Turais",
            tabTitle      : "PleanÃ¡il turas",
            inputTitle    : "SonraÃ­ turais",
            optTitle      : "roghanna turais (roghnach)",
            submitMsg     : "Ag pleanÃ¡il do thuras...",
            optionalTitle : "",
            date          : "DÃ¡ta",
            time          : "Am",
            when          : "Cathain",
            from          : "Ã“",
            fromHere      : "Ã“n Ã¡it seo",
            to            : "Go",
            toHere        : "Go dtÃ­ seo",
            intermediate  : "Intermediate Place",          // TODO
            minimize      : "TeaspÃ¡in dom an",
            maxWalkDistance: "SÃºil is faide",
            maxBikeDistance: "SÃºil is bike",              // TODO
            arriveDepart  : "Sroic roimh/Imigh ag",
            mode          : "Iompair ar",
            wheelchair    : "Turas oiriÃºnach do chathaoireacha rothaÃ­", 
            go            : "Imigh",
            planTrip      : "PleanÃ¡il do thuras",
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
            title        : "ThÃ¡rla earrÃ¡id sa phleanÃ¡laÃ­ turais",
            deadMsg      : "TÃ¡ pleanÃ¡laÃ­ lÃ©arscÃ¡il an dturais gan freagra a thabhairt faoi lÃ¡thair. Fan ar feadh cÃºpla nÃ³imÃ©ad chun iarracht eile a dhÃ©anamh, nÃ³ dÃ©an iarracht as an phleanÃ¡laÃ­ turais tÃ©acs (fÃ©ach an nasc thÃ­os).",
            geoFromMsg   : "Roghnaigh an suÃ­omh 'Ã“' do do thurais: ",
            geoToMsg     : "Roghnaigh an suÃ­omh 'Go dtÃ­' do do thurais:"
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Plean OK",
            500: "EarrÃ¡id FhreastalaÃ­",
            400: "Turas as teorainn",
            404: "NÃ­ bhfuarthas an tslÃ­",
            406: "Nil amanna idirthurais ar fÃ¡il",
            408: "Iarratas imithe thar am",
            413: "ParaimÃ©adar neamhbhailÃ­",
            440: "NÃ­ bhfuarthas an geocode 'Ã“'",
            450: "NÃ­ bhfuarthas an geocode 'Go dtÃ­'",
            460: "NÃ­ bhfuarthas na geocodes 'Ã“' nÃ³ 'Go dtÃ­'",
            409: "RÃ³-chÃ³ngarach",
            340: "Geocode 'Ã“' dÃ©bhrÃ­och",
            350: "Geocode 'Go dtÃ­' dÃ©bhrÃ­och",
            360: "Geocode 'Ã“' agus geocode 'Go dtÃ­' dÃ©bhrÃ­och"
        },

        options: 
        [
          ['TRANSFERS', 'Aistrithe is lÃº'],
          ['QUICK',     'Turas is tapÃºla'],
          ['SAFE',      'Turas is sabhÃ¡ilte'],
          ['TRIANGLE',  'Custom trip...']  // TODO localize
        ],
    
        arriveDepart: 
        [
          ['false', 'Imeacht'], 
          ['true',  'Teacht']
        ],
    
        maxWalkDistance : 
        [
            ['200',   '200 mÃ©adar'],
            ['500',   '500 mÃ©adar'],
            ['1000',  '1 cilimÃ©adar'],
            ['1500',  '1.5 cilimÃ©adar'],
            ['5000',  '5 chilimÃ©adar'],
            ['10000', '10 chilimÃ©adar']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Idirthurais'],
            ['BUSISH,TRAINISH,WALK', 'Bus & traen'],
            ['BUSISH,WALK', 'Bus amhÃ¡in'],
            ['TRAINISH,WALK', 'Traen amhÃ¡in'],
            ['WALK', 'SÃºil amhÃ¡in'],
            ['BICYCLE', 'Rothar'],
            ['TRANSIT,BICYCLE', 'Idirthurais & Rothar']
        ],

        wheelchair :
        [
            ['false', 'NÃ­ gÃ¡'],
            ['true', 'Riachtanach']
        ]
    },

    CLASS_NAME : "otp.locale.Gaelic"
};
