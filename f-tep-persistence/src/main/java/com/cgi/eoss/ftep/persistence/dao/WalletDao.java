package com.cgi.eoss.ftep.persistence.dao;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.Wallet;

public interface WalletDao extends FtepEntityDao<Wallet> {
    Wallet findOneByOwner(User user);
}
