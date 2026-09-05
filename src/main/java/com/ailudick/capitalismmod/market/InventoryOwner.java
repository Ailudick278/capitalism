package com.ailudick.capitalismmod.market;

import java.util.UUID;

/** Identifies the economic entity that owns persistent inventory. */
public record InventoryOwner(Type type, String id) {
    public enum Type { PLAYER, COMPANY }

    public InventoryOwner {
        if (type == null || id == null || id.isBlank()) {
            throw new IllegalArgumentException("Inventory owner must have a type and id");
        }
    }

    public static InventoryOwner player(UUID id) {
        return new InventoryOwner(Type.PLAYER, id.toString());
    }

    public static InventoryOwner company(String companyId) {
        return new InventoryOwner(Type.COMPANY, companyId);
    }

    public String storageKey() {
        return type.name().toLowerCase() + ":" + id;
    }

    public static InventoryOwner parse(String key) {
        if (key == null) return null;
        int separator = key.indexOf(':');
        if (separator <= 0 || separator == key.length() - 1) return null;
        try {
            Type type = Type.valueOf(key.substring(0, separator).toUpperCase());
            return new InventoryOwner(type, key.substring(separator + 1));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
