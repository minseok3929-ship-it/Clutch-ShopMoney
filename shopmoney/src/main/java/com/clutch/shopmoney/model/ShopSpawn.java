package com.clutch.shopmoney.model;

import java.util.UUID;

public record ShopSpawn(String shopId, UUID entityUuid, String world, double x, double y, double z, float yaw, float pitch) {
}
