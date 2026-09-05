package com.ailudick.capitalismmod.menu;

import com.ailudick.capitalismmod.init.ModMenuTypes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class LandMenu extends AbstractContainerMenu {
    public boolean claimed;
    public String id = "";
    public String dimension = "";
    public int chunkX;
    public int chunkZ;
    public String ownerUuid = "";
    public String purpose = "";
    public String linkedBusinessId = "";
    public String resourceType = "";
    public long resourceAmount;
    public long taxOwed;
    public long taxDueAt;
    public long taxGraceUntil;
    public boolean trusted;
    public boolean memberBuild = true;
    public boolean memberInteract = true;
    public boolean memberContainer;
    public boolean memberRedstone;
    public boolean saleActive;
    public String saleTarget = "";
    public long salePrice;
    public long saleExpiresAt;
    public boolean auctionActive;
    public long auctionStartPrice;
    public long auctionHighestBid;
    public String auctionBidder = "";
    public long auctionEndsAt;
    public boolean leased;
    public String leaseeUuid = "";
    public long leaseUntil;
    public long leaseRent;
    public long leaseDebt;
    public long leaseGraceUntil;
    public List<com.ailudick.capitalismmod.network.payload.SyncLandPayload.MapCell> mapCells = List.of();
    public int selectedChunkX;
    public int selectedChunkZ;
    public boolean hasSelectedChunk;
    public List<String> trustedPlayerNames = List.of();
    public List<com.ailudick.capitalismmod.network.payload.SyncOwnedLandsPayload.LandEntry> ownedLands = List.of();
    public List<String> landLogs = List.of();
    public List<String> ownershipHistory = List.of();
    private final Map<Long, int[]> mapTiles = new HashMap<>();

    public LandMenu(int containerId, Inventory inventory) {
        this(ModMenuTypes.LAND_MENU.get(), containerId);
    }

    protected LandMenu(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    public void setData(com.ailudick.capitalismmod.network.payload.SyncLandPayload data) {
        claimed = data.claimed(); id = data.id(); dimension = data.dimension(); chunkX = data.chunkX(); chunkZ = data.chunkZ();
        ownerUuid = data.ownerUuid(); purpose = data.purpose(); linkedBusinessId = data.linkedBusinessId();
        resourceType = data.resourceType(); resourceAmount = data.resourceAmount(); taxOwed = data.taxOwed();
        trusted = data.trusted(); leased = data.leased(); leaseeUuid = data.leaseeUuid();
        leaseUntil = data.leaseUntil(); leaseRent = data.leaseRent();
        leaseDebt = data.leaseDebt(); leaseGraceUntil = data.leaseGraceUntil();
        taxDueAt = data.taxDueAt(); taxGraceUntil = data.taxGraceUntil();
        mapCells = data.mapCells();
    }

    public void setTrustedPlayers(List<String> players) {
        trustedPlayerNames = List.copyOf(players);
    }

    public void setOwnedLands(List<com.ailudick.capitalismmod.network.payload.SyncOwnedLandsPayload.LandEntry> lands) {
        ownedLands = List.copyOf(lands);
    }

    public void setLandLogs(List<String> logs) { landLogs = List.copyOf(logs); }
    public void setOwnershipHistory(List<String> owners) { ownershipHistory = List.copyOf(owners); }

    public void setTiles(com.ailudick.capitalismmod.network.payload.SyncWorldMapTilesPayload data) {
        for (var tile : data.tiles()) mapTiles.put(key(tile.chunkX(), tile.chunkZ()), tile.colors());
    }

    public int[] tile(int chunkX, int chunkZ) { return mapTiles.get(key(chunkX, chunkZ)); }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public boolean stillValid(Player player) { return true; }
}
