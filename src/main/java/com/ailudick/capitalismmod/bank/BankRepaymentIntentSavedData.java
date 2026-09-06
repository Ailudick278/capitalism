package com.ailudick.capitalismmod.bank;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable intent for a bank debt repayment interrupted after wallet payment. */
public final class BankRepaymentIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_bank_repayment_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();
    public record Intent(String id, UUID player, String accountId, String currencyId, long debtBefore, long amount, boolean paid) {
        public Intent { id=id==null?"":id; accountId=accountId==null?"":accountId; currencyId=currencyId==null?"":currencyId; debtBefore=Math.max(0,debtBefore); amount=Math.max(0,amount); }
        public Intent withPaid(boolean value) { return new Intent(id,player,accountId,currencyId,debtBefore,amount,value); }
    }
    private BankRepaymentIntentSavedData() {}
    public static BankRepaymentIntentSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(BankRepaymentIntentSavedData::new, BankRepaymentIntentSavedData::load), ID); }
    public List<Intent> intents() { return List.copyOf(intents); }
    public Intent find(String id) { return intents.stream().filter(i -> id!=null&&id.equals(i.id())).findFirst().orElse(null); }
    public void add(Intent i) { if(i==null||i.id().isBlank()||i.player()==null||i.accountId().isBlank()||i.currencyId().isBlank()||i.debtBefore()<i.amount()||i.amount()<=0||find(i.id())!=null)return; intents.add(i); while(intents.size()>MAX_INTENTS)intents.remove(0); setDirty(); }
    public void markPaid(String id) { Intent i=find(id); if(i!=null&&!i.paid()){intents.set(intents.indexOf(i),i.withPaid(true));setDirty();} }
    public void remove(String id) { if(id!=null&&intents.removeIf(i->id.equals(i.id())))setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list=new ListTag(); for(Intent i:intents){CompoundTag e=new CompoundTag();e.putString("id",i.id());e.putUUID("player",i.player());e.putString("account",i.accountId());e.putString("currency",i.currencyId());e.putLong("debtBefore",i.debtBefore());e.putLong("amount",i.amount());e.putBoolean("paid",i.paid());list.add(e);}tag.put("intents",list);return tag; }
    public static BankRepaymentIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) { BankRepaymentIntentSavedData d=new BankRepaymentIntentSavedData();ListTag l=tag.getList("intents",Tag.TAG_COMPOUND);for(int n=Math.max(0,l.size()-MAX_INTENTS);n<l.size();n++){CompoundTag e=l.getCompound(n);if(!e.hasUUID("player")||e.getString("id").isBlank()||e.getString("account").isBlank()||e.getString("currency").isBlank()||e.getLong("amount")<=0||e.getLong("debtBefore")<e.getLong("amount"))continue;d.intents.add(new Intent(e.getString("id"),e.getUUID("player"),e.getString("account"),e.getString("currency"),e.getLong("debtBefore"),e.getLong("amount"),e.getBoolean("paid")));}return d; }
}
