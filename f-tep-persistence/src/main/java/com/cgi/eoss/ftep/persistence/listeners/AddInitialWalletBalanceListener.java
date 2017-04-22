package com.cgi.eoss.ftep.persistence.listeners;

import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.persistence.service.WalletDataService;
import lombok.extern.log4j.Log4j2;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManagerFactory;

@Component
@Log4j2
// TODO Replace this with an AOP aspect
public class AddInitialWalletBalanceListener implements PostInsertEventListener {

    private final EntityManagerFactory entityManagerFactory;
    private final WalletDataService walletDataService;
    private final int defaultWalletBalance;

    public AddInitialWalletBalanceListener(EntityManagerFactory entityManagerFactory,
                                           WalletDataService walletDataService,
                                           @Value("${ftep.user.default-wallet-balance:100}") int defaultWalletBalance) {
        this.entityManagerFactory = entityManagerFactory;
        this.walletDataService = walletDataService;
        this.defaultWalletBalance = defaultWalletBalance;
    }

    @PostConstruct
    protected void registerSelf() {
        SessionFactoryImpl sessionFactory = entityManagerFactory.unwrap(SessionFactoryImpl.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.getEventListenerGroup(EventType.POST_INSERT).appendListener(this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        // A new User gets a new Wallet, so hook that event rather than the User creation itself
        if (Wallet.class.equals(event.getEntity().getClass())) {
            Wallet wallet = (Wallet) event.getEntity();
            walletDataService.creditBalance(wallet, defaultWalletBalance);
        }
    }

    @Override
    public boolean requiresPostCommitHanding(EntityPersister persister) {
        return true;
    }

}
