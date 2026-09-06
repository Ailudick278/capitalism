package com.ailudick.capitalismmod.company;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Durable intent for withdrawing company treasury funds to the owner. */
public final class CompanyWithdrawalIntentSavedData extends SavedData {
    private static final String ID = "capitalismmod_company_withdrawal_intents";
    private static final int MAX_INTENTS = 8192;
    private final List<Intent> intents = new ArrayList<>();

    public record Intent(String id, UUID owner, String companyId, String currencyId,
                         long balanceBefore, long amount) {
        public Intent { id=id==null?"":id; companyId=companyId==null?"":companyId; currencyId=currencyId==null?"":currencyId; balanceBefore=Math.max(0,balanceBefore); amount=Math.max(0,amount); }
    }
    private CompanyWithdrawalIntentSavedData() {}
    public static CompanyWithdrawalIntentSavedData get(MinecraftServer server) { return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(CompanyWithdrawalIntentSavedData::new, CompanyWithdrawalIntentSavedData::load), ID); }
    public List<Intent> intents() { return List.copyOf(intents); }
    public Intent find(String id) { return intents.stream().filter(i->id!=null&&id.equals(i.id())).findFirst().orElse(null); }
    public void add(Intent i) { if(i==null||i.id().isBlank()||i.owner()==null||i.companyId().isBlank()||i.currencyId().isBlank()||i.amount()<=0||i.amount()>i.balanceBefore()||find(i.id())!=null)return;intents.add(i);while(intents.size()>MAX_INTENTS)intents.remove(0);setDirty(); }
    public void remove(String id) { if(id!=null&&intents.removeIf(i->id.equals(i.id())))setDirty(); }
    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) { ListTag list=new ListTag();for(Intent i:intents){CompoundTag e=new CompoundTag();e.putString("id",i.id());e.putUUID("owner",i.owner());e.putString("company",i.companyId());e.putString("currency",i.currencyId());e.putLong("balanceBefore",i.balanceBefore());e.putLong("amount",i.amount());list.add(e);}tag.put("intents",list);return tag; }
    public static CompanyWithdrawalIntentSavedData load(CompoundTag tag, HolderLookup.Provider registries) { CompanyWithdrawalIntentSavedData d=new CompanyWithdrawalIntentSavedData();ListTag l=tag.getList("intents",Tag.TAG_COMPOUND);for(int n=Math.max(0,l.size()-MAX_INTENTS);n<l.size();n++){CompoundTag e=l.getCompound(n);if(!e.hasUUID("owner")||e.getString("id").isBlank()||e.getString("company").isBlank()||e.getString("currency").isBlank()||e.getLong("amount")<=0||e.getLong("balanceBefore")<e.getLong("amount"))continue;d.intents.add(new Intent(e.getString("id"),e.getUUID("owner"),e.getString("company"),e.getString("currency"),e.getLong("balanceBefore"),e.getLong("amount")));}return d; }
}
