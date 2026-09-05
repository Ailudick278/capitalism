package com.ailudick.capitalismmod.land;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.business.IndividualBusinessHelper;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.tax.TaxService;
import com.ailudick.capitalismmod.tax.TaxSubject;
import com.ailudick.capitalismmod.tax.TaxType;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.List;
import java.util.UUID;

public final class LandHelper {
    private LandHelper() {}

    public static String id(ServerPlayer player, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        return player.level().dimension().location() + ":" + chunk.x + ":" + chunk.z;
    }

    public static LandClaim at(ServerPlayer player, BlockPos pos) {
        return LandSavedData.get(player.getServer()).get(id(player, pos));
    }

    public static boolean canBuild(ServerPlayer player, BlockPos pos) {
        if (isLandAdmin(player)) return true;
        LandClaim claim = at(player, pos);
        if (claim != null && isTaxFrozen(player, claim)) return false;
        if (claim != null && claim.leaseeUuid() != null
                && player.level().getGameTime() >= claim.leaseUntil()) {
            LandSavedData.get(player.getServer()).put(claim.clearLease());
            claim = claim.clearLease();
        }
        return claim == null || claim.ownerUuid().equals(player.getUUID())
                || (claim.trusts(player.getUUID())
                && LandPermissionSavedData.get(player.getServer()).canBuild(claim.id()));
    }

    public static boolean canInteract(ServerPlayer player, BlockPos pos) {
        if (isLandAdmin(player)) return true;
        LandClaim claim = at(player, pos);
        if (claim != null && isTaxFrozen(player, claim)) return false;
        return claim == null || claim.ownerUuid().equals(player.getUUID())
                || (claim.trusts(player.getUUID())
                && LandPermissionSavedData.get(player.getServer()).canInteract(claim.id()));
    }

    public static boolean canContainer(ServerPlayer player, BlockPos pos) {
        if (isLandAdmin(player)) return true;
        LandClaim claim = at(player, pos);
        if (claim != null && isTaxFrozen(player, claim)) return false;
        return claim == null || claim.ownerUuid().equals(player.getUUID())
                || (claim.trusts(player.getUUID()) && LandPermissionSavedData.get(player.getServer()).canContainer(claim.id()));
    }

    public static boolean canRedstone(ServerPlayer player, BlockPos pos) {
        if (isLandAdmin(player)) return true;
        LandClaim claim = at(player, pos);
        if (claim != null && isTaxFrozen(player, claim)) return false;
        return claim == null || claim.ownerUuid().equals(player.getUUID())
                || (claim.trusts(player.getUUID()) && LandPermissionSavedData.get(player.getServer()).canRedstone(claim.id()));
    }

    private static boolean isLandAdmin(ServerPlayer player) {
        return Config.LAND_ADMIN_BYPASS.get() && player.hasPermissions(2);
    }

    public static boolean isTaxFrozen(ServerPlayer player, LandClaim claim) {
        return statusAt(player, claim) == LandStatus.TAX_FROZEN
                || statusAt(player, claim) == LandStatus.AUCTION;
    }

    public static LandStatus statusAt(ServerPlayer player, LandClaim claim) {
        boolean auction = LandAuctionSavedData.get(player.getServer()).get(claim.id()) != null;
        return LandStatus.resolve(claim.taxOwed(), claim.taxDueAt(), claim.taxGraceUntil(), auction,
                player.level().getGameTime());
    }

    public static LandClaim.Role roleAt(ServerPlayer player, BlockPos pos) {
        LandClaim claim = at(player, pos);
        return claim == null ? null : claim.roleOf(player.getUUID());
    }

    public static boolean claim(ServerPlayer player) {
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        return claim(player, chunk.x, chunk.z);
    }

