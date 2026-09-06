package com.ailudick.capitalismmod.economy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable intent for a wallet-to-wallet player transfer. */
public final class PlayerTransferIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_player_transfer_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();
    public record Intent(String id, UUID sender, UUID target, String currencyId, long amount, boolean paid) {
        public Intent { id=id==null?"":id; currencyId=currencyId==null?"":currencyId; amount=Math.max(0,amount); }
        public Intent withPaid(boolean value) { return new Intent(id,sender,target,currencyId,amount,value); }
    }
    private PlayerTransferIntentSavedData() {}
    public static PlayerTransferIntentSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(PlayerTransferIntentSavedData::new, PlayerTransferIntentSavedData::load), ID); }
    public List<Intent> intents() { return List.copyOf(intents); }
    public Intent find(String id) { return intents.stream().filter(i->id!=null&&id.equals(i.id())).findFirst().orElse(null); }
    public Intent findPending(UUID sender, UUID target, String currencyId, long amount) { return intents.stream().filter(i->!i.paid()&&sender.equals(i.sender())&&target.equals(i.target())&&currencyId.equals(i.currencyId())&&amount==i.amount()).findFirst().orElse(null); }
    public void add(Intent i) { if(i==null||i.id().isBlank()||i.sender()==null||i.target()==null||i.currencyId().isBlank()||i.amount()<=0||find(i.id())!=null)return;intents.add(i);while(intents.size()>MAX_INTENTS)intents.remove(0);setDirty(); }
    public void markPaid(String id) { Intent i=find(id);if(i!=null&&!i.paid()){intents.set(intents.indexOf(i),i.withPaid(true));setDirty();} }
    public void remove(String id) { if(id!=null&&intents.removeIf(i->id.equals(i.id())))setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list=new ListTag();for(Intent i:intents){CompoundTag e=new CompoundTag();e.putString("id",i.id());e.putUUID("sender",i.sender());e.putUUID("target",i.target());e.putString("currency",i.currencyId());e.putLong("amount",i.amount());e.putBoolean("paid",i.paid());list.add(e);}tag.put("intents",list);return tag; }
    public static PlayerTransferIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) { PlayerTransferIntentSavedData d=new PlayerTransferIntentSavedData();ListTag l=tag.getList("intents",Tag.TAG_COMPOUND);for(int n=Math.max(0,l.size()-MAX_INTENTS);n<l.size();n++){CompoundTag e=l.getCompound(n);if(!e.hasUUID("sender")||!e.hasUUID("target")||e.getString("id").isBlank()||e.getString("currency").isBlank()||e.getLong("amount")<=0)continue;d.intents.add(new Intent(e.getString("id"),e.getUUID("sender"),e.getUUID("target"),e.getString("currency"),e.getLong("amount"),e.getBoolean("paid")));}return d; }
}
