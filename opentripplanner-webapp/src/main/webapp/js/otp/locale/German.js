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
        rightClickMsg : "Klicken Sie mit der rechte Maustaste auf die Karte um Startpunkt und Endpunkt auszuwählen",
        attribution   : {
            title   : "Lizenz",
            content : "Nutzungsbedingungen"
        }
    },

    contextMenu : 
    {
        fromHere         : "Startpunkt",
        toHere           : "Zielpunkt",
        intermediateHere : "Zwischenstop",

        centerHere       : "hier die Karte zentrieren",
        zoomInHere       : "Karte vergössern",
        zoomOutHere      : "Karte verkleinern",
        previous         : "vorherigen Position auf der Karte",
        next             : "nächste Position auf der Karte"
    },

    bikeTriangle : 
    {
        safeName : "für Fahrradnutzung",
        safeSym  : "B",

        hillName : "Flach",
        hillSym  : "F",

        timeName : "Schnell",
        timeSym  : "S"
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
        qEmptyText : "Addresse, Kreuzung, Bezugspunkt oder Haltstellen ID..."
    },

    buttons: 
    {
        reverse       : "Umkehren",
        reverseTip    : "<b>umgekehrte Richtung</b><br/>Vertauscht Start- und Zielpunkt, und ändert den Startzeitpunkt",
        reverseMiniTip: "umgekehrter Richtung",

        edit          : "Ändern",
        editTip       : "<b>Reise ändern</b><br/>Zurück zur Startseite mit den Details der geplanten Reise.",

        clear         : "Löschen",
        clearTip      : "<b>löschen</b><br/>Karte und alle Aktivität löschen.",

        fullScreen    : "Vollansicht",
        fullScreenTip : "<b>Vollansicht</b><br/>Symbolleiste zeigen oder ausblenden",

        print         : "Drucken",
        printTip      : "<b>Drucken</b><br/>Drucken Sie eine vereinfachte Version der Strecke (ohne Karte).",

        link          : "Links",
        linkTip      : "<b>Link</b><br/>Den Link für die gewählte Strecke zeigen.",

        feedback      : "Rückmeldung",
        feedbackTip   : "<b>Rückmeldung</b><br/>Rückmeldung geben",

        submit       : "bestätigen",
        clearButton  : "löschen",
        ok           : "OK",
        cancel       : "abbrechen",
        yes          : "ja",
        no           : "nein",
        showDetails  : "Zeige Details...",
        hideDetails  : "Verstecke Details..."
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
        left:           "Nach Links",
        right:          "Nach Rechts",
        slightly_left:  "links halten,",
        slightly_right: "rechts halten,",
        hard_left:      "Links Abbiegen",
        hard_right:     "Rechts Abbiegen",
        'continue':     "weiter",
        to_continue:    "weiter über",
        becomes:        "wird",
        at:             "um",
        on:             "entlang der",
        to:             "nach",
        via:            "Straße",
        circle_counterclockwise: "nehmen Sie den Kreisverkehr gegen den Uhrzeigersinn",
        circle_clockwise:        "nehmen Sie den Kreisverkehr im Uhrzeigersinn",
        // rather than just being a direction, this should be
        // full-fledged to take just the exit name at the end
        elevator: "Aufzug benutzen, nach"  
    },

    // see otp.planner.Templates for use
    instructions :
    {
        walk         : "zu Fuß",
        walk_toward  : "Gehe nach",
        walk_verb    : "gehen",
        bike         : "Fahrrad",
        bike_toward  : "Radfahren",
        bike_verb    : "Radfahren",
        drive        : "Fahren",
        drive_toward : "fahren in Richtung",
        drive_verb   : "fahren",
        move         : "weiterfahren",
        move_toward  : "weiterfahren",

        transfer     : "Umstiegshalt",
        transfers    : "Umstiegshalte",

        continue_as  : "Weiterfahren",
        stay_aboard  : "an Bord bleiben",

        depart       : "Abfahrt",
        arrive       : "Ankunft",

        start_at     : "Start",
        end_at       : "Ziel"
    },

    // see otp.planner.Templates for use
    labels : 
    {
        agency_msg   : "Ein Service von",
        agency_msg_tt: "Internetseite des Service-Anbieters in neuem Fenster...",
        about        : "Strecke:",
        stop_id      : "Haltstellen-Nummer",
        trip_details : "Reise Übersicht",
        fare         : "Preis",
        fare_symbol  : "\u20ac",

        // TODO  -- used in the Trip Details summary to describe different fares 
        regular_fare : "",
        student_fare : "",
        senior_fare  : "",

        travel       : "Reise",
        valid        : "Erstellt am",
        trip_length  : "Reisezeit",
        with_a_walk  : "Zu Fuß",
        alert_for_rt : "Achten Sie auf den Weg!"
    },

    modes : 
    {
        WALK:           "zu Fuß",
        BICYCLE:        "Fahrrad",
        CAR:            "Auto",
        TRAM:           "Straßenbahn",
        SUBWAY:         "U-Bahn",
        RAIL:           "Eisenbahn",
        BUS:            "Bus",
        FERRY:          "Fähre",
        CABLE_CAR:      "Standseilbahn",
        GONDOLA:        "Seilbahn",
        FUNICULAR:      "Standseilbahn"
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
        hour_abbrev    : "Std.",
        hours_abbrev   : "Std.",
        hour           : "Stunde",
        hours          : "Stunden",

        minute         : "Minute",
        minutes        : "Minuten",
        minute_abbrev  : "min.",
        minutes_abbrev : "min.",
        second_abbrev  : "s.",
        seconds_abbrev : "s.",
        format         : "d.m.Y \\u\\m H:i",
        date_format    : "d.m.Y",
        time_format    : "H:i",
        months         : ['Jan.', 'Feb.', 'Mär.', 'Apr.', 'Mai', 'Juni', 'Juli', 'Aug.', 'Sep.', 'Okt.', 'Nov.', 'Dez.']
    },

    systemmap :
    {
        labels :
        {
            panelTitle : "Strecke(n)"
        }
    },

    tripPlanner :
    {
        // see otp/planner/*.js for where these values are used
        labels : 
        {
            panelTitle    : "Strecke berechnen",
            tabTitle      : "Neue Strecke",
            inputTitle    : "Strecken Details",
            optTitle      : "Strecken Vorzug (optional)",
            submitMsg     : "Streckenberechnung...",
            optionalTitle : "",
            date          : "Datum",
            time          : "Zeit",
            when          : "Wann",
            from          : "von",
            fromHere      : "von hier",
            to            : "nach",
            toHere        : "nach hier",
            intermediate  : "Zwischenstop",
            minimize      : "Anzeigen von",
            maxWalkDistance: "Maximale Gehdistanz",
            walkSpeed     : "Geh-Geschwindigkeit",
            maxBikeDistance: "Maximale Distanz für Fahrradnutzung ",
            bikeSpeed     : "Fahrrad-Geschwindigkeit",
            arriveDepart  : "Ankunft innerhalb von/Abfahrt um",
            mode          : "Benutzen von",
            wheelchair    : "Rollstuhlfahrer geeignet", 
            go            : "gehe nach",
            planTrip      : "Strecken berechnen",
            newTrip       : "Neue Strecke"
        },

        // see otp/config.js for where these values are used
        link : 
        {
            text           : "Link dieser Reise",
            trip_separator : "Diese Reise in einem anderen Streckenplaner",
            bike_separator : "Anderer Fahhrad-Strecken-Planer",
            walk_separator : "Anderer Geh-Strecken-Planer",
            google_transit : "Google Transit",
            google_bikes   : "Google Bike Directions",
            google_walk    : "Google Walking Directions",
            google_domain  : "http://www.google.com"
        },

        // see otp.planner.Forms for use
        geocoder:
        {
            working      : "Adresse suchen ....",
            error        : "keine Ergebnisse gefunden",
            msg_title    : "Streckenplanung",
            msg_content  : "Fehler vor Suche korrigieren",
            select_result_title : "Bitte wählen Sie eine Reiseplan",
            address_header : "Adresse"
        },

        error:
        {
            title        : 'Fehler bei der Planung Ihrer Reise',
            deadMsg      : "Map Trip Planer reagirt nicht. Bitte, warten Sie einige Minute oder verwenden Sie die Text-Funktion (sehen Sie den Link unten).",
            geoFromMsg   : "Wählen Sie die Abfahrt: ",
            geoToMsg     : "Wählen Sie die Ankunft: "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "richtige Planung",
            500: "Server-Fehler",
            400: "Strecke außerhalb der Daten-Zone",
            404: "keine Strecke gefunden",
            406: "keine Laufzeiten",
            408: "Anforderung abgelaufen",
            413: "ungültiger Parameter",
            440: "Geocode der Abfahrt nicht gefunden",
            450: "Geocode der Ankuft nicht gefunden",
            460: "Abfahrt und Ankunft Geocode nicht gefunden",
            409: "zu nahe",
            340: "Abfahrt Geocode mehrdeutig",
            350: "Ankunft Geocode mehrdeutig",
            360: "Abfahrt und Ankunft geocode mehrdeutig"
        },

        options: 
        [
          ['TRANSFERS', 'wenigste Umstiegshalte'],
          ['QUICK',     'schnellste Strecke'],
          ['SAFE',      'sicherste Strecke'],
          ['TRIANGLE',  'Anpassen...']
        ],
    
        arriveDepart: 
        [
          ['false', 'Abfahrt'], 
          ['true',  'Ankunft']
        ],
    
        maxWalkDistance : 
        [
            ['500',    '500 Meter'],
            ['1000',   '1 km'],
            ['5000',   '5 km'],
            ['10000',  '10 km'],
            ['20000',  '20 km']
        ],

        walkSpeed :
        [
            ['0.278',  '1 km/h'],
            ['0.556',  '2 km/h'],
            ['0.833',  '3 km/h'],
            ['1.111',  '4 km/h'],
            ['1.389',  '5 km/h'],
            ['1.667',  '6 km/h'],
            ['1.944',  '7 km/h'],
            ['2.222',  '8 km/h'],
            ['2.500',  '9 km/h'],
            ['2.778',  '10 km/h']
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'ÖPNV'],
            ['BUSISH,TRAINISH,WALK', 'Bus und Zug'],
            ['BUSISH,WALK', 'nur Bus'],
            ['TRAINISH,WALK', 'nur Zug'],
            ['WALK', 'nur zu Fuß'],
            ['BICYCLE', 'Fahrrad'],
            ['TRANSIT,BICYCLE', 'ÖPNV & Fahrrad']
        ],

        wheelchair :
        [
            ['false', 'nicht erforderlich'],
            ['true', 'erforderlich']
        ]
    },

    CLASS_NAME : "otp.locale.German"
};
