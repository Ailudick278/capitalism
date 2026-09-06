package com.ailudick.capitalismmod.economy;

import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.market.MarketMailboxSavedData;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Executes and recovers player wallet transfers across a server interruption. */
public final class PlayerTransferService {
    private PlayerTransferService() {}
    public static boolean transfer(ServerPlayer sender, ServerPlayer target, Currency currency, long amount) {
        PlayerTransferIntentSavedData data=PlayerTransferIntentSavedData.get(sender.getServer());
        PlayerTransferIntentSavedData.Intent intent=data.findPending(sender.getUUID(),target.getUUID(),currency.id(),amount);
        boolean created=false;
        if(intent==null){intent=new PlayerTransferIntentSavedData.Intent(UUID.randomUUID().toString(),sender.getUUID(),target.getUUID(),currency.id(),amount,false);data.add(intent);created=true;}
        if(!EconomyHelper.tryPayWithReference(sender,currency,amount,"player-pay-payment:"+intent.id())){if(created)data.remove(intent.id());return false;}
        data.markPaid(intent.id());
        deliver(sender.getServer(),data,data.find(intent.id()));
        return true;
    }
    public static int recover(MinecraftServer server){
        PlayerTransferIntentSavedData data=PlayerTransferIntentSavedData.get(server);int recovered=0;
        for(PlayerTransferIntentSavedData.Intent intent:data.intents()){
            if(!intent.paid()){
                ServerPlayer sender=server.getPlayerList().getPlayer(intent.sender());
                if(sender==null||!Currencies.exists(intent.currencyId()))continue;
                if(!EconomyHelper.tryPayWithReference(sender,Currencies.byId(intent.currencyId()),intent.amount(),"player-pay-payment:"+intent.id()))continue;
                data.markPaid(intent.id());
            }
            if(deliver(server,data,data.find(intent.id())))recovered++;
        }
        return recovered;
    }
    private static boolean deliver(MinecraftServer server,PlayerTransferIntentSavedData data,PlayerTransferIntentSavedData.Intent intent){
        if(intent==null||!intent.paid()||!Currencies.exists(intent.currencyId()))return false;
        MarketMailboxSavedData mailbox=MarketMailboxSavedData.get(server);String source="player-pay-delivery:"+intent.id();
        if(!mailbox.hasTransferSource(source))mailbox.creditTransferOnce(intent.target(),intent.currencyId(),intent.amount(),source);
        ServerPlayer target=server.getPlayerList().getPlayer(intent.target());if(target!=null)mailbox.redeemTransferOnly(target);
        data.remove(intent.id());return true;
    }
}
