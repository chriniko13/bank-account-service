package com.chriniko.revolut.hometask.property;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@ApplicationScoped
public class PropertyProducer {

    private Properties properties;

    @PostConstruct
    public void init() {
        this.properties = new Properties();

        final InputStream stream = PropertyProducer.class.getResourceAsStream("/application.properties");
        if (stream == null) {
            throw new IllegalStateException("could not find configuration file");
        }
        try {
            this.properties.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException("could not load configuration", e);
        }

    }

    @Property
    @Produces
    @Dependent
    public String produceString(final InjectionPoint ip) {
        Property property = ip.getAnnotated().getAnnotation(Property.class);
        String propertyName = property.value();

        return this.properties.getProperty(propertyName);
    }

    @Property
    @Produces
    @Dependent
    public int produceInt(final InjectionPoint ip) {
        Property property = ip.getAnnotated().getAnnotation(Property.class);
        String propertyName = property.value();

        String value = this.properties.getProperty(propertyName);
        return Integer.parseInt(value);
    }

}
