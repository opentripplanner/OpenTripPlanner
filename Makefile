PYBABEL=.venv/bin/pybabel
I18NEXT=./node_modules/.bin/i18next-conv
LOCALE_FOLDER=./src/client/i18n
BABEL_CFG=$(LOCALE_FOLDER)/babel.cfg
TEMPLATE_FILE=$(LOCALE_FOLDER)/messages.pot
LANGS=sl en fr de it ca_ES pl pt es
JS_FILESPATH=./src/client/js/otp
JS_FILES = $(shell find $(JS_FILESPATH)/ -name '*.js')
LOCALE_FILES = $(shell find $(LOCALE_FOLDER)/ -name '*.po')
LAN=sl_SI

.PHONY: all
all: $(LOCALE_FILES)

.PHONY: update
update: $(TEMPLATE_FILE)

#Extracts new translation from JS files and creates PO template
$(TEMPLATE_FILE): $(JS_FILES)
	$(PYBABEL) extract --project=OpenTripPlanner -F $(BABEL_CFG) -s -k _tr -c TRANSLATORS: -o $(TEMPLATE_FILE) $(JS_FILESPATH)

#Updates translations with new unstraslated strings from template
.PHONY: update_po
update_po: $(LOCALE_FILES)

$(LOCALE_FILES): $(TEMPLATE_FILE)
	for LAN in $(LANGS); do $(PYBABEL) update --domain "$$LAN" --locale "$$LAN" --input-file $(TEMPLATE_FILE) --output-file $(LOCALE_FOLDER)/"$$LAN.po"; done

#Updates js files from new translations in po files
.PHONY: update_js
update_js: $(LOCALE_FILES)
	for LAN in $(LANGS); do $(I18NEXT) -l "$$LAN" -s "$(LOCALE_FOLDER)/$$LAN.po" -t "$(JS_FILESPATH)/locale/$$LAN.json"; done
	touch update_js

#Creates new translation with LAN culture info
.PHONY: init
init:
	#$(PYBABEL) init --domain "$(LAN)" --locale "$(LAN)" --input-file $(TEMPLATE_FILE) --output-file $(LOCALE_FOLDER)/"$(LAN).po";
	msginit -l "$(LAN)" -i $(TEMPLATE_FILE) -o "$(LOCALE_FOLDER)/$(LAN).po";

graphs/default/vvs-with-shapes.gtfs.zip:
	mkdir -p graphs/default
	wget https://gtfs.mfdz.de/gtfs/VVS.with-shapes.gtfs.zip -O graphs/default/vvs-with-shapes.gtfs.zip

graphs/default/stuttgart.pbf:
	mkdir -p graphs/default
	wget http://download.geofabrik.de/europe/germany/baden-wuerttemberg/stuttgart-regbez-latest.osm.pbf -O graphs/default/stuttgart.pbf

build-herrenberg: graphs/default/vvs-with-shapes.gtfs.zip graphs/default/stuttgart.pbf
	java -Xmx4G -jar otp.jar --build ./graphs/default

run:
	java -Xmx5G -server -jar otp.jar --server --basePath ./ --router default --insecure

rebuild:
	mvn package -DskipTests
	cp target/otp-1.5.0-SNAPSHOT-shaded.jar ./otp.jar
