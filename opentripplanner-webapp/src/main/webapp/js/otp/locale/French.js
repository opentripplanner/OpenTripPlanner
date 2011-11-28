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
otp.locale.French = {

    config : {
        metricsSystem : "international",
        rightClickMsg : "Cliquez avec le bouton droit de la souris sur la carte pour désigner le départ et l'arrivée de votre parcours.",
        attribution : {
            title : "License Attribution",  // TODO localize
            content : "Disclaimer goes here"
        }
    },

    contextMenu : {
        fromHere : "Partir d'ici",
        toHere : "Arriver ici",
        intermediateHere : "Add intermediate point",  // TODO localize

        centerHere : "Centrer la carte ici",
        zoomInHere : "Zoomer ici",
        zoomOutHere : "Dézoomer ici",
        previous : "position précédente",
        next : "position suivante"
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

    service : {
        weekdays : "jours de semaine",
        saturday : "Samedi",
        sunday : "Dimanche",
        schedule : "Horaires"
    },

    indicators : {
        ok : "OK",
        date : "Date",
        loading : "Chargement",
        searching : "Calcul...",
        qEmptyText : "Adresse, intersection, point de repère ou nom d'arrêt..."
    },

    buttons : {
        reverse : "Inverser",
        reverseTip : "<b>Inverser l'itinéraire</b><br/>Planifier un itinéraire retour en inversant les points de départs et d'arrivée, et ajuster l'heure de départ.",
        reverseMiniTip : "Inverser l'itinéraire",

        edit : "Modifier",
        editTip : "<b>Modifier l'itinéraire</b><br/>Retourner à la configuration de l'itinéraire pour en modifier des paramètres.",

        clear : "Effacer",
        clearTip : "<b>Effacer</b><br/>Effacer la carte et les outils actifs.",

        fullScreen : "Plein écran",
        fullScreenTip : "<b>Plein écran</b><br/>Montrer ou cacher le panneau latéral",

        print : "Imprimer",
        printTip : "<b>Imprimer</b><br/>Imprimer l'itinéraire (sans carte).",

        link : "Lien",
        linkTip : "<b>Lien</b><br/>Voir le lien direct vers cet itinéraire.",

        feedback : "Commentaires",
        feedbackTip : "<b>Commentaires</b><br/>Envoyez votre retour d'expérience avec la carte.",

        submit : "Valider",
        clearButton : "Effacer",
        ok : "OK",
        cancel : "Annuler",
        yes : "Oui",
        no : "Non"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : {
        southeast : "le sud-est",
        southwest : "le sud-ouest",
        northeast : "le nord-est",
        northwest : "le nord-ouest",
        north : "le nord",
        west : "l'ouest",
        south : "le sud",
        east : "l'est",
        bound : "en direction",
        left : "à gauche",
        right : "à droite",
        slightly_left : "légèrement à gauche",
        slightly_right : "légèrement à droite",
        hard_left : "complètement à gauche",
        hard_right : "complètement à droite",
        'continue' : "continuer sur",
        to_continue : "pour continuer sur",
        becomes : "devenant",
        at : "à",
        on : "sur",
        to : "vers",
        via : "via",
        circle_counterclockwise : "prendre le rond-point",
        circle_clockwise : "prendre le rond-point dans le sens horaire"
    },

    // see otp.planner.Templates for use ... these are used on the trip
    // itinerary as well as forms and other places
    instructions : {
        walk : "Marche à pied",
        walk_toward : "Marcher vers",
        walk_verb : "Marcher",
        bike : "Trajet à vélo",
        bike_toward : "Pédaler vers",
        bike_verb : "Pédaler",
        drive : "Conduite",
        drive_toward : "Rouler vers",
        drive_verb : "Rouler",
        move : "Continuer",
        move_toward : "Continuer vers",

        transfer : "correspondance",
        transfers : "correspondances",

        continue_as : "Continuer comme",
        stay_aboard : "rester à bord",

        depart : "Départ",
        arrive : "Arrivée",

        start_at : "Départ à",
        end_at : "Arrivée à"
    },

    // see otp.planner.Templates for use
    labels : {
        about : "Environ",
        stop_id : "Id arrêt",
        trip_details : "Détails de l'itinéraire",
        fare : "Tarif",
        fare_symbol : "\u20ac",
        travel : "Départ le",
        valid : "Calculé le",
        trip_length : "Durée",
        with_a_walk : "avec une marche de",
        alert_for_rt : "Alerte sur la ligne"
    },

    // see otp.planner.Templates for use -- one output are the itinerary leg
    // headers
    modes : {
        WALK : "MARCHE À PIED",
        BICYCLE : "VÉLO",
        CAR : "VOITURE",
        TRAM : "TRAMWAY",
        SUBWAY : "MÉTRO",
        RAIL : "TRAIN",
        BUS : "BUS",
        FERRY : "FERRY",
        CABLE_CAR : "TRAMWAY FUNICULAIRE",
        GONDOLA : "TÉLÉPHÉRIQUE",
        FUNICULAR : "FUNICULAIRE"
    },

    ordinal_exit : {
        1 : "(première sortie)",
        2 : "(deuxième sortie)",
        3 : "(troisième sortie)",
        4 : "(quatrième sortie)",
        5 : "(cinquième sortie)",
        6 : "(sixième sortie)",
        7 : "(septième sortie)",
        8 : "(huitième sortie)",
        9 : "(neuvième sortie)",
        10 : "(dixième sortie)"
    },

    time : {
        format : "d.m.Y \\à H:i",
        minute : "minute",
        minutes : "minutes",
        minute_abbrev : "min",
        minutes_abbrev : "min",
        second_abbrev : "sec",
        seconds_abbrev : "sec",
        months : [ 'Jan', 'Fév', 'Mar', 'Avr', 'Mai', 'Juin', 'Juil', 'Aoû', 'Sep', 'Oct', 'Nov', 'Déc' ]
    },

    systemmap : {
        labels : {
            panelTitle : "System Map"
        }
    },

    tripPlanner : {
        labels : {
            panelTitle : "Itinéraire",
            tabTitle : "Planifier un itinéraire",
            inputTitle : "Détails de l'itinéraire",
            optTitle : "Préférences de l'itinéraire (facultatif)",
            submitMsg : "Calcul de l'itinéraire...",
            optionalTitle : "",
            date : "Date",
            time : "Heure",
            when : "Quand",
            from : "Départ",
            fromHere : "Partir d'ici",
            to : "Arrivée",
            toHere : "Arriver ici",
            minimize : "Optimiser pour",
            maxWalkDistance : "Marche maximum",
            maxBikeDistance: "Parcours à vélo maximum",
            arriveDepart : "Arriver à/Partir de",
            mode : "Voyager par",
            wheelchair : "Accessible aux fauteuils roulants",
            go : "Go",
            planTrip : "Calculer un itinéraire",
            newTrip : "Nouvel itinéraire"
        },

        // see otp/config.js for where these values are used
        link : {
            text : "Lien vers cet itinéraire (OTP)",
            trip_separator : "Cet itinéraire sur d'autres calculateurs",
            bike_separator : "Sur d'autres calculateurs 'vélo'",
            walk_separator : "Sur d'autres calculateurs 'piéton'",
            google_transit : "Google Maps (transport en commun)",
            google_bikes : "Google Maps (vélo)",
            google_walk : "Google Maps (piéton)",
            google_domain : "http://www.google.fr"
        },

        // see otp.planner.Forms for use
        geocoder : {
            working : "Recherche de l'adresse...",
            error : "Aucun résultat n'a été trouvé",
            msg_title : "Recommencer avec une autre adresse",
            msg_content : "Veuillez corriger les erreurs avant de planifier votre itinéraire",
            select_result_title : "Veuillez sélectionner un résultat",
            address_header : "Adresse"
        },

        error : {
            title : "Erreur du planificateur d'itinéraire",
            deadMsg : "Le planificateur d'itinéraire ne répond pas actuellement. Merci de patienter quelques minutes et de réessayer, ou essayez le planificateur en mode texte (voir le lien ci-dessous).",
            geoFromMsg : "Merci de sélectionner le lieu de départ de votre itinéraire : ",
            geoToMsg : "Merci de sélectionner le lieu d'arrivée de votre itinéraire : "
        },

        // default messages from server if a message was not returned
        msgcodes : {
            200 : "Calcul OK",
            500 : "Erreur serveur",
            400 : "Voyage en dehors de la zone",
            404 : "Itinéraire non trouvé",
            406 : "Pas d'horaires de transport en commun",
            408 : "Temps d'attente de la demande dépassé",
            413 : "Paramètre invalide",
            440 : "Adresse de départ non trouvée",
            450 : "Adresse de destination non trouvée",
            460 : "Adresses de départ et destination non trouvées",
            409 : "Départ trop proche de l'arrivée",
            340 : "Adresse de départ ambigüe",
            350 : "Adresse de destination ambigüe",
            360 : "Adresses de départ et destination ambigües"
        },

        options : [ 
                [ 'TRANSFERS', 'Le plus direct' ],
                [ 'QUICK', 'Le plus rapide' ], 
                [ 'SAFE', 'Le plus sûr' ],
                ['TRIANGLE',  'Custom trip...']  // TODO: localize 
        ],

        arriveDepart : [ [ 'false', 'Départ' ], [ 'true', 'Arriver' ] ],

        maxWalkDistance : [ [ '200', '200 m' ], [ '500', '500 m' ],
                [ '1000', '1 km' ], [ '1500', '1,5 km' ], [ '5000', '5 km' ],
                [ '10000', '10 km' ] ],

        mode : [ [ 'TRANSIT,WALK', 'Transports publics' ],
                [ 'BUSISH,TRAINISH,WALK', 'Bus & Train' ],
                [ 'BUSISH,WALK', 'Bus seulement' ],
                [ 'TRAINISH,WALK', 'Train seulement' ],
                [ 'WALK', 'Marche seulement' ], [ 'BICYCLE', 'Vélo' ],
                [ 'TRANSIT,BICYCLE', 'Transports publics & Vélo' ] ],

        wheelchair : [ [ 'false', 'Non requis' ], [ 'true', 'Requis' ] ]
    },

    CLASS_NAME : "otp.locale.French"
};
