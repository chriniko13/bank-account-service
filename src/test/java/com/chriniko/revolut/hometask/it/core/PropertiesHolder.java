package com.chriniko.revolut.hometask.it.core;

import com.chriniko.revolut.hometask.property.Property;

import javax.inject.Inject;

public class PropertiesHolder {

    @Inject
    @Property("server.port")
    int serverPort;

    @Inject
    @Property("server.host")
    String serverHost;
}
