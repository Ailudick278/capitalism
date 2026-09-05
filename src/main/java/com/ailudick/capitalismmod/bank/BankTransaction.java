package com.ailudick.capitalismmod.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.entity.player.Player;

/**
 * A single entry in a bank account's transaction history.
 *
 * @param type       one of "deposit", "withdraw", "loan", "repay", "interest", "transfer_out", "transfer_fee", "transfer_in"
 * @param currencyId currency the transaction is denominated in
 * @param amount     signed amount (positive = into the account / earned, negative = out / owed)
 * @param occurredAt world tick when the transaction was recorded; -1 means legacy data
 * @param reference  stable human-readable source/reference, when available
 * @param counterparty account, wallet, or institution on the other side, when known
 */
public record BankTransaction(String type, String currencyId, long amount, long occurredAt,
                              String reference, String counterparty) {

    public BankTransaction(String type, String currencyId, long amount) {
        this(type, currencyId, amount, -1L, "legacy", "unknown");
    }

    public BankTransaction {
        reference = reference == null ? "" : reference;
        counterparty = counterparty == null ? "unknown" : counterparty;
    }

    public static BankTransaction now(Player player, String type, String currencyId, long amount) {
        return now(player, type, currencyId, amount, type, "bank");
    }

    public static BankTransaction now(Player player, String type, String currencyId, long amount,
                                      String reference, String counterparty) {
        long tick = player == null || player.level() == null ? -1L : player.level().getGameTime();
        return new BankTransaction(type, currencyId, amount, tick, reference, counterparty);
    }

    public static Codec<BankTransaction> codec() { return Codecs.CODEC; }

    private static final class Codecs {
        private static final Codec<BankTransaction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(BankTransaction::type),
            Codec.STRING.fieldOf("currencyId").forGetter(BankTransaction::currencyId),
            Codec.LONG.fieldOf("amount").forGetter(BankTransaction::amount),
            Codec.LONG.optionalFieldOf("occurredAt", -1L).forGetter(BankTransaction::occurredAt),
            Codec.STRING.optionalFieldOf("reference", "legacy").forGetter(BankTransaction::reference),
            Codec.STRING.optionalFieldOf("counterparty", "unknown").forGetter(BankTransaction::counterparty)
        ).apply(instance, BankTransaction::new));
    }
}
