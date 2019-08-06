package com.chriniko.revolut.hometask.jmx;

import com.chriniko.revolut.hometask.account.repository.AccountRepository;
import com.chriniko.revolut.hometask.cdi.Eager;
import lombok.extern.log4j.Log4j2;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.management.*;
import java.lang.management.ManagementFactory;

@Log4j2

@Eager
@ApplicationScoped
public class JmxRegistrer {

    @PostConstruct
    void init() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();

        ObjectName objectName;
        try {
            objectName = new ObjectName("com.chriniko.revolut.hometask.account.repository:type=basic,name=accountRepository");
        } catch (MalformedObjectNameException e) {
            log.error("error during object name construction, message: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }

        Instance<AccountRepository> accountRepositoryInstance = CDI.current().select(AccountRepository.class);
        AccountRepository accountRepository = accountRepositoryInstance.get();

        try {
            if (!server.isRegistered(objectName)) {
                server.registerMBean(accountRepository, objectName);
            }
        } catch (InstanceAlreadyExistsException | MBeanRegistrationException | NotCompliantMBeanException e) {
            log.error("error during jmx registration, message: " + e.getMessage(), e);
            throw new RuntimeException(e);
        }

        log.debug("Registration for Account Repository mbean with the platform server is successful");
        log.debug("Please open VisualVM to access Account Repository mbean");
    }

}
