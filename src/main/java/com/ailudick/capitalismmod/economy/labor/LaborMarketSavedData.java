package com.ailudick.capitalismmod.economy.labor;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/** Persistent first-stage labor market: profiles, job offers and matching records. */
public final class LaborMarketSavedData extends SavedData {
    private static final String ID = "capitalismmod_labor_market";
    private static final int MAX_RECORDS = 4096;
    private final List<LaborProfile> profiles = new ArrayList<>();
    private final List<JobOffer> offers = new ArrayList<>();
    private final List<EmploymentRecord> employments = new ArrayList<>();

    private LaborMarketSavedData() {}

    public static LaborMarketSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new Factory<>(LaborMarketSavedData::new, LaborMarketSavedData::load), ID);
    }

    public List<LaborProfile> profiles() { return List.copyOf(profiles); }
    public List<JobOffer> offers() { return List.copyOf(offers); }
    public List<EmploymentRecord> employments() { return List.copyOf(employments); }
    public LaborProfile profile(String actorId) { return profiles.stream().filter(p -> p.actorId().equals(actorId)).findFirst().orElse(null); }
    public List<JobOffer> openOffers(long now) { return offers.stream().filter(o -> o.vacancies() > 0 && (o.closesAt() <= 0L || now < o.closesAt())).toList(); }
    public List<EmploymentRecord> activeForWorker(String workerId) { return employments.stream().filter(e -> e.active() && e.workerId().equals(workerId)).toList(); }
    public List<EmploymentRecord> activeForEmployer(String employerId) { return employments.stream().filter(e -> e.active() && e.employerId().equals(employerId)).toList(); }
    public int activeWorkers(String employerId) { return activeForEmployer(employerId).size(); }
    public long dailyWages(String employerId) { return activeForEmployer(employerId).stream().mapToLong(EmploymentRecord::dailyWageMinor).reduce(0L, LaborMarketSavedData::add); }

    public JobOffer offer(String id) { return offers.stream().filter(o -> o.id().equals(id)).findFirst().orElse(null); }

    public boolean reserveVacancy(String id) {
        JobOffer offer = offer(id);
        if (offer == null || offer.vacancies() <= 0) return false;
        offers.set(offers.indexOf(offer), offer.withVacancies(offer.vacancies() - 1));
        setDirty(); return true;
    }

    public void registerProfile(LaborProfile profile) {
        if (profile == null) return;
        profiles.removeIf(existing -> existing.actorId().equals(profile.actorId()));
        profiles.add(profile); trim(); setDirty();
    }

    public boolean post(JobOffer offer) {
        if (offer == null || offers.stream().anyMatch(existing -> existing.id().equals(offer.id()))) return false;
        offers.add(offer); trim(); setDirty(); return true;
    }

    public boolean hire(EmploymentRecord employment) {
        if (employment == null || employments.stream().anyMatch(e -> e.id().equals(employment.id()))
                || employments.stream().anyMatch(e -> e.active() && e.workerId().equals(employment.workerId()))) return false;
        employments.add(employment); trim(); setDirty(); return true;
    }

    public boolean endEmployment(String id, long endedAt) {
        for (int i = 0; i < employments.size(); i++) {
            EmploymentRecord employment = employments.get(i);
            if (employment.id().equals(id) && employment.active()) {
                employments.set(i, employment.end(endedAt)); setDirty(); return true;
            }
        }
        return false;
    }

    public int expireOffers(long now) {
        int before = offers.size(); offers.removeIf(offer -> offer.closesAt() > 0L && now >= offer.closesAt());
        if (offers.size() != before) setDirty(); return before - offers.size();
    }

    private void trim() { while (profiles.size() > MAX_RECORDS) profiles.remove(0); while (offers.size() > MAX_RECORDS) offers.remove(0); while (employments.size() > MAX_RECORDS) employments.remove(0); }
    private static long add(long a, long b) { try { return Math.addExact(a, b); } catch (ArithmeticException e) { return Long.MAX_VALUE; } }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag profileList = new ListTag();
        for (LaborProfile p : profiles) { CompoundTag e = new CompoundTag(); e.putString("actor", p.actorId()); e.putInt("participation", p.participation()); e.putLong("reservation", p.reservationWageMinor()); CompoundTag skills = new CompoundTag(); p.skills().forEach((k,v)->skills.putInt(k.name(),v)); e.put("skills",skills); profileList.add(e); }
        tag.put("profiles", profileList);
        ListTag offerList = new ListTag();
        for (JobOffer o : offers) { CompoundTag e = new CompoundTag(); e.putString("id",o.id()); e.putString("employer",o.employerId()); e.putString("role",o.role()); e.putInt("vacancies",o.vacancies()); e.putLong("wage",o.dailyWageMinor()); e.putString("skill",o.requiredSkill().name()); e.putInt("minimum",o.minimumSkill()); e.putLong("posted",o.postedAt()); e.putLong("closes",o.closesAt()); offerList.add(e); }
        tag.put("offers", offerList);
        ListTag employmentList = new ListTag();
        for (EmploymentRecord e : employments) { CompoundTag n = new CompoundTag(); n.putString("id",e.id()); n.putString("worker",e.workerId()); n.putString("employer",e.employerId()); n.putString("role",e.role()); n.putLong("wage",e.dailyWageMinor()); n.putLong("started",e.startedAt()); n.putLong("ended",e.endedAt()); n.putBoolean("active",e.active()); employmentList.add(n); }
        tag.put("employments", employmentList); return tag;
    }

    public static LaborMarketSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LaborMarketSavedData data = new LaborMarketSavedData();
        ListTag ps = tag.getList("profiles", Tag.TAG_COMPOUND);
        for (int i=0;i<ps.size();i++) { CompoundTag e=ps.getCompound(i); try { java.util.EnumMap<LaborSkill,Integer> s=new java.util.EnumMap<>(LaborSkill.class); CompoundTag st=e.getCompound("skills"); for(String k:st.getAllKeys()) try{s.put(LaborSkill.valueOf(k),st.getInt(k));}catch(IllegalArgumentException ignored){} data.profiles.add(new LaborProfile(e.getString("actor"),s,e.getInt("participation"),Math.max(0L,e.getLong("reservation")))); } catch(IllegalArgumentException ignored){} }
        ListTag os = tag.getList("offers", Tag.TAG_COMPOUND);
        for (int i=0;i<os.size();i++) { CompoundTag e=os.getCompound(i); try { data.offers.add(new JobOffer(e.getString("id"),e.getString("employer"),e.getString("role"),e.getInt("vacancies"),Math.max(0L,e.getLong("wage")),LaborSkill.valueOf(e.getString("skill")),e.getInt("minimum"),e.getLong("posted"),e.getLong("closes"))); } catch(IllegalArgumentException ignored){} }
        ListTag es = tag.getList("employments", Tag.TAG_COMPOUND);
        for (int i=0;i<es.size();i++) { CompoundTag e=es.getCompound(i); try { data.employments.add(new EmploymentRecord(e.getString("id"),e.getString("worker"),e.getString("employer"),e.getString("role"),Math.max(0L,e.getLong("wage")),e.getLong("started"),Math.max(0L,e.getLong("ended")),e.getBoolean("active"))); } catch(IllegalArgumentException ignored){} }
        data.trim(); return data;
    }
}
