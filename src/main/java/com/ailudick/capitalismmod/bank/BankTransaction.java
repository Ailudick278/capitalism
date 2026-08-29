package com.ailudick.capitalismmod.bank;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A single entry in a bank account's transaction history.
 *
 * @param type       one of "deposit", "withdraw", "loan", "repay", "interest"
 * @param currencyId currency the transaction is denominated in
 * @param amount     signed amount (positive = into the account / earned, negative = out / owed)
 */
public record BankTransaction(String type, String currencyId, long amount) {

    public static final Codec<BankTransaction> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("type").forGetter(BankTransaction::type),
            Codec.STRING.fieldOf("currencyId").forGetter(BankTransaction::currencyId),
            Codec.LONG.fieldOf("amount").forGetter(BankTransaction::amount)
    ).apply(instance, BankTransaction::new));
}
