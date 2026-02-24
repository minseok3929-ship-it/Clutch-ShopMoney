package com.clutch.shopmoney.service;

import com.clutch.shopmoney.model.Account;
import com.clutch.shopmoney.repository.AccountRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class MoneyService {
    private final AccountRepository repository;

    public MoneyService(AccountRepository repository) {
        this.repository = repository;
    }

    public void ensure(UUID uuid, String name) throws SQLException { repository.ensure(uuid, name); }

    public long balance(UUID uuid) throws SQLException {
        return repository.find(uuid).map(Account::balance).orElse(0L);
    }

    public boolean withdraw(UUID uuid, long amount) throws SQLException {
        if (balance(uuid) < amount) return false;
        repository.addBalance(uuid, -amount);
        return true;
    }

    public void deposit(UUID uuid, long amount) throws SQLException { repository.addBalance(uuid, amount); }

    public List<Account> top10() throws SQLException { return repository.top10(); }
}
