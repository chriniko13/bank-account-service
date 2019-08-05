package com.chriniko.revolut.hometask.account.service;

import com.chriniko.revolut.hometask.account.dto.AccountDto;
import com.chriniko.revolut.hometask.account.dto.CreateAccountRequest;
import com.chriniko.revolut.hometask.account.entity.Account;
import com.chriniko.revolut.hometask.account.repository.AccountRepository;
import com.chriniko.revolut.hometask.error.EntityNotFoundException;
import org.javatuples.Pair;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
public class AccountService {

    private final AccountRepository accountRepository;

    @Inject
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public void process(CreateAccountRequest createAccountRequest) {
        Account account = new Account();
        account.setName(createAccountRequest.getName());
        account.setAddress(createAccountRequest.getAddress());
        account.setBalance(createAccountRequest.getBalance());

        accountRepository.create(account);
    }

    public List<AccountDto> findAll() {
        List<Account> accounts = accountRepository.findAll();
        return accounts
                .stream()
                .map(AccountDto::new)
                .collect(Collectors.toList());
    }

    public Pair<AccountDto, Long> find(long accountId) {
        return accountRepository
                .find(accountId)
                .map(account -> Pair.with(new AccountDto(account), account.getOptimisticReadStamp()))
                .orElseThrow(() -> new EntityNotFoundException("could not found account with provided id: " + accountId));
    }

    public AccountDto delete(long accountId) {
        return accountRepository
                .delete(accountId)
                .map(AccountDto::new)
                .orElseThrow(() -> new EntityNotFoundException("could not found account with provided id: " + accountId));
    }
}
