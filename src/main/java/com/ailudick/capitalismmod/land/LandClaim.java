package com.ailudick.capitalismmod.land;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;
import java.util.UUID;

/** Persistent ownership and resource data for one claimed chunk. */
public record LandClaim(String id, String dimension, int chunkX, int chunkZ, UUID ownerUuid,
                        String purpose, String linkedBusinessId, List<UUID> trustedPlayers,
                        String resourceType, long resourceAmount, long taxOwed,
                        UUID leaseeUuid, long leaseUntil, long leaseRent,
                        long leaseDebt, long leaseGraceUntil, long taxDueAt, long taxGraceUntil) {
    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);

    public enum Role {
        OWNER,
        MEMBER,
        VISITOR
    }
    public static final Codec<LandClaim> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(LandClaim::id),
            Codec.STRING.fieldOf("dimension").forGetter(LandClaim::dimension),
            Codec.INT.fieldOf("chunkX").forGetter(LandClaim::chunkX),
            Codec.INT.fieldOf("chunkZ").forGetter(LandClaim::chunkZ),
            UUID_CODEC.fieldOf("ownerUuid").forGetter(LandClaim::ownerUuid),
            Codec.STRING.fieldOf("purpose").forGetter(LandClaim::purpose),
            Codec.STRING.fieldOf("linkedBusinessId").forGetter(LandClaim::linkedBusinessId),
            UUID_CODEC.listOf().fieldOf("trustedPlayers").forGetter(LandClaim::trustedPlayers),
            Codec.STRING.fieldOf("resourceType").forGetter(LandClaim::resourceType),
            Codec.LONG.fieldOf("resourceAmount").forGetter(LandClaim::resourceAmount),
            Codec.LONG.fieldOf("taxOwed").forGetter(LandClaim::taxOwed),
            UUID_CODEC.optionalFieldOf("leaseeUuid").forGetter(claim -> java.util.Optional.ofNullable(claim.leaseeUuid())),
            Codec.LONG.fieldOf("leaseUntil").forGetter(LandClaim::leaseUntil),
            Codec.LONG.fieldOf("leaseRent").forGetter(LandClaim::leaseRent),
            Codec.LONG.listOf().optionalFieldOf("leaseState", List.of(0L, 0L))
                    .forGetter(claim -> List.of(claim.leaseDebt(), claim.leaseGraceUntil())),
            Codec.LONG.listOf().optionalFieldOf("taxSchedule", List.of(0L, 0L))
                    .forGetter(claim -> List.of(claim.taxDueAt(), claim.taxGraceUntil()))
    ).apply(instance, (id, dimension, x, z, owner, purpose, linked, trusted, resource, amount, tax, leasee, until, rent, leaseState, taxSchedule) ->
            new LandClaim(id, dimension, x, z, owner, purpose, linked, trusted, resource, amount, tax,
                    leasee.orElse(null), until, rent, valueAt(leaseState, 0), valueAt(leaseState, 1),
                    valueAt(taxSchedule, 0), valueAt(taxSchedule, 1))));

    private static long valueAt(List<Long> values, int index) {
        return values.size() > index ? values.get(index) : 0L;
    }

    public boolean trusts(UUID playerUuid) {
        return ownerUuid.equals(playerUuid) || trustedPlayers.contains(playerUuid) ||
                (leaseeUuid != null && leaseeUuid.equals(playerUuid));
    }

    /** Resolves the player's effective role on this claim. The owner always has priority. */
    public Role roleOf(UUID playerUuid) {
        if (ownerUuid.equals(playerUuid)) return Role.OWNER;
        if (trustedPlayers.contains(playerUuid) || (leaseeUuid != null && leaseeUuid.equals(playerUuid))) {
            return Role.MEMBER;
        }
        return Role.VISITOR;
    }

    public LandClaim withPurpose(String value) {
        return new LandClaim(id, dimension, chunkX, chunkZ, ownerUuid, value, linkedBusinessId, trustedPlayers,
                resourceType, resourceAmount, taxOwed, leaseeUuid, leaseUntil, leaseRent, leaseDebt, leaseGraceUntil, taxDueAt, taxGraceUntil);
    }

    public LandClaim withOwner(UUID newOwner) {
        return new LandClaim(id, dimension, chunkX, chunkZ, newOwner, purpose, linkedBusinessId, List.of(),
                resourceType, resourceAmount, taxOwed, null, 0L, 0L, 0L, 0L, 0L, 0L);
    }

    public LandClaim withLink(String value) {
        return new LandClaim(id, dimension, chunkX, chunkZ, ownerUuid, purpose, value, trustedPlayers,
                resourceType, resourceAmount, taxOwed, leaseeUuid, leaseUntil, leaseRent, leaseDebt, leaseGraceUntil, taxDueAt, taxGraceUntil);
    }

    public LandClaim withResource(long amount) {
        return new LandClaim(id, dimension, chunkX, chunkZ, ownerUuid, purpose, linkedBusinessId, trustedPlayers,
                resourceType, Math.max(0L, amount), taxOwed, leaseeUuid, leaseUntil, leaseRent, leaseDebt, leaseGraceUntil, taxDueAt, taxGraceUntil);
    }

    public LandClaim withTax(long amount) {
        return new LandClaim(id, dimension, chunkX, chunkZ, ownerUuid, purpose, linkedBusinessId, trustedPlayers,
                resourceType, resourceAmount, Math.max(0L, amount), leaseeUuid, leaseUntil, leaseRent, leaseDebt, leaseGraceUntil, taxDueAt, taxGraceUntil);
    }

    public LandClaim withTaxSchedule(long amount, long dueAt, long graceUntil) {
        return new LandClaim(id, dimension, chunkX, chunkZ, ownerUuid, purpose, linkedBusinessId, trustedPlayers,
                resourceType, resourceAmount, Math.max(0L, amount), leaseeUuid, leaseUntil, leaseRent, leaseDebt,
                leaseGraceUntil, dueAt, graceUntil);
    }

    public LandClaim withLease(UUID player, long until, long rent) {
        return withLeaseState(player, until, rent, 0L, 0L);
    }

    public LandClaim withLeaseState(UUID player, long until, long rent,
                                    long debt, long graceUntil) {
        return new LandClaim(id, dimension, chunkX, chunkZ, ownerUuid, purpose, linkedBusinessId, trustedPlayers,
                resourceType, resourceAmount, taxOwed, player, until, rent, debt, graceUntil, taxDueAt, taxGraceUntil);
    }

    public LandClaim clearLease() {
        return withLeaseState(null, 0L, 0L, 0L, 0L);
    }

    public LandClaim addTrusted(UUID playerUuid) {
        java.util.ArrayList<UUID> players = new java.util.ArrayList<>(trustedPlayers);
        if (!players.contains(playerUuid)) players.add(playerUuid);
        return new LandClaim(id, dimension, chunkX, chunkZ, ownerUuid, purpose, linkedBusinessId, players,
                resourceType, resourceAmount, taxOwed, leaseeUuid, leaseUntil, leaseRent, leaseDebt, leaseGraceUntil, taxDueAt, taxGraceUntil);
    }

    public LandClaim removeTrusted(UUID playerUuid) {
        java.util.ArrayList<UUID> players = new java.util.ArrayList<>(trustedPlayers);
        players.remove(playerUuid);
        return new LandClaim(id, dimension, chunkX, chunkZ, ownerUuid, purpose, linkedBusinessId, players,
                resourceType, resourceAmount, taxOwed, leaseeUuid, leaseUntil, leaseRent, leaseDebt, leaseGraceUntil, taxDueAt, taxGraceUntil);
    }
}
