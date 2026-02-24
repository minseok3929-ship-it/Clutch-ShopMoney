package com.clutch.shopmoney.model;

import java.util.UUID;

public record Account(UUID uuid, String lastKnownName, long balance) {
}
