package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.QWallet;
import com.cgi.eoss.ftep.model.QWalletTransaction;
import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.model.WalletTransaction;
import com.cgi.eoss.ftep.persistence.dao.FtepEntityDao;
import com.cgi.eoss.ftep.persistence.dao.WalletDao;
import com.cgi.eoss.ftep.persistence.dao.WalletTransactionDao;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class JpaWalletDataService extends AbstractJpaDataService<Wallet> implements WalletDataService {

    private final WalletDao dao;

    private final WalletTransactionDao transactionDao;

    @Autowired
    public JpaWalletDataService(WalletDao WalletDao, WalletTransactionDao transactionDao) {
        this.dao = WalletDao;
        this.transactionDao = transactionDao;
    }

    @Override
    FtepEntityDao<Wallet> getDao() {
        return dao;
    }

    @Override
    Predicate getUniquePredicate(Wallet entity) {
        return QWallet.wallet.owner.eq(entity.getOwner());
    }

    @Override
    public Wallet findByOwner(User user) {
        return dao.findOneByOwner(user);
    }

    @Override
    @Transactional
    public void transact(WalletTransaction transaction) {
        Wallet wallet = transaction.getWallet();
        wallet.getTransactions().add(transactionDao.save(transaction));
        int balanceChange = transaction.getBalanceChange();
        wallet.setBalance(wallet.getBalance() + balanceChange);
    }

    @Override
    @Transactional
    public void creditBalance(Wallet wallet, int amount) {
        transact(WalletTransaction.builder()
                .wallet(wallet)
                .balanceChange(amount)
                .transactionTime(LocalDateTime.now(ZoneOffset.UTC))
                .type(WalletTransaction.Type.CREDIT)
                .associatedId(null)
                .build());
    }

    @Override
    public List<WalletTransaction> getDownloadTransactionsByTimeAndOwner(YearMonth yearMonth, Long userId) {
        QWalletTransaction walletTransaction = QWalletTransaction.walletTransaction;

        BooleanExpression exp = walletTransaction.transactionTime.year().eq(yearMonth.getYear())
                .and(walletTransaction.transactionTime.month().eq(yearMonth.getMonthValue()))
                .and(walletTransaction.type.eq(WalletTransaction.Type.DOWNLOAD));
        if (Optional.ofNullable(userId).isPresent()) {
            exp = exp.and(walletTransaction.wallet.owner.id.eq(userId));
        }

        return transactionDao.findAll(exp);
    }

    @Override
    public Optional<WalletTransaction> getLaunchTransactionByJobId(Long jobId) {
        return transactionDao.findOne(QWalletTransaction.walletTransaction.associatedId.eq(jobId)
                .and(QWalletTransaction.walletTransaction.type.eq(WalletTransaction.Type.JOB)));
    }

}
