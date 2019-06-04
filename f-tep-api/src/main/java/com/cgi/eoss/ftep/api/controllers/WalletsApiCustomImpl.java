package com.cgi.eoss.ftep.api.controllers;

import com.cgi.eoss.ftep.model.QUser;
import com.cgi.eoss.ftep.model.QWallet;
import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.persistence.dao.WalletDao;
import com.cgi.eoss.ftep.security.FtepSecurityService;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberPath;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Getter
@Component
public class WalletsApiCustomImpl extends BaseRepositoryApiImpl<Wallet> implements WalletsApiCustom {

    private final FtepSecurityService securityService;
    private final WalletDao dao;

    public WalletsApiCustomImpl(FtepSecurityService securityService, WalletDao dao) {
        super(Wallet.class);
        this.securityService = securityService;
        this.dao = dao;
    }

    @Override
    NumberPath<Long> getIdPath() {
        return QWallet.wallet.id;
    }

    @Override
    QUser getOwnerPath() {
        return QWallet.wallet.owner;
    }

    @Override
    Class<Wallet> getEntityClass() {
        return Wallet.class;
    }

    @Override
    public Page<Wallet> findAll(Pageable pageable) {
        if (getSecurityService().isAdmin()) {
            return getDao().findAll(pageable);
        } else {
            BooleanExpression isOwned = QWallet.wallet.owner.eq(getSecurityService().getCurrentUser());
            return getDao().findAll(isOwned, pageable);
        }
    }

}
