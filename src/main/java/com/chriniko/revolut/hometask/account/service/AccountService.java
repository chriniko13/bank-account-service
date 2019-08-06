package com.chriniko.revolut.hometask.account.service;

import com.chriniko.revolut.hometask.account.dto.*;
import com.chriniko.revolut.hometask.account.entity.Account;
import com.chriniko.revolut.hometask.account.entity.Transaction;
import com.chriniko.revolut.hometask.account.repository.AccountRepository;
import com.chriniko.revolut.hometask.interceptor.LogInvocation;
import org.javatuples.Pair;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

@LogInvocation
@ApplicationScoped
public class AccountService {

    private final AccountRepository accountRepository;

    @Inject
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void create(ModifyAccountRequest modifyAccountRequest) {
        Account account = new Account(
                modifyAccountRequest.getName(),
                modifyAccountRequest.getAddress()
        );
        accountRepository.create(account);
    }

    public List<AccountDto> findAll() {
        return accountRepository
                .findAll()
                .stream()
                .map(AccountDto::new)
                .collect(Collectors.toList());
    }

    public Pair<AccountDto, Long> find(long accountId, boolean acquireReadLock) {
        Account findResult = accountRepository.find(accountId, acquireReadLock);
        return Pair.with(new AccountDto(findResult), findResult.getOptimisticReadStamp());
    }

    public AccountDto delete(long accountId) {
        Account account = accountRepository.delete(accountId);
        return new AccountDto(account);
    }

    public void update(long accountId, ModifyAccountRequest modifyAccountRequest, Long optimisticReadStamp) {
        Account account = new Account(
                accountId,
                modifyAccountRequest.getName(),
                modifyAccountRequest.getAddress()
        );
        accountRepository.update(account, optimisticReadStamp);
    }

    public void credit(long accountId, ModifyBalanceRequest modifyBalanceRequest, Long optimisticReadStamp) {
        accountRepository.credit(accountId,
                modifyBalanceRequest.getAmount(),
                modifyBalanceRequest.getDetails(),
                optimisticReadStamp);
    }

    public void debit(long accountId, ModifyBalanceRequest modifyBalanceRequest, Long optimisticReadStamp) {
        accountRepository.debit(accountId,
                modifyBalanceRequest.getAmount(),
                modifyBalanceRequest.getDetails(),
                optimisticReadStamp);
    }

    public void transfer(TransferAmountRequest request) {
        accountRepository.transfer(request.getSourceAccountId(),
                request.getDestinationAccountId(),
                request.getAmount(),
                request.getDetails());
    }

    public Pair<Long, List<TransactionDto>> findTransactions(Long accountId, boolean acquireReadLock) {

        Pair<Long, List<Transaction>> result = accountRepository.findTransactions(accountId, acquireReadLock);

        return Pair.with(
                result.getValue0(),
                result.getValue1()
                        .stream()
                        .map(TransactionDto::new)
                        .collect(Collectors.toList())
        );
    }
}
