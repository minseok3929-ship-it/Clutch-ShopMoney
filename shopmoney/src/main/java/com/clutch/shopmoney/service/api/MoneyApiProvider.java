package com.clutch.shopmoney.service.api;

import com.clutch.shopmoney.api.MoneyAPI;
import com.clutch.shopmoney.service.MoneyService;

import java.sql.SQLException;
import java.util.UUID;

public class MoneyApiProvider implements MoneyAPI {
    private final MoneyService moneyService;

    public MoneyApiProvider(MoneyService moneyService) {
        this.moneyService = moneyService;
    }

    @Override
    public long getBalance(UUID uuid) {
        try {
            return moneyService.balance(uuid);
        } catch (SQLException e) {
            return 0L;
        }
    }
}
