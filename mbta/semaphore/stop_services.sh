#!/bin/bash

# Stop some high-memory service running on Semaphore
# Turn off some high-memory apps
SERVICES="cassandra elasticsearch mysql memcached mongod docker sphinxsearch postgresql apache2 redis-server rabbitmq-server"

for service in $SERVICES; do
    sudo service $service stop
done
killall Xvfb