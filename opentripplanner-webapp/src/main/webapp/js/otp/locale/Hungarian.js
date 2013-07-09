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
        metricsSystem : "international",
        rightClickMsg : "A jobb gombbal kijelölheted a térképen az utazásod kezdő és végpontját.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"
        }
    },

    contextMenu : 
    {
        fromHere         : "Útvonal innen",
        toHere           : "Útvonal ide",
        intermediateHere : "Közbenső pont hozzáadása",

        centerHere       : "Térkép középre helyezése ide",
        zoomInHere       : "Közelítés ide",
        zoomOutHere      : "Távolítás innen",
        previous         : "Legutóbbi térképpozíció",
        next             : "Következő térképpozíció"
    },

    bikeTriangle : 
    {
        safeName : "Kerékpárbarát",
        safeSym  : "B",

        hillName : "Lapos",
        hillSym  : "L",

        timeName : "Gyors",
        timeSym  : "Gy"
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
        qEmptyText : "Cím, útkereszteződés, jellegzetes pont vagy megállókód..."
    },

    buttons: 
    {
        reverse       : "Visszafele",
        reverseTip    : "<b>Irány megfordítása</b><br/>Visszaút tervezése a kezdő- és végpont megfordításával, és az idő későbbre állításával.",
        reverseMiniTip: "Irány megfordítása",

        edit          : "Modosítás",
        editTip       : "<b>Útvonal modosítása</b><br/>Vissza a fő útvonaltervező beviteli űrlaphoz ennek az útvonalnak a részleteivel.",

        clear         : "Törlés",
        clearTip      : "<b>Törlés</b><br/>A térkép és az összes aktív eszköz törlése.",

        fullScreen    : "Teljes képernyő",
        fullScreenTip : "<b>Teljes képernyő</b><br/>Eszközpanel megjelenítése -vagy- elrejtése",

        print         : "Nyomtatás",
        printTip      : "<b>Nyomtatás</b><br/>Az útvonalterv nyomtatóbarát változata (térkép nélkül).",

        link          : "Hivatkozás",
        linkTip       : "<b>Hivatkozás</b><br/>Hivatkozó URL megjelenítése az útvonaltervhez.",

        feedback      : "Visszajelzés",
        feedbackTip   : "<b>Visszajelzés</b><br/>Küldje el gondolatait vagy tapasztalatait a térképpel kapcsolatban",

        submit       : "Küldés",
        clearButton  : "Törlés",
        ok           : "OK",
        cancel       : "Mégse",
        yes          : "Igen",
        no           : "Nem",
        showDetails  : "&darr; Részletek... &darr;",
        hideDetails  : "&uarr; Elrejtés... &uarr;"
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
        circle_counterclockwise: "a körforgalomban balra",
        circle_clockwise:        "a körforgalomban jobbra",
        // rather than just being a direction, this should be
        // full-fledged to take just the exit name at the end
        elevator: "take elevator to"
    },

    // see otp.planner.Templates for use
    instructions :
    {
// TODO
        walk         : "Séta",
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

        transfer     : "átszállás",
        transfers    : "átszállás",

        continue_as  : "Folytatodik mint:",
        stay_aboard  : "Maradjon a fedélzeten",

        depart       : "Indul",
        arrive       : "Érkezik",

        start_at     : "Kezdés",
        end_at       : "Érkezés"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "Üzemeltető: ",
        agency_msg_tt: "Honlap megnyítása uj ablakban...",
        about        : "Körülbelül",
        stop_id      : "Megállókód: ",
        trip_details : "Útvonal részletek",
        travel       : "Utazás",
        valid        : "Érvényes",
        trip_length  : "Hossz",
        with_a_walk  : "Séta",
        alert_for_rt : "Alert for route",
        fare         : "Ár",
        regular_fare : "Normál",
        student_fare : "Diák",
        senior_fare  : "Nyugdíjas",
        fare_symbol  : "HUF"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg headers
    modes :
    {
        WALK:           "SÉTA",
        BICYCLE:        "KERÉKPÁR",
        CAR:            "AUTÓ",
        TRAM:           "VILLAMOS",
        SUBWAY:         "METRÓ",
        RAIL:           "VASÚT",
        BUS:            "BUSZ",
        FERRY:          "KOMP",
        CABLE_CAR:      "LIBEGŐ",
        GONDOLA:        "GONDOLA",
        FUNICULAR:      "SIKLÓ"
    },

    ordinal_exit:
    {
        1:  "az első kijáratig",
        2:  "a második kijáratig",
        3:  "a harmadik kijáratig",
        4:  "a negyedik kijáratig",
        5:  "az ötödik kijáratig",
        6:  "a hatodik kijáratig",
        7:  "a hetedik kijáratig",
        8:  "a nyolcadik kijáratig",
        9:  "a kilencedik kijáratig",
        10: "a tízedik kijáratig"
    },

    time:
    {
        hour_abbrev    : "óra",
        hours_abbrev   : "óra",
        hour           : "óra",
        hours          : "óra",

        minute        : "perc",
        minutes       : "perc",
        minute_abbrev : "perc",
        minutes_abbrev: "perc",
        second_abbrev : "mp",
        seconds_abbrev: "mp",
        format        : "D, j M H:i",
        date_format   : "Y-m-d",
        time_format   : "H:i",
        months        : ['jan', 'feb', 'már', 'ápr', 'máj', 'jún', 'júl', 'aug', 'szep', 'okt', 'nov', 'dec']
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "Vonalhálozat"
        }
    },

    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used
        labels : 
        {
            panelTitle    : "Útazástervező",
            tabTitle      : "Útazástervező",
            inputTitle    : "Útvonal részletek",
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
            intermediate  : "Közbenső pont",
            minimize      : "A következő megjelenítése",
            maxWalkDistance: "Maximális gyaloglás",
            walkSpeed     : "Séta sebessége",
            maxBikeDistance: "Maximális kerékpározás",
            bikeDistance  : "Lerékpáros sebesség",
            arriveDepart  : "Érkezés/indulás",
            mode          : "Közlekedés",
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
            working      : "Cím keresése ....",
            error        : "Did not receive any results",
            msg_title    : "Kérem review trip plan",
            msg_content  : "Kérem correct errors before planning your trip",
            select_result_title : "Please select a result",
            address_header : "Address"
        },

        error:
        {
            title        : 'Útvonaltervezési hiba',
            deadMsg      : "A térképes útvonaltervező jelenleg nem válaszol. Kérem, várjon néhány percet, és próbálja újra.",
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
          ['TRANSFERS', 'Kevés átszállással'],
          ['QUICK',     'Gyors útvonallal'],
          ['SAFE',      'Biztonságos útvonallal'],
          ['TRIANGLE',  'Egyedi...']
        ],
    
        arriveDepart: 
        [
          ['false', 'Indulás'], 
          ['true',  'Érkezés']
        ],
    
        maxWalkDistance : 
        [
            [   '100',  '100 m'],
            [   '500',  '500 m'],
            [  '1000',   '1 km'],
            [  '5000',   '5 km'],
            [ '10000',  '10 km'],
            [ '50000',  '50 km'],
            ['100000', '100 km']
        ],

        walkSpeed :
        [
            ['0.556',  '2 km/h'],
            ['1.111',  '4 km/h'],
            ['1.667',  '6 km/h'],
            ['2.222',  '8 km/h'],
            ['2.778', '10 km/h']
        ],

        mode : 
        [
            ['TRANSIT,WALK', 'közösségi közlekedéssel'],
            ['BUSISH,WALK', 'busszal'],
            ['TRAINISH,WALK', 'vonattal'],
            ['WALK', 'gyalog'],
            ['BICYCLE', 'kerékpárral'],
            ['TRANSIT,BICYCLE', 'közösségi közlekedéssel és kerékpárral']
        ],

        // TODO: remove this hack, and provide code that allows the mode array to be configured with different transit modes.
        //       (note that we've been broken for awhile here, since many agencies don't have a 'Train' mode either...this needs attention)
        // IDEA: maybe we start with a big array (like below), and the pull out modes from this array when turning off various modes...
        with_bikeshare_mode : 
        [
            ['TRANSIT,WALK', 'közösségi közlekedéssel'],
            ['BUSISH,WALK', 'busszal'],
            ['TRAINISH,WALK', 'vonattal'],
            ['WALK', 'gyalog'],
            ['BICYCLE', 'kerékpárral'],
            ['WALK,BICYCLE', 'közbringával (Bubi)'],
            ['TRANSIT,BICYCLE', 'közösségi közlekedéssel és kerékpárral'],
            ['TRANSIT,WALK,BICYCLE', 'közösségi közlekedéssel és közbringával']
        ],

        wheelchair :
        [
            ['false', 'Nem szükséges'],
            ['true', 'Szükséges']
        ]
    },

    CLASS_NAME : "otp.locale.Hungarian"
};
