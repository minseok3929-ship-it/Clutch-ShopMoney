package com.clutch.shopmoney.api;

import java.util.UUID;

public interface MoneyAPI {
    long getBalance(UUID uuid);
}
