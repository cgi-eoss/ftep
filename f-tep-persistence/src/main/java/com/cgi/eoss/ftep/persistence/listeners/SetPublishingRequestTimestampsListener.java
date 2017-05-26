package com.cgi.eoss.ftep.persistence.listeners;

import com.cgi.eoss.ftep.model.PublishingRequest;
import lombok.extern.log4j.Log4j2;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
@Log4j2
// TODO Replace this with an AOP aspect
public class SetPublishingRequestTimestampsListener implements PreInsertEventListener, PreUpdateEventListener {

    private final EntityManagerFactory entityManagerFactory;

    public SetPublishingRequestTimestampsListener(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    @PostConstruct
    protected void registerSelf() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.PRE_INSERT).appendListener(this);
        registry.getEventListenerGroup(EventType.PRE_UPDATE).appendListener(this);
    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        if (PublishingRequest.class.equals(event.getEntity().getClass())) {
            PublishingRequest publishingRequest = (PublishingRequest) event.getEntity();
            publishingRequest.setRequestTime(LocalDateTime.now(ZoneOffset.UTC));
            publishingRequest.setUpdatedTime(LocalDateTime.now(ZoneOffset.UTC));
        }
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        if (PublishingRequest.class.equals(event.getEntity().getClass())) {
            PublishingRequest publishingRequest = (PublishingRequest) event.getEntity();
            publishingRequest.setUpdatedTime(LocalDateTime.now(ZoneOffset.UTC));
        }
        return false;
    }
}
