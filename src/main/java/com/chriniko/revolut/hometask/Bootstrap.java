package com.chriniko.revolut.hometask;

import com.chriniko.revolut.hometask.error.ProcessingException;
import com.chriniko.revolut.hometask.http.HttpEnvironment;
import lombok.extern.log4j.Log4j2;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;
import java.util.concurrent.CountDownLatch;

@Log4j2
public class Bootstrap {

    public static void main(String[] args) {

        log.info("bootstrapping money transfer service...");

        SeContainerInitializer containerInit = SeContainerInitializer.newInstance();

        SeContainer container = containerInit.initialize();
        Instance<HttpEnvironment> httpEnvironmentInstance = container.select(HttpEnvironment.class);

        final CountDownLatch shutdownLatch = new CountDownLatch(1);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("will shutdown http environment...");
            shutdownLatch.countDown();
        }));

        HttpEnvironment httpEnvironment = httpEnvironmentInstance.get();
        httpEnvironment.run();

        try {
            shutdownLatch.await();
            log.info("http environment shut down...");
        } catch (InterruptedException e) {
            if (!Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            throw new ProcessingException("could not shutdown gracefully the undertow env", e);
        } finally {
            if (container.isRunning()) {
                container.close();
            }
        }

    }

}