    public static boolean claim(ServerPlayer player, int chunkX, int chunkZ) {
        String dimension = player.level().dimension().location().toString();
        String id = dimension + ":" + chunkX + ":" + chunkZ;
        if (LandSavedData.get(player.getServer()).get(id) != null) return false;
        long owned = LandSavedData.get(player.getServer()).claims().values().stream()
                .filter(claim -> claim.ownerUuid().equals(player.getUUID())
                        && claim.dimension().equals(dimension))
                .count();
        if (owned >= Config.MAX_LAND_CLAIMS.get()) return false;
        if (Config.REQUIRE_ADJACENT_LAND_CLAIMS.get() && owned > 0
                && !hasAdjacentClaim(player, chunkX, chunkZ)) return false;
        long price = Config.LAND_CLAIM_PRICE.get();
        if (!EconomyHelper.tryPay(player, Config.defaultCurrency(), price)) return false;
        long seed = Math.abs(((long) chunkX * 341873128712L) ^ ((long) chunkZ * 132897987541L));
        String[] resources = {"coal", "iron", "copper", "wheat", "wood"};
        LandClaim claim = new LandClaim(id, dimension, chunkX, chunkZ,
                player.getUUID(), "2301", "", List.of(), resources[(int) (seed % resources.length)],
                100 + seed % 901, 0L, null, 0L, 0L, 0L, 0L, 0L, 0L);
        LandSavedData.get(player.getServer()).put(claim);
        LandOwnershipSavedData.get(player.getServer()).record(claim.id(), player.getUUID(),
                player.level().getGameTime(), "初次认领");
        LandOperationLogSavedData.get(player.getServer()).record(player.level().getGameTime(), player.getUUID(),
                "认领土地", dimension, chunkX, chunkZ);
        return true;
    }

    public static boolean hasAdjacentClaim(ServerPlayer player, int chunkX, int chunkZ) {
        String dimension = player.level().dimension().location().toString();
        var claims = LandSavedData.get(player.getServer()).claims();
        return isOwnedBy(claims, dimension, chunkX - 1, chunkZ, player.getUUID())
                || isOwnedBy(claims, dimension, chunkX + 1, chunkZ, player.getUUID())
                || isOwnedBy(claims, dimension, chunkX, chunkZ - 1, player.getUUID())
                || isOwnedBy(claims, dimension, chunkX, chunkZ + 1, player.getUUID());
    }

    private static boolean isOwnedBy(java.util.Map<String, LandClaim> claims, String dimension,
                                     int chunkX, int chunkZ, UUID owner) {
        LandClaim claim = claims.get(dimension + ":" + chunkX + ":" + chunkZ);
        return claim != null && claim.ownerUuid().equals(owner);
    }

    public static boolean release(ServerPlayer player) {
        ChunkPos chunk = new ChunkPos(player.blockPosition());
        return release(player, chunk.x, chunk.z);
    }

