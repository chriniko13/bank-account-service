package com.chriniko.revolut.hometask.account.repository;

import com.chriniko.revolut.hometask.account.entity.Account;
import com.chriniko.revolut.hometask.account.entity.Currency;
import com.chriniko.revolut.hometask.account.entity.Transaction;
import com.chriniko.revolut.hometask.account.entity.TransactionType;
import com.chriniko.revolut.hometask.account.error.AccountNotFoundException;
import com.chriniko.revolut.hometask.account.error.InsufficientFundsException;
import com.chriniko.revolut.hometask.error.ProcessingException;
import com.chriniko.revolut.hometask.repository.TransactionalRepository;
import com.chriniko.revolut.hometask.time.UtcZone;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javatuples.Pair;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;


@ApplicationScoped
public class AccountRepository extends TransactionalRepository<Account> implements AccountRepositoryMBean {

    private static final String DEFAULT_DEBIT_DETAILS = "debit transaction";
    private static final String DEFAULT_CREDIT_DETAILS = "credit transaction";

    private static final String DEBIT_TRANSFER_MSG_TEMPLATE = "transferred money to account with id: %s";
    private static final String CREDIT_TRANSFER_MSG_TEMPLATE = "money transferred from account with id: %s";

    private final Clock clock;

    private final AtomicLong idGenerator;
    private final ConcurrentHashMap<Long, Account> accountsById;
    private final ConcurrentHashMap<Long, StampedLock> stampedLocksById;

    private final ObjectMapper objectMapper;

    @Inject
    private AccountRepository(@UtcZone Clock clock, ObjectMapper objectMapper) {
        this.clock = clock;
        this.objectMapper = objectMapper;
        idGenerator = new AtomicLong(0);
        accountsById = new ConcurrentHashMap<>();
        stampedLocksById = new ConcurrentHashMap<>();
    }

    @Override
    protected StampedLock findStampedLock(Account account) {
        return stampedLocksById.get(account.getId());
    }

    @Override
    protected Class<Account> entityClass() {
        return Account.class;
    }

    public List<Account> findAll() {
        return new LinkedList<>(accountsById.values());
    }

    public void clear() {
        idGenerator.set(0);
        accountsById.clear();
        stampedLocksById.clear();
    }

    public void create(Account account) {
        long id = idGenerator.incrementAndGet();
        account.setId(id);
        account.setCreated(Instant.now(clock));
        accountsById.put(id, account);
        stampedLocksById.put(id, new StampedLock());
    }

    public Account find(long accountId, boolean acquireReadLock) {

        Account account = ensureAccountExists(accountId);
        return getAccountForRead(acquireReadLock, account);
    }

    public Pair<Long, List<Transaction>> findTransactions(Long accountId, boolean acquireReadLock) {

        Account account = ensureAccountExists(accountId);

        final Account record = getAccountForRead(acquireReadLock, account);

        return Pair.with(record.getOptimisticReadStamp(), record.getTransactions());
    }

    public Account delete(long accountId) {

        Account account = ensureAccountExists(accountId);

        UnaryOperator<Account> deleteAccountOperation = _account -> {
            stampedLocksById.remove(_account.getId());
            return accountsById.remove(_account.getId());
        };

        return tryAcquireWriteLock(account, deleteAccountOperation);
    }

    public void update(Account toBeAccount, Long optimisticReadStamp) {
        long accountId = toBeAccount.getId();

        Account asIsAccount = ensureAccountExists(accountId);
        merge(toBeAccount, asIsAccount);

        Consumer<Account> accountUpdateOperation = _account -> {
            _account.setUpdated(Instant.now(clock));
            accountsById.put(accountId, _account);
        };

        if (optimisticReadStamp != null) {
            tryAcquireWriteLockFromOptimisticRead(toBeAccount, optimisticReadStamp, accountUpdateOperation);
        } else {
            tryAcquireWriteLock(toBeAccount, accountUpdateOperation);
        }
    }

