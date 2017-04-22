package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.model.WalletTransaction;

public interface WalletDataService extends
        FtepEntityDataService<Wallet> {
    Wallet findByOwner(User user);
    void transact(WalletTransaction transaction);
    void creditBalance(Wallet wallet, int amount);
}
