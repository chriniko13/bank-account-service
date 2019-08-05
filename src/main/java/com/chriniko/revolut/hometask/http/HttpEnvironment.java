package com.chriniko.revolut.hometask.http;

import com.chriniko.revolut.hometask.property.Property;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.util.StatusCodes;
import lombok.extern.log4j.Log4j2;
import org.jboss.weld.environment.se.events.ContainerBeforeShutdown;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@Log4j2

@ApplicationScoped
public class HttpEnvironment {

    private final Instance<RouteDefinition> routeDefinitions;

    @Inject
    @Property("server.port")
    private int serverPort;

    @Inject
    @Property("server.host")
    private String serverHost;

    private Undertow undertow;

    @Inject
    public HttpEnvironment(@Any Instance<RouteDefinition> routeDefinitions) {
        this.routeDefinitions = routeDefinitions;
    }

    @PostConstruct
    void init() {
        RoutingHandler routingHandler = defineRoutes();
        undertow = createServer(routingHandler);
    }

    public void clear(@Observes ContainerBeforeShutdown evt) {
        if (undertow != null) {
            log.info("will shutdown http environment...");
            undertow.stop();
        }
    }

    public void run() {
        undertow.start();
        log.info("http environment up and running at: http://" + serverHost + ":" + serverPort);
    }

    private RoutingHandler defineRoutes() {
        RoutingHandler routingHandler = Handlers.routing();

        routeDefinitions.forEach(routeDefinition -> {
            routingHandler.add(routeDefinition.httpMethod(), routeDefinition.url(), routeDefinition.httpHandler());
        });

        routingHandler.setFallbackHandler(exchange -> {
            exchange.setStatusCode(StatusCodes.BAD_REQUEST);
            exchange.getResponseSender().send("no route found for the provided url");
        });

        return routingHandler;
    }

    private Undertow createServer(RoutingHandler routingHandler) {
        return Undertow.builder()
                .addHttpListener(serverPort, serverHost)
                .setHandler(routingHandler)
                .build();
    }
}
