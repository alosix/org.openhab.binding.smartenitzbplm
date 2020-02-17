FROM openhab/openhab:2.5.1-amd64-debian


ADD target/org.openhab.binding.smartenitzbplm-2.5.1.jar /openhab/addons/org.openhab.binding.smartenitzbplm-2.5.1.jar

ADD docker/keys.txt /openhab/dist/userdata/etc/keys.properties
ADD docker/keys.txt /openhab/userdata/etc/keys.properties