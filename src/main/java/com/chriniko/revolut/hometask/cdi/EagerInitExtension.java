package com.chriniko.revolut.hometask.cdi;

import lombok.extern.log4j.Log4j2;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.*;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class EagerInitExtension implements Extension {

    private final List<Bean<?>> eagerBeansList = new ArrayList<Bean<?>>();

    void beforeBeanDiscovery(@Observes BeforeBeanDiscovery bbd) {
        log.debug("beginning the scanning process");
    }

    <T> void processBean(@Observes ProcessBean<T> pb) {
        Bean<T> bean = pb.getBean();
        String beanName = bean.getBeanClass().getSimpleName();

        Annotated annotated = pb.getAnnotated();
        boolean isApplicationScoped = annotated.getAnnotation(ApplicationScoped.class) != null;
        boolean isEager = annotated.getAnnotation(Eager.class) != null;

        if (isApplicationScoped && isEager) {
            eagerBeansList.add(bean);
            log.debug("bean: " + beanName + " is eager");
        }
    }

    void afterBeanDiscovery(@Observes AfterBeanDiscovery abd) {
        log.debug("finished the scanning process");
    }

    void afterDeploymentValidation(@Observes AfterDeploymentValidation adv, BeanManager beanManager) {
        log.debug("finished the after deployment validation");

        for (Bean<?> bean : eagerBeansList) {
            CreationalContext<?> creationalContext = beanManager.createCreationalContext(bean);
            Object obj = beanManager.getReference(bean, bean.getBeanClass(), creationalContext);
            obj.toString(); // Note: In order to instantiate it (eager).
        }
    }

}
