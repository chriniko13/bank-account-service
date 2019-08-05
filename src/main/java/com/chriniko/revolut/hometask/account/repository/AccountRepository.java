package com.chriniko.revolut.hometask.account.repository;

import com.chriniko.revolut.hometask.account.entity.Account;
import com.chriniko.revolut.hometask.error.AcquireWriteLockFailureException;
import com.chriniko.revolut.hometask.error.ProcessingException;
import com.chriniko.revolut.hometask.time.UtcZone;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;


@ApplicationScoped
public class AccountRepository {

    private static final int ACQUIRE_WRITE_LOCK_TIMEOUT_IN_SECS = 3;

    private final Clock clock;

    private final AtomicLong idGenerator;
    private final ConcurrentHashMap<Long, Account> accountsById;
    private final ConcurrentHashMap<Long, StampedLock> stampedLocksById;

    @Inject
    private AccountRepository(@UtcZone Clock clock) {
        this.clock = clock;
        idGenerator = new AtomicLong(0);
        accountsById = new ConcurrentHashMap<>();
        stampedLocksById = new ConcurrentHashMap<>();
    }

    public void create(Account account) {
        long id = idGenerator.incrementAndGet();
        account.setId(id);
        account.setCreated(Instant.now(clock));
        accountsById.put(id, account);
        stampedLocksById.put(id, new StampedLock());
    }

    public List<Account> findAll() {
        return new LinkedList<>(accountsById.values());
    }

    public void clear() {
        idGenerator.set(0);
        accountsById.clear();
        stampedLocksById.clear();
    }

    public Optional<Account> find(long accountId) {
        Account foundRecord = accountsById.compute(accountId, (id, record) -> {
            if (record == null) {
                return null;
            }
            StampedLock stampedLock = stampedLocksById.get(id);
            long optimisticReadStamp = stampedLock.tryOptimisticRead();
            record.setOptimisticReadStamp(optimisticReadStamp);

            return record;
        });

        return Optional.ofNullable(foundRecord);
    }

    public Optional<Account> delete(long accountId) {

        Account deletedRecord = accountsById.compute(accountId, (id, record) -> {
            if (record == null) {
                return null;
            }
            StampedLock stampedLock = stampedLocksById.get(id);
            long stamp = tryAcquireWriteLock(stampedLock);
            if (stamp == 0) { // Note: lock is not available.
                throw new AcquireWriteLockFailureException();
            }

            try {
                return accountsById.remove(accountId);
            } finally {
                stampedLocksById.remove(accountId);
                stampedLock.unlock(stamp);
            }
        });

        return Optional.ofNullable(deletedRecord);
    }

    private long tryAcquireWriteLock(StampedLock stampedLock) {
        try {
            return stampedLock.tryWriteLock(ACQUIRE_WRITE_LOCK_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            if (!Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            throw new ProcessingException("could not acquire write lock", e);
        }
    }
}
