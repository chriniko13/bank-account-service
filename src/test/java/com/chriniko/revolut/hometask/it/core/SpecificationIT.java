package com.chriniko.revolut.hometask.it.core;

import com.chriniko.revolut.hometask.account.repository.AccountRepository;
import com.chriniko.revolut.hometask.http.HttpEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

public class SpecificationIT {

    protected static SeContainer container;

    protected static OkHttpClient httpClient;

    protected static PropertiesHolder propertiesHolder;

    protected static String baseUrl;

    protected static ObjectMapper objectMapper;

    protected static AccountRepository accountRepository;

    @BeforeClass
    public static void globalSetup() {
        SeContainerInitializer containerInit = SeContainerInitializer.newInstance();

        // Note: add your test infra beans here
        containerInit.addBeanClasses(PropertiesHolder.class);
        containerInit.addBeanClasses(ErrorHandlingDemonstrationRoute.class);

        container = containerInit.initialize();

        Instance<HttpEnvironment> httpEnvironmentInstance = container.select(HttpEnvironment.class);
        HttpEnvironment httpEnvironment = httpEnvironmentInstance.get();
        httpEnvironment.run();

        propertiesHolder = container.select(PropertiesHolder.class).get();
        baseUrl = "http://" + propertiesHolder.serverHost + ":" + propertiesHolder.serverPort;

        accountRepository = container.select(AccountRepository.class).get();

        httpClient = new OkHttpClient();
        objectMapper = new ObjectMapper();
    }

    @AfterClass
    public static void globalClear() {
        if (container.isRunning()) {
            container.close();
        }
    }

}
