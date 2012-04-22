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

otp.locale.German = {

    config :
    {
        metricsSystem : "international",
        rightClickMsg : "verwenden Sie die rechte Taste Ihrer Maus auf die Karte um die Abfahrt und Ankunft zu wählen",
        attribution   : {
            title   : "Lizenz",
            content : "Nutzungsbedingungen"
        }
    },

    contextMenu : 
    {
        fromHere         : "Ausgangspunkt",
        toHere           : "Zielort",
        intermediateHere : "Add intermediate point", // TODOO

        centerHere       : "zielen Sie auf die Karte",
        zoomInHere       : "Zoom",
        zoomOutHere      : "verkleinern von hier",
        previous         : "vorherigen Position auf der Karte",
        next             : "nächste Position auf der Karte"
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
        weekdays:  "Wochentage",
        saturday:  "Samstag",
        sunday:    "Sonntag",
        schedule:  "Zeitplan"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Datum",
        loading    : "laden",
        searching  : "Suche...",
        qEmptyText : "Addresse, Kreuzung, Bezugspunkt oder Haltstelle ID..."
    },

    buttons: 
    {
        reverse       : "umkehren",
        reverseTip    : "<b>umgekehrter Richtung</b><br/>Rückweg berechnen und Zeitpunkt richtigen",
        reverseMiniTip: "umgekehrter Richtung",

        edit          : "editieren",
        editTip       : "<b>Reise ändern</b><br/>Zurück zur Startseite mit den Details der geplanten Reise.",

        clear         : "löschen",
        clearTip      : "<b>löschen</b><br/>Karte und alle Aktivität löschen.",

        fullScreen    : "Vollbild",
        fullScreenTip : "<b>Vollbild</b><br/>Symbolleiste zeigen oder ausblenden",

        print         : "Drucken",
        printTip      : "<b>Drucken</b><br/>drucken Sie eine vereinfachte Version der Route (ohne Karte).",

        link          : "Link",
        linkTip      : "<b>Link</b><br/>den Link für die gewählte Route zeigen.",

        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Feedback geben",

        submit       : "bestätigen",
        clearButton  : "löschen",
        ok           : "OK",
        cancel       : "abbrechen",
        yes          : "ja",
        no           : "nein",
        showDetails  : "Show details...",
        hideDetails  : "Hide details..."
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southeast:      "Südosten",
        southwest:      "Südwesten",
        northeast:      "Nordosten",
        northwest:      "Nordwest",
        north:          "Norden",
        west:           "Westen",
        south:          "Süden",
        east:           "Osten",
        bound:          "Grenze",
        left:           "links",
        right:          "rechts",
        slightly_left:  "links halten",
        slightly_right: "rechts halten",
        hard_left:      "Links abbiegen",
        hard_right:     "Rechts abbiegen",
        'continue':     "weiterfahren",
        to_continue:    "weiterfahren",
        becomes:        "wird",
        at:             "um",
        on:             "auf",
        to:             "bis",
        via:            "Straße",
        circle_counterclockwise: "nehmen Sie den Kreisverkehr gegen den Uhrzeigersinn",
        circle_clockwise:        "nehmen Sie den Kreisverkehr im Uhrzeigersinn",
        // rather than just being a direction, this should be
        // full-fledged to take just the exit name at the end
        elevator: "take elevator to"     // TODO   
    },

    // see otp.planner.Templates for use
    instructions :
    {
        walk         : "zu Fuß",
        walk_toward  : "gehen",
        walk_verb    : "gehen",
        bike         : "Fahrrad",
        bike_toward  : "Radfahren",
        bike_verb    : "Radfahren",
        drive        : "Fahren",
        drive_toward : "fahren in Richtung",
        drive_verb   : "fahren",
        move         : "weiterfahren",
        move_toward  : "weiterfahren",

        transfer     : "Änderung ",
        transfers    : "Änderungen",

        continue_as  : "Weiterfahren",
        stay_aboard  : "an Bord bleiben",

        depart       : "Abfahrt",
        arrive       : "Ankunft",

        start_at     : "Start",
        end_at       : "Ende"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "Service run by", // TODO
        agency_msg_tt: "Open agency website in separate window...", // TODO
        about        : "Informationen",
        stop_id      : "ID Haltstelle",
        trip_details : "Reisedetails",
        fare         : "Preis",
        fare_symbol  : "\u20ac",
        travel       : "Reise",
        valid        : "gültig",
        trip_length  : "Reisezeit",
        with_a_walk  : "Zu Fuß",
        alert_for_rt : "achten Sie auf den Weg"
    },

    modes : 
    {
        WALK:           "zu fuß",
        BICYCLE:        "Fahrrad",
        CAR:            "Auto",
        TRAM:           "Straßenbahn",
        SUBWAY:         "U-Bahn",
        RAIL:           "Eisenbahn",
        BUS:            "Bus",
        FERRY:          "Fähre",
        CABLE_CAR:      "Standseilbahn",
        GONDOLA:        "Gondel",
        FUNICULAR:      "Seilbahn"
    },

    ordinal_exit:
    {
        1:  "erste Ausfahrt",
        2:  "zweite Ausfahrt",
        3:  "dritte Ausfahrt",
        4:  "vierte Ausfahrt",
        5:  "fünfte Ausfahrt",
        6:  "sechste Ausfahrt",
        7:  "siebte Ausfahrt",
        8:  "achte Ausfahrt",
        9:  "neunte Ausfahrt",
        10: "zehnte Ausfahrt"
    },

    time:
    {
        minute         : "Minute",
        minutes        : "Minuten",
        minute_abbrev  : "min.",
        minutes_abbrev : "min.",
        second_abbrev  : "s.",
        seconds_abbrev : "s.",
        format         : "D, j M H:i",
        date_format    : "d-m-Y",
        time_format    : "H:i",
        months         : ['Jan.', 'Feb.', 'Mär.', 'Apr.', 'Mai.', 'Juni', 'Juli', 'Aug.', 'Sep.', 'Okt.', 'Nov.', 'Dez.']
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "System abbilden"
        }
    },

    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used
        labels : 
        {
            panelTitle    : "Route berechnen",
            tabTitle      : "Neue Route",
            inputTitle    : "Route Details",
            optTitle      : "Route Vorzug (fakultativ)",
            submitMsg     : "Rutenberechnung...",
            optionalTitle : "",
            date          : "Datum",
            time          : "Zeit",
            when          : "Wann",
            from          : "von",
            fromHere      : "von hier",
            to            : "nach",
            toHere        : "nach hier",
            intermediate  : "Intermediate Place", // TODO
            minimize      : "anzeineg",
            maxWalkDistance: "maximale  zu Fuß Gehstrecke",
            maxBikeDistance: "Maximum bike", // TODO
            arriveDepart  : "Ankunft innerhalb von/Abfahrt um",
            mode          : "fahren durch",
            wheelchair    : "Route für Rollstuhlfahrer", 
            go            : "gehen",
            planTrip      : "Eine Route planen",
            newTrip       : "Neue Fahrt"
        },

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "link zu dieser Reise",
            trip_separator : "Diese Reise basiert auf anderen Transit Planer",
            bike_separator : "Auf andere Fahhradtour Planer",
            walk_separator : "Auf andere Gehrichtungen Planer",
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.com"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
            working      : "Addresse suchen ....",
            error        : "keine Ergebnisse gefunden",
            msg_title    : "Reiseplanung",
            msg_content  : "Fehler korrigieren, bevor die Suche",
            select_result_title : "bitte, wählen Sie ein Ergebnis",
            address_header : "Addresse"
        },

        error:
        {
            title        : 'Fehler bei der Planung Ihrer Reise',
            deadMsg      : "Map Trip Planer reagirt nicht. Bitte, warten Sie einige Minute oder verwenden Sie die textuelle Funktion (sehen Sie den Link unten).",
            geoFromMsg   : "Wählen Sie die Abfahrt: ",
            geoToMsg     : "Wählen Sie die Ankunft: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "richtige Planung",
            500: "Server-Fehler",
            400: "Route außerhalb der servierten Zone",
            404: "keine Route gefunden",
            406: "keine Laufzeiten",
            408: "Anforderung abgelaufen",
            413: "ungültiger Parameter",
            440: "Abfahrt Geocode nicht gefunden",
            450: "Ankunft Geocode nicht gefunden",
            460: "Abfahrt und Ankunft Geocode nicht gefunden",
            409: "zu nahe",
            340: "Abfahrt Geocode mehrdeutig",
            350: "Ankunft Geocode mehrdeutig",
            360: "Abfahrt und Ankunft geocode mehrdeutig"
        },

        options: 
        [
          ['TRANSFERS', 'wenigste Transportmittel Änderungen'],
          ['QUICK',     'schnellste Route'],
          ['SAFE',      'sicherste Route'],
          ['TRIANGLE',  'Custom trip...'] // TODO
        ],
    
        arriveDepart: 
        [
          ['false', 'Abfahrt'], 
          ['true',  'Ankunft']
        ],
    
        maxWalkDistance : 
        [
            ['500',    '500 meter'],
            ['1000',   '1 km'],
            ['5000',   '5 km'],
            ['10000',  '10 km'],
            ['20000',  '20 km']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Transit'], // TODO
            ['BUSISH,TRAINISH,WALK', 'Bus und Züge'],
            ['BUSISH,WALK', 'Bus nur'],
            ['TRAINISH,WALK', 'Züge nur'],
            ['WALK', 'nur zu Fuß'],
            ['BICYCLE', 'Bicycle'], // TODO
            ['TRANSIT,BICYCLE', 'Transit & Bicycle'] // TODO
        ],

        wheelchair :
        [
            ['false', 'nicht erforderlich'],
            ['true', 'erforderlich']
        ]
    },

    CLASS_NAME : "otp.locale.German"
};
