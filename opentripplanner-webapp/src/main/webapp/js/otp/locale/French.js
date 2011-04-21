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

    config : 
    {
        metricsSystem : "international",
        rightClickMsg : "TODO - localize me and tripPlanner.link below - Right-click on the map to designate the start and end of your trip.",
        attribution   : {
            title   : "License Attribution",
            content : "Disclaimer goes here"
        }
    },

    contextMenu : 
    {
        fromHere         : "Partir d'ici",
        toHere           : "Arriver ici",

        centerHere       : "Centrer la carte ici",
        zoomInHere       : "Zoomer ici",
        zoomOutHere      : "Dézoomer ici",
        previous         : "position précédente",
        next             : "position suivante"
    },

    service : 
    {
        weekdays:  "jours de semaine",
        saturday:  "Samedi",
        sunday:    "Dimanche",
        schedule:  "Horaires"
    },

    indicators : 
    {
        ok         : "OK",
        date       : "Date",
        loading    : "Chargement",
        searching  : "Calcul...",
        qEmptyText : "Adresse, intersection, point de repère ou nom d'arrêt..."
    },

    buttons: 
    {
        reverse       : "Inverser",
        reverseTip    : "<b>Inverser l'itinéraire</b><br/>Planifier un itinéraire retour en inversant les points de départs et d'arriver, et ajuster l'heure de départ.",
        reverseMiniTip: "Inverser l'itinéraire",

        edit          : "Modifier",
        editTip       : "<b>Modifier l'itinéraire</b><br/>Retourner à la configuration de l'itinéraire pour en modifier des paramètres.",

        clear         : "Effaçer",
        clearTip      : "<b>Effaçer</b><br/>Effaçer la carte et les outils actifs.",

        fullScreen    : "Plein écran",
        fullScreenTip : "<b>Plein écran</b><br/>Montrer ou cacher le panneau latéral",

        print         : "Imprimer",
        printTip      : "<b>Imprimer</b><br/>Imprimer l'itinéraire (sans carte).",

        link          : "Lien",
        linkTip      : "<b>Lien</b><br/>Voir le lien direct vers cet itinéraire.",

        feedback      : "Feedback",
        feedbackTip   : "<b>Feedback</b><br/>Envoyez votre retour d'expérience avec la carte.",

        submit       : "Valider",
        clearButton  : "Effaçer",
        ok           : "OK",
        cancel       : "Annuler",
        yes          : "Oui",
        no           : "Non"
    },

    // note: keep these lower case (and uppercase via template / code if needed)
    directions : 
    {
        southEast:      "sud-est",
        southWest:      "sud-ouest",
        northEast:      "nord-est",
        northWest:      "nord-ouest",
        north:          "nord",
        west:           "ouest",
        south:          "sud",
        east:           "est",
        bound:          "en direction",
        left:           "gauche",
        right:          "droite",
        slightly_left:  "légèrement à gauche",
        slightly_right: "légèrement à droite",
        hard_left:      "complètement à gauche",
        hard_right:     "complètement à droite",
        'continue':     "continuer sur",
        to_continue:    "pour continuer sur",
        becomes:        "devient",
        at:             "à"
    },

    time:
    {
        minute_abbrev:  "min",
        minutes_abbrev: "min",
        second_abbrev: "sec",
        seconds_abbrev: "sec",
        months:         ['Jan', 'Fev', 'Mar', 'Avr', 'Mai', 'Jui', 'Juil', 'Aou', 'Sep', 'Oct', 'Nov', 'Dec']
    },
    
    systemmap :
    {
        labels :
        {
            panelTitle : "System Map"
        }
    },

    tripPlanner :
    {
        labels : 
        {
            panelTitle    : "Itinéraire",
            tabTitle      : "Planifier un itinéraire",
            inputTitle    : "Détails de l'itinéraire",
            optTitle      : "Préférences de l'itinéraire (facultatif)",
            submitMsg     : "Calcul de l'itinéraire...",
            optionalTitle : "",
            date          : "Date",
            time          : "Heure",
            when          : "Quand",
            from          : "Départ",
            fromHere      : "Partir d'ici",
            to            : "Arrivée",
            toHere        : "Arriver ici",
            minimize      : "Voir le",
            maxWalkDistance: "Marche maximum",
            arriveDepart  : "Arriver à/Partir de",
            mode          : "Voyager par",
            wheelchair    : "Accessible aux fauteuils roulants", 
            go            : "Go",
            planTrip      : "Calculer un itinéraire",
            newTrip       : "Nouvel itinéraire"
        },

        error:
        {
            title        : "Erreur du planificateur d'itinéraire",
            deadMsg      : "Le planificateur d'itinéraire de répond pas actuellement. Merci de patienter quelques minutes et de réessayer, ou essayez le planificateur en mode texte (voir le lien ci-dessous).",
            geoFromMsg   : "Merci de sélectionner le lieu de départ de votre itinéraire : ",
            geoToMsg     : "Merci de sélectionner le lieu d'arrivée de votre itinéraire : "
        },
        
        // default messages from server if a message was not returned
        msgcodes:
        {
            200: "Plan OK",
            500: "Server error",
            400: "Trip out of bounds",
            404: "Path not found",
            406: "No transit times",
            408: "Request timed out",
            413: "Invalid parameter",
            440: "From geocode not found",
            450: "To geocode not found",
            460: "Geocode from and to not found",
            409: "Too close",
            340: "Geocode from ambiguous",
            350: "Geocode to ambiguous",
            360: "Geocode from and to ambiguous"
        },

        options: 
        [
          ['TRANSFERS', 'Le plus direct'],
          ['QUICK',     'Le plus rapide'],
          ['SAFE',      'Le plus sûr']
        ],
    
        arriveDepart: 
        [
          ['false', 'Départ'], 
          ['true',  'Arriver']
        ],
    
        maxWalkDistance : 
        [
			['200',   '200 m'],
            ['500',   '500 m'],
			['1000',   '1 km'],
			['1500',  '1,5 km'],
			['5000',  '5 km'],
			['10000',  '10 km'], 
        ],
    
        mode : 
        [
            ['TRANSIT,WALK', 'Transports publics'],
            ['BUSISH,TRAINISH,WALK', 'Bus & Train'],
            ['BUSISH,WALK', 'Bus seulement'],
            ['TRAINISH,WALK', 'Train seulement'],
            ['WALK', 'Marche seulement'],
            ['BICYCLE', 'Vélo'],
            ['TRANSIT,BICYCLE', 'Transports publics & Vélo']
        ],

        wheelchair :
        [
            ['false', 'Non requis'],
            ['true', 'Requis']
        ]
    },

    CLASS_NAME : "otp.locale.French"
};
