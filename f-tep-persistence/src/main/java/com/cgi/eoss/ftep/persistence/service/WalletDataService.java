package com.cgi.eoss.ftep.persistence.service;

import com.cgi.eoss.ftep.model.User;
import com.cgi.eoss.ftep.model.Wallet;
import com.cgi.eoss.ftep.model.WalletTransaction;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface WalletDataService extends
        FtepEntityDataService<Wallet> {
    Wallet findByOwner(User user);
    void transact(WalletTransaction transaction);
    void creditBalance(Wallet wallet, int amount);
    List<WalletTransaction> getDownloadTransactionsByTimeAndOwner(YearMonth period, Long userId);
    Optional<WalletTransaction> getLaunchTransactionByJobId(Long jobId);
}
