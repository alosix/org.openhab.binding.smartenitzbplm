FROM openhab/openhab:2.5.1-amd64-debian


ADD docker/keys.txt /openhab/dist/userdata/etc/keys.properties
ADD docker/keys.txt /openhab/userdata/etc/keys.properties