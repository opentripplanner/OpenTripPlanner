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
otp.locale.Hungarian = {

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
        fromHere         : "Útvonal kezdete itt",
        toHere           : "Útvonal vége itt",
        intermediateHere : "Add intermediate point",  // TODO localize

        centerHere       : "Térkép középre helyezése ide",
        zoomInHere       : "Közelítés ide",
        zoomOutHere      : "Távolítás innen",
        previous         : "Legutóbbi térképpozíció",
        next             : "Következő térképpozíció"
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
        weekdays:  "Hétköznap",
        saturday:  "Szombat",
        sunday:    "Vasárnap",
        schedule:  "Menetrend"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Dátum",
        loading    : "Betöltés",
        searching  : "Keresés...",
        qEmptyText : "Cím, útkereszteződés, jellegzetes pont vagy megálló azonosító..."
    },

    buttons: 
    {
        reverse       : "Megfordítás",
        reverseTip    : "<b>Irány megfordítása</b><br/>Visszaút tervezése a kezdő- és végpont megfordításával, és az idő későbbre állításával.",
        reverseMiniTip: "Irány megfordítása",

        edit          : "Szerkesztés",
        editTip       : "<b>Útvonal szerkesztése</b><br/>Vissza a fő útvonaltervező beviteli űrlaphoz ennek az útvonalnak a részleteivel.",

        clear         : "Törlés",
        clearTip      : "<b>Törlés</b><br/>A térkép és az összes aktív eszköz törlése.",

        fullScreen    : "Teljes képernyő",
        fullScreenTip : "<b>Teljes képernyő</b><br/>Eszközpanel megjelenítése -vagy- elrejtése",

        print         : "Nyomtatás",
        printTip      : "<b>Nyomtatás</b><br/>Az útvonalterv nyomtatóbarát változata (térkép nélkül).",

        link          : "Hivatkozás",
        linkTip      : "<b>Hivatkozás</b><br/>Hivatkozó URL megjelenítése ehhez az útvonaltervhez.",

        feedback      : "Visszajelzés",
        feedbackTip   : "<b>Visszajelzés</b><br/>Küldje el gondolatait vagy tapasztalatait a térképpel kapcsolatban",

        submit       : "Küldés",
        clearButton  : "Törlés",
        ok           : "OK",
        cancel       : "Mégse",
        yes          : "Igen",
        no           : "Nem",
// TODO
        showDetails  : "Show details...",
        hideDetails  : "Hide details..."
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "délkelet",
        southwest:      "délnyugat",
        northeast:      "északkelet",
        northwest:      "északnyugat",
        north:          "észak",
        west:           "nyugat",
        south:          "dél",
        east:           "kelet",
        bound:          "határ",
        left:           "balra",
        right:          "jobbra",
        slightly_left:  "enyhén balra",
        slightly_right: "enyhén jobbra",
        hard_left:      "élesen balra",
        hard_right:     "élesen jobbra",
        'continue':     "haladjon ezen:",
        to_continue:    "haladjon ezen:",
        becomes:        "evvé válik:",
        at:             "ide:",
// TODO
        on:             "on",
        to:             "to",
        via:            "via",
        circle_counterclockwise: "take roundabout counterclockwise",
        circle_clockwise:        "take roundabout clockwise"
    },

    // see otp.planner.Templates for use ... these are used on the trip itinerary 
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
        agency_msg_tt: "Open agency website in separate window...", // TODO
        about        : "About",
        stop_id      : "Stop ID",
        trip_details : "útvonal Details",
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
        minute        : "minute",
        minutes       : "minutes",
        minute_abbrev : "perc",
        minutes_abbrev: "perc",
        second_abbrev : "másodperc",
        seconds_abbrev: "másodperc",
        months:         ['jan', 'feb', 'már', 'ápr', 'máj', 'jún', 'júl', 'aug', 'szep', 'okt', 'nov', 'dec']
    },
    
    systemmap :
    {
        labels :
        {
            panelTitle : "Rendszertérkép"
        }
    },

    tripPlanner :
    {
        labels : 
        {
            panelTitle    : "Útvonaltervező",
            tabTitle      : "Útvonal tervezése",
            inputTitle    : "Útvonal részletei",
            optTitle      : "Útvonal beállításai (opcionális)",
            submitMsg     : "Útvonal tervezése...",
            optionalTitle : "",
            date          : "Dátum",
            time          : "Idő",
            when          : "Mikor",
            from          : "Honnan",
            fromHere      : "Innen",
            to            : "Hová",
            toHere        : "Ide",
            intermediate  : "Intermediate Place",          // TODO
            minimize      : "A következő megjelenítése",
            maxWalkDistance: "Maximális gyaloglás",
            maxBikeDistance: "Maximális bike",              // TODO
            arriveDepart  : "Érkezés/indulás",
            mode          : "Utazás ezzel",
            wheelchair    : "Kerekesszékkel megtehető útvonal", 
            go            : "Menj",
            planTrip      : "Útvonal tervezése",
            newTrip       : "Új útvonal"
        },

        // see otp/config.js for where these values are used
        link : 
        {
//TODO
            text           : "Link to this trip (OTP)",
            trip_separator : "This trip on other transit planners",
            bike_separator : "On other bike trip planners",
            walk_separator : "On other walking direction planners",
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.hu"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
//TODO
            working      : "Looking up address ....",
            error        : "Did not receive any results",
            msg_title    : "Kérem review trip plan",
            msg_content  : "Kérem correct errors before planning your trip",
            select_result_title : "Please select a result",
            address_header : "Address"
        },

        error:
        {
            title        : 'Útvonaltervező hiba',
            deadMsg      : "A térképes útvonaltervező jelenleg nem válaszol. Kérem, várjon néhány percet, és próbálja újra, vagy próbálja a szöveges útvonaltervezővel (lásd a hivatkozást alább).",
            geoFromMsg   : "Kérem, válasszon kezdőpontot az útvonalhoz: ",
            geoToMsg     : "Kérem, válasszon végpontot az útvonalhoz: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Tervezés OK",
            500: "Szerverhiba",
            400: "Az útvonal határon kívül",
            404: "Nem található útvonal",
            406: "Nincs közlekedési idő",
            408: "A kérés túllépte az időt",
            413: "Érvénytelen paraméter",
            440: "A kezdőpont geokódja nem található",
            450: "A végpont geokódja nem található",
            460: "A kezdő- és végpont geokódja nem található",
            409: "Túl közel",
            340: "A kezdőpont geokódja nem egyértelmű",
            350: "A végpont geokódja nem egyértelmű",
            360: "A kezdő- és végpont geokódja nem egyértelmű"
        },

        options: 
        [
          ['TRANSFERS', 'Legkevesebb átszállással'],
          ['QUICK',     'Leggyorsabb útvonal'],
          ['SAFE',      'Legbiztonságosabb útvonal'],
          ['TRIANGLE',  'Custom trip...']  // TODO localize
        ],
    
        arriveDepart: 
        [
          ['false', 'Indulás'], 
          ['true',  'Érkezés']
        ],
    
        maxWalkDistance : 
        [
            ['100',   '100 m'],
            ['500',   '500 m'],
            ['1000',  '1 km'],
            ['5000',  '5 km'],
            ['10000', '10 km'],
            ['50000', '50 km'],
            ['100000','100 km']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Tömegközlekedés'],
            ['BUSISH,TRAINISH,WALK', 'Busz és vonat'],
            ['BUSISH,WALK', 'Csak busz'],
            ['TRAINISH,WALK', 'Csak vonat'],
            ['WALK', 'Csak gyalog'],
            ['BICYCLE', 'Kerékpár'],
            ['TRANSIT,BICYCLE', 'Tömegközlekedés és kerékpár']
        ],

        wheelchair :
        [
            ['false', 'Nem szükséges'],
            ['true', 'Szükséges']
        ]
    },

    CLASS_NAME : "otp.locale.Hungarian"
};