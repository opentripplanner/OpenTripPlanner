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

    config: {
        metricsSystem: "international",
        rightClickMsg: "Klicken Sie mit der rechte Maustaste auf die Karte um Startpunkt und Endpunkt auszuwählen.",
        attribution: {
            title: "Lizenz",
            content: "Nutzungsbedingungen einfügen"
        }
    },

    contextMenu: {
        fromHere: "Ausgangspunkt",
        toHere: "Zielort",
        intermediateHere: "Zwischenziel einfügen",

        centerHere: "Karte hier zentrieren",
        zoomInHere: "hineinzoomen",
        zoomOutHere: "herauszoomen",
        previous: "vorherige Position auf der Karte",
        next: "nächste Position auf der Karte",

        analysisLocation: "Ausgangspunkt für Analyse setzen",

        minimize: "Minimieren",
        bringToFront: "in den Vordergrund",
        sendToBack: "in den Hintergrund"
    },

    widgets: {
        managerMenu: {
            minimizeAll: "Alle Dialoge minimieren",
            unminimizeAll: "Dialoge wiederanzeigen"
        },

        ItinerariesWidget: {
            title: "Routenvorschläge",
            itinerariesLength: "gefundene Routenvorschläge",
            linkToSearch: "Link für diese Suche erstellen",
            buttons: {
                first: "Erster",
                previous: "Vorheriger",
                next: "Nächster",
                last: "Letzter"
            },
            realtimeDelay: {
                late: "min Verspätung",
                early: "min zu früh",
                onTime: "pünktlich"
            }
        },

        AnalystLegend: {
            title: "Legende: Reisezeit in Minuten"
        },

        MultimodalPlannerModule: {
            title: "Einstellungen für Routensuche"
        },

        TripOptionsWidget: {
            title: "Routeneinstellungen"
        }
    },

    modules: {
        analyst: {
            AnalystModule: {
                name: "Analyse",
                refresh: "Aktualisieren"
            }
        },

        multimodal: {
            MultimodalPlannerModule: {
                name: "Multimodaler Routenplaner"
            }
        }
    },

    bikeTriangle: {
        safeName: "Fahrradgeeignet",
        safeSym: "F",

        hillName: "Flach",
        hillSym: "Fl",

        timeName: "Schnellste",
        timeSym: "S"
    },

    service: {
        weekdays: "wochentags",
        saturday: "Samstag",
        sunday: "Sonntag",
        schedule: "Fahrplan"
    },

    indicators: {
        ok: "OK",
        date: "Datum",
        loading: "Laden",
        searching: "Suche läuft...",
        qEmptyText: "Adresse, Kreuzung, Haltestelle oder Sehenswürdigkeit..."
    },

    buttons: {
        reverse: "umkehren",
        reverseTip: "<b>Umgekehrte Richtung</b><br/>Startpunkt und Zielort vertauschen und Route mit aktualisierter Abfahrtszeit berechnen.",
        reverseMiniTip: "Umgekehrte Richtung",

        edit: "anpassen",
        editTip: "<b>Route anpassen</b><br/>Zurück zu den Routeneinstellungen.",

        clear: "löschen",
        clearTip: "<b>Zurücksetzen</b><br/>Aktuelle Suche löschen und neue Suche starten.",

        fullScreen: "Vollbild",
        fullScreenTip: "<b>Vollbild</b><br/>Werkzeugleiste ein- oder ausblenden",

        print: "drucken",
        printTip: "<b>Drucken</b><br/>Vereinfachte Version der angezeigten Route druchen (ohne Kartenansicht).",

        link: "Link",
        linkTip: "<b>Link anzeigen</b><br/>Link für ausgewählte Route anzeigen.",

        feedback: "Feedback",
        feedbackTip: "<b>Feedback hinterlassen</b><br/>Hinterlassen Sie Feedback zur Karte und Routenplanung.",

        submit: "Bestätigen",
        clearButton: "Zurücksetzen",
        ok: "OK",
        cancel: "Abbrechen",
        yes: "ja",
        no: "nein",
        showDetails: "Details anzeigen...",
        hideDetails: "Details verbergen..."
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions: {
        southeast: "südost",
        southwest: "südwest",
        northeast: "nordost",
        northwest: "nordwest",
        north: "nord",
        west: "west",
        south: "süd",
        east: "ost",
        bound: "Richtung",
        left: "links",
        right: "rechts",
        slightly_left: "links halten",
        slightly_right: "rechts halten",
        hard_left: "scharf links",
        hard_right: "scharf rechts",
        'continue': "weiter auf",
        to_continue: "weiter auf",
        becomes: "wird",
        at: "um",
        on: "auf",
        to: "bis",
        via: "via",
        circle_counterclockwise: "nehmen Sie den Kreisverkehr",
        circle_clockwise: "nehmen Sie den Kreisverkehr im Uhrzeigersinn",
        // rather than just being a direction, this should be
        // full-fledged to take just the exit name at the end
        elevator: "nehmen Sie den Aufzug bis"
    },

    // see otp.planner.Templates for use ... these are used on the trip
    // itinerary as well as forms and other places
    instructions: {
        walk: "zu Fuß",
        walk_toward: "gehen",
        walk_verb: "gehen",
        bike: "Fahrrad",
        bike_toward: "fahren Sie",
        bike_verb: "radfahren",
        drive: "Fahren",
        drive_toward: "fahren Sie",
        drive_verb: "fahren",
        move: "weiter",
        move_toward: "weiter",

        transfer: "umsteigen",
        transfers: "Umsteigepunkt",

        continue_as: "Weiterfahren",
        stay_aboard: "an Bord bleiben",

        depart: "Abfahrt",
        arrive: "Ankunft",
        now: "Jetzt",

        presets_label: "Voreinstellungen",

        preferredRoutes_label: "bevorzugte Routen",
        edit: "ändern",
        none: "keine",
        weight: "Gewichtung",

        allRoutes: "alle Routen",

        save: "Speichern",
        close: "Schließen",

        start_at: "Abfahrt um",
        end_at: "Ankunft um",

        start: "Abfahrt",
        end: "Ankunft",

        geocoder: "Geocoder"
    },

    // see otp.planner.Templates for use
    labels: {
        agency_msg: "Betrieb durch",
        agency_msg_tt: "Webseite des Betreibers in neuem Fenster öffnen...",
        about: "Über",
        stop_id: "Haltestelle",
        trip_details: "Routendetails",
        fare: "Tarif",
        fare_symbol: "\u20ac",

        regular_fare: "Normaltarif",
        student_fare: "Ausbildungstarif",
        senior_fare: "Seniorentarif",

        travel: "Reise",
        valid: "Gültig bis",
        trip_length: "Dauer",
        with_a_walk: "Fußweg",
        alert_for_rt: "Achten Sie auf Linie"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg
    // headers
    modes: {
        WALK: "zu Fuß gehen",
        BICYCLE: "Fahrrad",
        CAR: "Auto",
        TRAM: "Straßenbahn",
        SUBWAY: "U-Bahn",
        RAIL: "Bahn",
        BUS: "Bus",
        FERRY: "Fähre",
        CABLE_CAR: "Standseilbahn",
        GONDOLA: "Gondel",
        FUNICULAR: "Seilbahn"
    },

    ordinal_exit: {
        1: "bis zur ersten Ausfahrt",
        2: "bis zur zweiten Ausfahrt",
        3: "bis zur dritten Ausfahrt",
        4: "bis zur vierten Ausfahrt",
        5: "bis zur fünften Ausfahrt",
        6: "bis zur sechsten Ausfahrt",
        7: "bis zur siebten Ausfahrt",
        8: "bis zur achten Ausfahrt",
        9: "bis zur neunten Ausfahrt",
        10: "bis zur zehnten Ausfahrt"
    },

    time: {
        hour_abbrev: "Std.",
        hours_abbrev: "Std.",
        hour: "Stunde",
        hours: "Stunden",

        format: "d.m.Y \\H:i",
        date_format: "d.m.Y",
        time_format: "H:i",
        minute: "Minute",
        minutes: "Minuten",
        minute_abbrev: "Min.",
        minutes_abbrev: "Min.",
        second_abbrev: "Sek.",
        seconds_abbrev: "Sek.",
        months: [ 'Jan', 'Feb', 'Mär', 'Apr', 'Mai', 'Jun', 'Jul', 'Aug', 'Sep', 'Okt', 'Nov', 'Dez' ]
    },

    systemmap: {
        labels: {
            panelTitle: "Verkehrslinienplan"
        }
    },

    tripPlanner: {
        labels: {
            panelTitle: "Routenplaner",
            tabTitle: "Route berechnen",
            inputTitle: "Routendetails",
            optTitle: "Routeneinstellungen (optional)",
            submitMsg: "Route wird berechnet...",
            optionalTitle: "",
            date: "Datum",
            time: "Zeit",
            when: "Wann",
            from: "Startpunkt",
            fromHere: "von hier (Start)",
            to: "Ankunft",
            toHere: "nach hier (Ziel)",
            intermediate: "Zwischenziel",
            minimize: "Anzeigen",
            maxWalkDistance: "maximale Gehstrecke",
            walkSpeed: "Gehgeschwindigkeit",
            maxBikeDistance: "maximale Fahrradstrecke",
            bikeSpeed: "Fahrradgeschwindigkeit",
            arriveDepart: "Abfahrt um/Ankunft um",
            mode: "Fortbewegungsart/Verkehrsmittel",
            wheelchair: "barrierefreie Route",
            go: "Los",
            planTrip: "Route berechnen",
            newTrip: "Neue Route planen",
            bannedRoutes: "ausgeschlossene Routen"
        },

        // see otp/config.js for where these values are used
        link: {
            text: "Link zu dieser Route (OTP)",
            trip_separator: "Diese Route mit anderen Routenplaner berechnen",
            bike_separator: "mit einem anderen Fahrradroutenplaner",
            walk_separator: "mit einem anderen Fußwegplaner",
            google_transit: "Google Maps (ÖPNV)",
            google_bikes: "Google Maps (Fahrrad)",
            google_walk: "Google Maps (Fußgänger)",
            google_domain: "http://www.google.de"
        },

        // see otp.planner.Forms for use
        geocoder: {
            working: "Adresse wird gesucht...",
            error: "Keine passende Adresse gefunden",
            msg_title: "Adresse ändern",
            msg_content: "Bitte ändern Sie die angegebene Adresse, um mit der Routenplanung fortsetzen zu können",
            select_result_title: "Bitte wählen Sie ein Ergebnis aus",
            address_header: "Adresse"
        },

        error: {
            title: "Fehler bei der Routenberechnung",
            deadMsg: "Die Routenberechnung funktioniert momentan nicht. Bitte versuchen Sie es in wenigen Minuten noch einmal oder nutzen Sie einen anderen Routenplaner (z. B. einen der folgenden Liste)",
            geoFromMsg: "Bitte wählen Sie einen Startpunkt: ",
            geoToMsg: "Bitte wählen Sie einen Zielort: "
        },

        // default messages from server if a message was not returned ... 'Place' error messages also used when trying to submit without From & To coords.
        msgcodes: {
            200: "Berechnung i. O.",
            500: "Serverfehler",
            400: "Route geht über planbaren Bereich hinaus",
            404: "Keine Route gefunden",
            406: "Keine ÖPNV-Fahrzeiten vorhanden",
            408: "Anfragezeit überschritten",
            413: "Ungültiger Parameter",
            440: "Startadresse wurde nicht gefunden. Bitte versuchen Sie es mit geänderter Startadresse noch einmal",
            450: "Zieladresse wurde nicht gefunden. Bitte versuchen Sie es mit geänderter Zieladresse noch einmal",
            460: "Start- und Zieladresse wurden nicht gefunden",
            470: "Start- oder Zieladresse sind nicht barrierefrei erreichbar",
            409: "Start und Ziel liege zu nahe beieinander",
            340: "Startadresse kann nicht eindeutig ermittelt werden. Bitte geben Sie mehr Details ein (z. B. Postleitzahl)",
            350: "Zieladresse kann nicht eindeutig ermittelt werden. Bitte geben Sie mehr Details ein (z. B. Postleitzahl)",
            360: "Start- und Zieladresse können nicht eindeutig ermittelt werden. Bitte geben Sie mehr Details ein (z. B. Postleitzahl)"
        },

        options: [
            [ 'TRANSFERS', 'Wenigste Umstiege' ],
            [ 'QUICK', 'schnellste Route' ],
            [ 'SAFE', 'sicherste Route' ],
            [ 'TRIANGLE', 'Personalisieren...']
        ],

        arriveDepart: [
            ['false', 'Abfahrt'],
            ['true', 'Ankunft']
        ],

        maxWalkDistance: [
            [ '200', '200 m' ],
            [ '500', '500 m' ],
            [ '1000', '1 km' ],
            [ '1500', '1,5 km' ],
            [ '5000', '5 km' ],
            [ '10000', '10 km' ]
        ],

        walkSpeed: [
            ['0.278', '1 km/h'],
            ['0.556', '2 km/h'],
            ['0.833', '3 km/h'],
            ['1.111', '4 km/h'],
            ['1.389', '5 km/h'],
            ['1.667', '6 km/h'],
            ['1.944', '7 km/h'],
            ['2.222', '8 km/h'],
            ['2.500', '9 km/h'],
            ['2.778', '10 km/h']
        ],

        modes: // leaflet client
        {
            "TRANSIT,WALK"              : "ÖPNV",
            "BUSISH,WALK"               : "nur Bus",
            "TRAINISH,WALK"             : "nur Bahn",
            "BICYCLE"                   : 'Fahrrad',
            "WALK"                      : 'zu Fuß',
            "TRANSIT,BICYCLE"           : "ÖPNV und Fahrrad",
            "CAR"                       : 'Auto',
            "CAR_PARK,WALK,TRANSIT"     : 'Parken und Reisen',
            "CAR,WALK,TRANSIT"          : 'Kiss and Ride',
            "BICYCLE_PARK,WALK,TRANSIT" : 'Bike and Ride'
        },

        mode: // OL client
        [
            [ 'TRANSIT,WALK', 'ÖPNV' ],
            [ 'BUSISH,TRAINISH,WALK', 'nur Bus und Bahn' ],
            [ 'BUSISH,WALK', 'nur Bus' ],
            [ 'TRAINISH,WALK', 'nur Bahn' ],
            [ 'WALK', 'zu Fuß' ],
            [ 'BICYCLE', 'Fahrrad' ],
            [ 'TRANSIT,BICYCLE', 'ÖPNV und Fahrrad' ]
        ],

        // TODO: remove this hack, and provide code that allows the mode array to be configured with different transit modes.
        //       (note that we've been broken for awhile here, since many agencies don't have a 'Train' mode either...this needs attention)
        // IDEA: maybe we start with a big array (like below), and the pull out modes from this array when turning off various modes...
        with_bikeshare_mode :
        [
            ['TRANSIT,WALK', 'ÖPNV'],
            ['BUSISH,WALK', 'nur Bus'],
            ['TRAINISH,WALK', 'nur Bahn'],
            ['WALK', 'zu Fuß'],
            ['BICYCLE', 'Fahrrad'],
            ['WALK,BICYCLE', 'Fahrradverleih'],
            ['TRANSIT,BICYCLE', 'ÖPNV und Fahrrad'],
            ['TRANSIT,WALK,BICYCLE', 'ÖPNV und Fahrradverleih']
        ],


        wheelchair: [
            [ 'false', 'nicht benötigt' ],
            [ 'true', 'benötigt' ]
        ]
    },

    CLASS_NAME: "otp.locale.German"
};
