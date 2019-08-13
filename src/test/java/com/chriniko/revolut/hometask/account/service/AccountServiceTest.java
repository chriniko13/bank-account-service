package com.chriniko.revolut.hometask.account.service;

import com.chriniko.revolut.hometask.account.dto.AccountDto;
import com.chriniko.revolut.hometask.account.entity.Account;
import com.chriniko.revolut.hometask.account.entity.Address;
import com.chriniko.revolut.hometask.account.entity.Name;
import com.chriniko.revolut.hometask.account.repository.AccountRepository;
import org.javatuples.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class AccountServiceTest {

    private AccountService accountService;

    @Mock
    private AccountRepository accountRepository;

    @Before
    public void setup() {
        accountService = new AccountService(accountRepository);
    }

    @Test
    public void find() {

        // given
        long accountId = 1;
        boolean acquireReadLock = true;

        long optimisticReadStamp = 123;

        Account account = new Account(
                new Name("f", "i", "l"),
                new Address("n", "s", "c", "s", "z")
        );
        account.setOptimisticReadStamp(optimisticReadStamp);

        Mockito.when(accountRepository.find(accountId, acquireReadLock)).thenReturn(account);


        // when
        Pair<AccountDto, Long> result = accountService.find(accountId, acquireReadLock);


        // then
        assertNotNull(result);
        assertEquals(optimisticReadStamp, result.getValue1().longValue());
        assertEquals("f", result.getValue0().getName().getFirst());
        assertEquals("i", result.getValue0().getName().getInitials());
        assertEquals("l", result.getValue0().getName().getLast());

        Mockito.verify(accountRepository).find(accountId, acquireReadLock);

    }
}