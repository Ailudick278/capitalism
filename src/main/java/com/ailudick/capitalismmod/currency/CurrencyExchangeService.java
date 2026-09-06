package com.ailudick.capitalismmod.currency;

import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Crash-safe execution and recovery for currency conversion. */
public final class CurrencyExchangeService {
    private CurrencyExchangeService() {}

    public static boolean exchange(ServerPlayer player, Currency from, Currency to, long amount, long converted) {
        String id = UUID.randomUUID().toString();
        CurrencyExchangeIntentSavedData data = CurrencyExchangeIntentSavedData.get(player.getServer());
        data.add(new CurrencyExchangeIntentSavedData.Intent(id, player.getUUID(), from.id(), to.id(), amount, converted, false));
        if (!EconomyHelper.tryPayWithReference(player, from, amount, "currency-exchange-payment:" + id)) {
            data.remove(id);
            return false;
        }
        data.markPaid(id);
        deliver(player.getServer(), data, data.find(id));
        return true;
    }

    public static int recover(MinecraftServer server) {
        CurrencyExchangeIntentSavedData data = CurrencyExchangeIntentSavedData.get(server);
        int recovered = 0;
        for (CurrencyExchangeIntentSavedData.Intent intent : data.intents()) {
            ServerPlayer player = server.getPlayerList().getPlayer(intent.player());
            if (player == null) continue;
            if (!intent.paid()) {
                if (!Currencies.exists(intent.from()) || !Currencies.exists(intent.to())) continue;
                Currency from = Currencies.byId(intent.from());
                if (!EconomyHelper.tryPayWithReference(player, from, intent.amount(), "currency-exchange-payment:" + intent.id())) continue;
                data.markPaid(intent.id());
            }
            if (deliver(server, data, data.find(intent.id()))) recovered++;
        }
        return recovered;
    }

    private static boolean deliver(MinecraftServer server, CurrencyExchangeIntentSavedData data,
                                   CurrencyExchangeIntentSavedData.Intent intent) {
        if (intent == null || !intent.paid()) return false;
        if (!Currencies.exists(intent.to())) return false;
        MarketMailboxSavedData mailbox = MarketMailboxSavedData.get(server);
        String source = "currency-exchange-result:" + intent.id();
        if (!mailbox.hasTransferSource(source)) mailbox.creditTransferOnce(intent.player(), intent.to(), intent.converted(), source);
        ServerPlayer player = server.getPlayerList().getPlayer(intent.player());
        if (player != null) mailbox.redeemTransferOnly(player);
        data.remove(intent.id());
        return true;
    }
}
