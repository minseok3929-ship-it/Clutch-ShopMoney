package com.clutch.shopmoney.repository;

import com.clutch.shopmoney.model.Account;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AccountRepository {
    private final Database database;

    public AccountRepository(Database database) {
        this.database = database;
    }

    public void ensure(UUID uuid, String name) throws SQLException {
        try (Connection conn = database.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("INSERT OR IGNORE INTO accounts(uuid,name,balance) VALUES (?,?,0)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, name);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement("UPDATE accounts SET name=? WHERE uuid=?")) {
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
        }
    }

    public Optional<Account> find(UUID uuid) throws SQLException {
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT uuid,name,balance FROM accounts WHERE uuid=?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Account(UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getLong("balance")));
            }
        }
    }

    public long addBalance(UUID uuid, long delta) throws SQLException {
        try (Connection conn = database.getConnection()) {
            conn.setAutoCommit(false);
            long bal;
            try (PreparedStatement get = conn.prepareStatement("SELECT balance FROM accounts WHERE uuid=?")) {
                get.setString(1, uuid.toString());
                try (ResultSet rs = get.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Account missing");
                    bal = rs.getLong(1);
                }
            }
            long next = bal + delta;
            if (next < 0) {
                conn.rollback();
                throw new SQLException("negative balance");
            }
            try (PreparedStatement up = conn.prepareStatement("UPDATE accounts SET balance=? WHERE uuid=?")) {
                up.setLong(1, next);
                up.setString(2, uuid.toString());
                up.executeUpdate();
            }
            conn.commit();
            return next;
        }
    }

    public List<Account> top10() throws SQLException {
        List<Account> list = new ArrayList<>();
        try (Connection conn = database.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT uuid,name,balance FROM accounts ORDER BY balance DESC LIMIT 10"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new Account(UUID.fromString(rs.getString("uuid")), rs.getString("name"), rs.getLong("balance")));
            }
        }
        return list;
    }
}
