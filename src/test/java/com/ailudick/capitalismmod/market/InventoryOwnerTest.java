package com.ailudick.capitalismmod.market;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InventoryOwnerTest {
    @Test
    void playerAndCompanyUseDifferentStableKeys() {
        UUID playerId = UUID.randomUUID();
        assertEquals("player:" + playerId, InventoryOwner.player(playerId).storageKey());
        assertEquals("company:company-1", InventoryOwner.company("company-1").storageKey());
        assertNotEquals(InventoryOwner.player(playerId), InventoryOwner.company(playerId.toString()));
    }

    @Test
    void ownerKeysRoundTrip() {
        InventoryOwner player = InventoryOwner.player(UUID.randomUUID());
        InventoryOwner company = InventoryOwner.company("abc123");
        assertEquals(player, InventoryOwner.parse(player.storageKey()));
        assertEquals(company, InventoryOwner.parse(company.storageKey()));
        assertNull(InventoryOwner.parse("legacy-key"));
    }
}