    public static boolean release(ServerPlayer player, int chunkX, int chunkZ) {
        String id = player.level().dimension().location() + ":" + chunkX + ":" + chunkZ;
        LandClaim claim = LandSavedData.get(player.getServer()).get(id);
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || isTaxFrozen(player, claim)) return false;
        LandSavedData.get(player.getServer()).remove(id);
        LandPermissionSavedData.get(player.getServer()).remove(id);
        long refund = Math.round(Config.LAND_CLAIM_PRICE.get() * Config.LAND_RELEASE_REFUND_RATE.get());
        EconomyHelper.giveMoney(player, Config.defaultCurrency(), refund);
        LandOperationLogSavedData.get(player.getServer()).record(player.level().getGameTime(), player.getUUID(),
                "释放土地", claim.dimension(), chunkX, chunkZ);
        return true;
    }

    public static boolean setPurpose(ServerPlayer player, int chunkX, int chunkZ, String purpose) {
        if (!validPurpose(purpose)) return false;
        String id = player.level().dimension().location() + ":" + chunkX + ":" + chunkZ;
        LandClaim claim = LandSavedData.get(player.getServer()).get(id);
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || isTaxFrozen(player, claim)) return false;
        LandSavedData.get(player.getServer()).put(claim.withPurpose(purpose));
        LandOperationLogSavedData.get(player.getServer()).record(player.level().getGameTime(), player.getUUID(),
                "修改用途:" + purpose, claim.dimension(), chunkX, chunkZ);
        return true;
    }

    public static boolean manageTrust(ServerPlayer player, int chunkX, int chunkZ,
                                      UUID targetUuid, boolean add) {
        String id = player.level().dimension().location() + ":" + chunkX + ":" + chunkZ;
        LandClaim claim = LandSavedData.get(player.getServer()).get(id);
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || targetUuid.equals(player.getUUID())
                || isTaxFrozen(player, claim)) return false;
        LandSavedData.get(player.getServer()).put(add
                ? claim.addTrusted(targetUuid) : claim.removeTrusted(targetUuid));
        LandOperationLogSavedData.get(player.getServer()).record(player.level().getGameTime(), player.getUUID(),
                add ? "添加信任" : "移除信任", claim.dimension(), chunkX, chunkZ);
        return true;
    }

    public static boolean lease(ServerPlayer player, int chunkX, int chunkZ,
                                UUID targetUuid, long days, long rent) {
        if (days <= 0 || rent < 0 || targetUuid.equals(player.getUUID())) return false;
        String id = player.level().dimension().location() + ":" + chunkX + ":" + chunkZ;
        LandClaim claim = LandSavedData.get(player.getServer()).get(id);
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || isTaxFrozen(player, claim)) return false;
        long until = player.level().getGameTime() + days * 24000L;
        LandSavedData.get(player.getServer()).put(claim.withLease(targetUuid, until, rent));
        LandOperationLogSavedData.get(player.getServer()).record(player.level().getGameTime(), player.getUUID(),
                "创建租约", claim.dimension(), chunkX, chunkZ);
        return true;
    }

    public static boolean unlease(ServerPlayer player, int chunkX, int chunkZ) {
        String id = player.level().dimension().location() + ":" + chunkX + ":" + chunkZ;
        LandClaim claim = LandSavedData.get(player.getServer()).get(id);
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || claim.leaseeUuid() == null
                || isTaxFrozen(player, claim)) return false;
        LandSavedData.get(player.getServer()).put(claim.clearLease());
        LandOperationLogSavedData.get(player.getServer()).record(player.level().getGameTime(), player.getUUID(),
                "解除租约", claim.dimension(), chunkX, chunkZ);
        return true;
    }

    public static boolean bindBusiness(ServerPlayer player) {
        LandClaim claim = at(player, player.blockPosition());
        var business = IndividualBusinessHelper.get(player);
        if (claim == null || business == null || !claim.ownerUuid().equals(player.getUUID())
                || isTaxFrozen(player, claim)) return false;
        LandSavedData.get(player.getServer()).put(claim.withLink(business.businessId()));
        return true;
    }

    public static boolean validPurpose(String purpose) {
        return LandPurpose.exists(purpose) || List.of("residential", "agriculture", "industrial", "commercial", "warehouse", "mining", "public").contains(purpose);
    }
    public static List<String> purposes() { return LandPurpose.codes(); }

    public static boolean extract(ServerPlayer player, long amount) {
        LandClaim claim = at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || isTaxFrozen(player, claim)
                || amount <= 0 || claim.resourceAmount() < amount
                || !(claim.purpose().startsWith("01") || claim.purpose().startsWith("02")
                || claim.purpose().startsWith("03") || claim.purpose().startsWith("04")
                || claim.purpose().startsWith("06") || "1002".equals(claim.purpose())
                || "mining".equals(claim.purpose()) || "agriculture".equals(claim.purpose()))) return false;
        LandSavedData.get(player.getServer()).put(claim.withResource(claim.resourceAmount() - amount));
        return true;
    }

    public static boolean payTax(ServerPlayer player, long amount) {
        LandClaim claim = at(player, player.blockPosition());
        if (claim == null || !claim.ownerUuid().equals(player.getUUID()) || amount <= 0
                || claim.taxOwed() < amount) return false;
        TaxSubject subject = new TaxSubject(TaxType.LAND, claim.id(), claim.ownerUuid());
        TaxService.ensureOutstanding(player.getServer(), subject, Config.defaultCurrencyId(), Money.toMinorSaturated(claim.taxOwed()),
                player.level().getGameTime(), claim.taxDueAt(), claim.taxGraceUntil());
        if (!TaxService.pay(player, subject, Money.toMinorSaturated(amount))) return false;
        long remaining = TaxService.outstanding(player.getServer(), subject);
        LandSavedData.get(player.getServer()).put(remaining == 0L
                ? claim.withTaxSchedule(0L, 0L, 0L)
                : claim.withTaxSchedule(Money.toMajorCeiling(remaining), claim.taxDueAt(), claim.taxGraceUntil()));
        return true;
    }
}
