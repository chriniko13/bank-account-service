package com.chriniko.revolut.hometask.repository;

import com.chriniko.revolut.hometask.entity.IdentifiableLong;
import com.chriniko.revolut.hometask.error.AcquireEntityLockFailureException;
import com.chriniko.revolut.hometask.error.EntityModifiedSinceLastViewException;
import com.chriniko.revolut.hometask.error.ProcessingException;
import org.javatuples.Pair;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TransactionalRepository<E extends IdentifiableLong> {

    private static final int ACQUIRE_WRITE_LOCK_TIMEOUT_IN_SECS = 3;
    private static final int ACQUIRE_READ_LOCK_TIMEOUT_IN_SECS = 5;

    /**
     * Due to the fact that {@link java.util.concurrent.ConcurrentHashMap#compute} (lock stripping technique) operation says the following in the javadoc:
     * <p>
     * <br/>
     * <p>
     * `The entire method invocation is performed atomically.
     * Some attempted update operations on this map by other threads may be blocked while computation is in progress,
     * so the computation should be short and simple, <strong>and must not attempt to update any other mappings of this Map.</strong>`
     * <p>
     * <br/>
     * <p>
     * We have used stamped locks ({@link java.util.concurrent.locks.StampedLock}) in order to
     * make business operations between more than one entity behaving properly.
     */
    protected abstract StampedLock findStampedLock(E e);

    protected abstract Class<E> entityClass();

    protected void tryAcquireWriteLockFromOptimisticRead(E entity,
                                                         long optimisticReadStamp,
                                                         Consumer<E> accountConsumer) {
        long accountId = entity.getId();
        StampedLock stampedLock = findStampedLock(entity);

        long writeStamp = stampedLock.tryConvertToWriteLock(optimisticReadStamp);
        if (writeStamp == 0) {
            throw new EntityModifiedSinceLastViewException(accountId, entityClass());
        }
        try {
            accountConsumer.accept(entity);
        } finally {
            stampedLock.unlock(writeStamp);
        }
    }

    protected void tryAcquireWriteLock(E entity, Consumer<E> entityConsumer) {

        long accountId = entity.getId();
        StampedLock stampedLock = findStampedLock(entity);

        long writeStamp = tryAcquireWriteLock(stampedLock, accountId);
        try {
            entityConsumer.accept(entity);
        } finally {
            stampedLock.unlock(writeStamp);
        }
    }

    protected void tryAcquireWriteLock(Pair<E, E> entities, Consumer<Pair<E, E>> entitiesConsumer) {

        List<Pair<StampedLock, Long>> lockInfos = Stream
                .of(entities.getValue0(), entities.getValue1())
                .map(e -> Pair.with(findStampedLock(e), e.getId()))
                .map(p -> {
                    StampedLock stampedLock = p.getValue0();
                    long writeStamp = tryAcquireWriteLock(stampedLock, p.getValue1());
                    return Pair.with(stampedLock, writeStamp);
                })
                .collect(Collectors.toList());

        try {
            entitiesConsumer.accept(entities);
        } finally {
            lockInfos.forEach(lockInfo -> {
                StampedLock stampedLock = lockInfo.getValue0();
                Long writeStamp = lockInfo.getValue1();
                stampedLock.unlock(writeStamp);
            });
        }
    }

    protected E tryAcquireWriteLock(E entity, UnaryOperator<E> f) {

        long accountId = entity.getId();
        StampedLock stampedLock = findStampedLock(entity);

        long writeStamp = tryAcquireWriteLock(stampedLock, accountId);
        try {
            return f.apply(entity);
        } finally {
            stampedLock.unlock(writeStamp);
        }
    }

    protected <R> R tryAcquireReadLock(E entity, Supplier<R> s) {

        long accountId = entity.getId();
        StampedLock stampedLock = findStampedLock(entity);

        long readStamp = tryAcquireReadLock(stampedLock, accountId);
        try {
            return s.get();
        } finally {
            stampedLock.unlock(readStamp);
        }
    }

    private long tryAcquireWriteLock(StampedLock stampedLock, long accountId) {
        try {
            long writeStamp = stampedLock.tryWriteLock(ACQUIRE_WRITE_LOCK_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
            if (writeStamp == 0) {
                throw new AcquireEntityLockFailureException(accountId, entityClass());
            }
            return writeStamp;

        } catch (InterruptedException e) {
            if (!Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            throw new ProcessingException("could not acquire write lock", e);
        }
    }

    private long tryAcquireReadLock(StampedLock stampedLock, long accountId) {
        try {
            long readStamp = stampedLock.tryReadLock(ACQUIRE_READ_LOCK_TIMEOUT_IN_SECS, TimeUnit.SECONDS);
            if (readStamp == 0) {
                throw new AcquireEntityLockFailureException(accountId, entityClass());
            }
            return readStamp;

        } catch (InterruptedException e) {
            if (!Thread.currentThread().isInterrupted()) {
                Thread.currentThread().interrupt();
            }
            throw new ProcessingException("could not acquire read lock", e);
        }
    }

}