    public void debit(long accountId, BigDecimal amount, String details, Long optimisticReadStamp) {

        Account account = ensureAccountExists(accountId);

        Consumer<Account> accountDebitOperation = _account -> {
            Instant now = Instant.now(clock);

            BigDecimal existingAmount = _account.getBalance().getAmount();

            ensureFundsExist(amount, existingAmount);

            Transaction tx = new Transaction(
                    TransactionType.DEBIT,
                    now,
                    amount,
                    Currency.EURO,
                    Optional.ofNullable(details).orElse(DEFAULT_DEBIT_DETAILS)
            );

            _account.applyTransaction(tx);
            _account.setUpdated(now);
        };

        if (optimisticReadStamp != null) {
            tryAcquireWriteLockFromOptimisticRead(account, optimisticReadStamp, accountDebitOperation);
        } else {
            tryAcquireWriteLock(account, accountDebitOperation);
        }
    }

    public void credit(long accountId, BigDecimal amount, String details, Long optimisticReadStamp) {

        Account account = ensureAccountExists(accountId);

        Consumer<Account> accountCreditOperation = _account -> {
            Instant now = Instant.now(clock);

            Transaction tx = new Transaction(
                    TransactionType.CREDIT,
                    now,
                    amount,
                    Currency.EURO,
                    Optional.ofNullable(details).orElse(DEFAULT_CREDIT_DETAILS)
            );

            _account.applyTransaction(tx);
            _account.setUpdated(now);
        };

        if (optimisticReadStamp != null) {
            tryAcquireWriteLockFromOptimisticRead(account, optimisticReadStamp, accountCreditOperation);
        } else {
            tryAcquireWriteLock(account, accountCreditOperation);
        }
    }

    public void transfer(long sourceAccountId, long destinationAccountId, BigDecimal amount, String details) {

        Account sourceAccount = ensureAccountExists(sourceAccountId);
        Account destinationAccount = ensureAccountExists(destinationAccountId);

        Consumer<Pair<Account, Account>> transferOperation = accounts -> {

            Account from = accounts.getValue0();
            Account to = accounts.getValue1();

            ensureFundsExist(amount, from.getBalance().getAmount());

            Instant now = Instant.now();

            Transaction debitTx = new Transaction(
                    TransactionType.DEBIT,
                    now,
                    amount,
                    Currency.EURO,
                    Optional.ofNullable(details)
                            .orElse(String.format(DEBIT_TRANSFER_MSG_TEMPLATE, to.getId()))
            );

            Transaction creditTx = new Transaction(
                    TransactionType.CREDIT,
                    now,
                    amount,
                    Currency.EURO,
                    Optional.ofNullable(details)
                            .orElse(String.format(CREDIT_TRANSFER_MSG_TEMPLATE, from.getId()))
            );

            from.applyTransaction(debitTx);
            from.setUpdated(now);

            to.applyTransaction(creditTx);
            to.setUpdated(now);
        };

        tryAcquireWriteLock(Pair.with(sourceAccount, destinationAccount), transferOperation);
    }

    /*
        Note: JMX exposed method.
     */
    @Override
    public Map<Long, String> getAllAccounts() {
        return accountsById
                .entrySet()
                .stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                e -> {
                                    Account account = e.getValue();
                                    try {
                                        return objectMapper.writeValueAsString(account);
                                    } catch (JsonProcessingException error) {
                                        throw new ProcessingException("could not serialize account", error);
                                    }
                                }
                        )
                );
    }

    // --- internals ---

    private void merge(Account toBeAccount, Account asIsAccount) {
        Instant created = asIsAccount.getCreated();
        toBeAccount.setCreated(created);
    }

    private void ensureFundsExist(BigDecimal amountToWithdraw, BigDecimal existingAmount) {
        if (amountToWithdraw.compareTo(existingAmount) > 0) {
            throw new InsufficientFundsException();
        }
    }

    private Account ensureAccountExists(long accountId) {
        Account account = accountsById.get(accountId);
        if (account == null) {
            throw new AccountNotFoundException("could not found account with provided id: " + accountId);
        }
        return account;
    }

    private Account getAccountForRead(boolean acquireReadLock, Account account) {

        final Account record;
        if (acquireReadLock) {
            record = tryAcquireReadLock(account, () -> account);
        } else {
            record = account;
        }

        StampedLock stampedLock = stampedLocksById.get(record.getId());
        long optimisticReadStamp = stampedLock.tryOptimisticRead();
        record.setOptimisticReadStamp(optimisticReadStamp);

        return record;
    }


}
