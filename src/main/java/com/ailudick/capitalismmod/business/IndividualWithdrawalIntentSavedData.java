package com.ailudick.capitalismmod.business;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable intent for an individual-business withdrawal. */
public final class IndividualWithdrawalIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_individual_withdrawal_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String id, UUID player, String businessId, String currencyId,
                         long balanceBefore, long amount, boolean accountApplied) {
        public Intent { id=id==null?"":id; businessId=businessId==null?"":businessId; currencyId=currencyId==null?"":currencyId; balanceBefore=Math.max(0,balanceBefore); amount=Math.max(0,amount); }
        public Intent withAccountApplied(boolean value) { return new Intent(id,player,businessId,currencyId,balanceBefore,amount,value); }
    }
    private IndividualWithdrawalIntentSavedData() {}
    public static IndividualWithdrawalIntentSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(IndividualWithdrawalIntentSavedData::new, IndividualWithdrawalIntentSavedData::load), ID); }
    public List<Intent> intents() { return List.copyOf(intents); }
    public Intent find(String id) { return intents.stream().filter(i->id!=null&&id.equals(i.id())).findFirst().orElse(null); }
    public void add(Intent i) { if(i==null||i.id().isBlank()||i.player()==null||i.businessId().isBlank()||i.currencyId().isBlank()||i.amount()<=0||i.amount()>i.balanceBefore()||find(i.id())!=null)return;intents.add(i);while(intents.size()>MAX_INTENTS)intents.remove(0);setDirty(); }
    public void markAccountApplied(String id) { Intent i=find(id);if(i!=null&&!i.accountApplied()){intents.set(intents.indexOf(i),i.withAccountApplied(true));setDirty();} }
    public void remove(String id) { if(id!=null&&intents.removeIf(i->id.equals(i.id())))setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list=new ListTag();for(Intent i:intents){CompoundTag e=new CompoundTag();e.putString("id",i.id());e.putUUID("player",i.player());e.putString("business",i.businessId());e.putString("currency",i.currencyId());e.putLong("balanceBefore",i.balanceBefore());e.putLong("amount",i.amount());e.putBoolean("accountApplied",i.accountApplied());list.add(e);}tag.put("intents",list);return tag; }
    public static IndividualWithdrawalIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) { IndividualWithdrawalIntentSavedData d=new IndividualWithdrawalIntentSavedData();ListTag l=tag.getList("intents",Tag.TAG_COMPOUND);for(int n=Math.max(0,l.size()-MAX_INTENTS);n<l.size();n++){CompoundTag e=l.getCompound(n);if(!e.hasUUID("player")||e.getString("id").isBlank()||e.getString("business").isBlank()||e.getString("currency").isBlank()||e.getLong("amount")<=0||e.getLong("balanceBefore")<e.getLong("amount"))continue;d.intents.add(new Intent(e.getString("id"),e.getUUID("player"),e.getString("business"),e.getString("currency"),e.getLong("balanceBefore"),e.getLong("amount"),e.getBoolean("accountApplied")));}return d; }
}
