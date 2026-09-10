package com.ailudick.capitalismmod.blockentity;

import com.ailudick.capitalismmod.company.MachineType;
import com.ailudick.capitalismmod.company.Industries;
import com.ailudick.capitalismmod.company.IndustrySpec;
import com.ailudick.capitalismmod.company.ProductionRecipe;
import com.ailudick.capitalismmod.compat.IndustrialTransportPort;
import com.ailudick.capitalismmod.factory.MultiblockMachineService;
import com.ailudick.capitalismmod.factory.MultiblockMachineSpec;
import com.ailudick.capitalismmod.init.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.ailudick.capitalismmod.entity.PlaceholderNpc;
import com.ailudick.capitalismmod.population.NpcProfession;
import com.ailudick.capitalismmod.progression.TechnologyEra;
import com.ailudick.capitalismmod.progression.TechnologyProgression;
import net.minecraft.world.phys.AABB;

/**
 * Physical machine state. Recipes remain data-driven in CapitalismData; this
 * block entity only owns the machine's local identity and visible progress.
 */
public final class FactoryMachineBlockEntity extends BlockEntity implements IndustrialTransportPort {
    private static final String MACHINE_TAG = "MachineType";
    private static final String RECIPE_TAG = "RecipeId";
    private static final String PROGRESS_TAG = "Progress";
    private static final String MAX_PROGRESS_TAG = "MaxProgress";
    private static final String STATUS_TAG = "Status";
    private static final String INDUSTRY_TAG = "IndustryId";
    private static final String FORMED_TAG = "Formed";
    private static final int BUFFER_LIMIT = 4096;
    private String industryId = "manufacturing";
    private String machineType = MachineType.ASSEMBLY_LINE.id();
    private String recipeId = "default";
    private int progress;
    private int maxProgress = 100;
    private String status = "idle";
    private boolean formed;
    private UUID workerUuid;
    private final Map<String, Integer> inputs = new HashMap<>();
    private final Map<String, Integer> outputs = new HashMap<>();

    public FactoryMachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTORY_MACHINE.get(), pos, state);
    }

    public String machineType() { return machineType; }
    public String industryId() { return industryId; }
    public String recipeId() { return recipeId; }
    public int progress() { return progress; }
    public int maxProgress() { return maxProgress; }
    public String status() { return status; }
    public boolean formed() { return formed; }
    public MultiblockMachineSpec structure() { return MultiblockMachineSpec.forMachine(machineType); }
    public UUID workerUuid() { return workerUuid; }
    public boolean assignWorker(PlaceholderNpc npc) {
        if (npc == null || npc.profession() == null || !npc.profession().canOperate(machineType)) return false;
        if (npc.hasWorkMachine() && !worldPosition.equals(npc.workMachine())) return false;
        PlaceholderNpc previous = worker();
        if (previous != null && previous != npc) previous.clearWorkMachine();
        workerUuid = npc.getUUID(); npc.assignWorkMachine(worldPosition); setChanged(); return true;
    }
    public void clearWorker() {
        PlaceholderNpc previous = worker();
        if (previous != null) {
            previous.clearWorkMachine();
            previous.assignProfession(NpcProfession.UNEMPLOYED);
        }
        workerUuid = null; setChanged();
    }
    public PlaceholderNpc worker() {
        if (workerUuid == null || level == null) return null;
        return level instanceof ServerLevel server && server.getEntity(workerUuid) instanceof PlaceholderNpc npc ? npc : null;
    }

    /** Recruits only an unemployed nearby NPC; occupied workers are never stolen. */
    private boolean autoHireNearbyWorker() {
        if (!(level instanceof ServerLevel server)) return false;
        NpcProfession preferred = NpcProfession.preferredFor(machineType);
        for (PlaceholderNpc npc : server.getEntitiesOfClass(PlaceholderNpc.class,
                new AABB(worldPosition).inflate(8.0D))) {
            if (npc.hasWorkMachine() || (npc.profession() != null && npc.profession() != NpcProfession.UNEMPLOYED)) continue;
            npc.assignProfession(preferred);
            if (assignWorker(npc)) return true;
        }
        return false;
    }

    public boolean formStructure() {
        formed = level != null && MultiblockMachineService.isFormed(level, worldPosition, structure());
        status = formed ? "ready" : "invalid_structure";
        setChanged();
        return formed;
    }

    public void configure(String machineType, String recipeId) {
        configure(industryId, machineType, recipeId);
    }

    public void configure(String industryId, String machineType, String recipeId) {
        this.industryId = Industries.byId(industryId) == null ? "manufacturing" : industryId;
        MachineType parsed = MachineType.parse(machineType);
        this.machineType = parsed == null ? MachineType.ASSEMBLY_LINE.id() : parsed.id();
        this.recipeId = recipeId == null || recipeId.isBlank() ? "default" : recipeId.trim();
        this.progress = 0;
        this.status = "idle";
        setChanged();
    }

    public ProductionRecipe recipe() {
        IndustrySpec industry = Industries.byId(industryId);
        return industry == null ? null : industry.recipe(recipeId);
    }

    @Override public Map<String, Integer> inputBuffer() { return Map.copyOf(inputs); }
    @Override public Map<String, Integer> outputBuffer() { return Map.copyOf(outputs); }
    @Override public int insertInput(String itemId, int amount) { return insert(inputs, itemId, amount); }
    @Override public int extractOutput(String itemId, int amount) { return extract(outputs, itemId, amount); }

    private int insert(Map<String, Integer> buffer, String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) return 0;
        int current = buffer.getOrDefault(itemId, 0), moved = Math.min(amount, BUFFER_LIMIT - current);
        if (moved <= 0) return 0;
        buffer.put(itemId, current + moved); setChanged(); return moved;
    }

    private int extract(Map<String, Integer> buffer, String itemId, int amount) {
        if (itemId == null || itemId.isBlank() || amount <= 0) return 0;
        int current = buffer.getOrDefault(itemId, 0), moved = Math.min(amount, current);
        if (moved <= 0) return 0;
        if (moved == current) buffer.remove(itemId); else buffer.put(itemId, current - moved);
        setChanged(); return moved;
    }

    public boolean canCraft() {
        ProductionRecipe current = recipe();
        if (current == null || current.outputs().isEmpty()) return false;
        for (Map.Entry<String, Integer> input : current.inputs().entrySet())
            if (inputs.getOrDefault(input.getKey(), 0) < input.getValue()) return false;
        return current.outputs().entrySet().stream().allMatch(output ->
                outputs.getOrDefault(output.getKey(), 0) <= BUFFER_LIMIT - output.getValue());
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state,
                                  FactoryMachineBlockEntity machine) {
        if (!(level instanceof ServerLevel server)) return;
        if (machine.tickCount() % 20 == 0) machine.formStructure();
        if (!machine.formed) {
            machine.status = "invalid_structure";
            return;
        }
        PlaceholderNpc worker = machine.worker();
        if (worker == null) {
            machine.autoHireNearbyWorker();
            worker = machine.worker();
        }
        if (worker == null || worker.profession() == null || !worker.profession().canOperate(machine.machineType)) {
            machine.status = "waiting_for_worker";
            machine.progress = 0;
            return;
        }
        TechnologyEra requiredEra = TechnologyEra.forMachine(machine.machineType);
        if (!TechnologyProgression.isUnlocked(server, requiredEra)) {
            machine.status = "technology_locked";
            machine.progress = 0;
            return;
        }
        if (machine.canCraft()) {
            machine.status = "running";
            machine.progress++;
            int efficiency = Math.max(25, 50 + worker.skill() / 2);
            machine.maxProgress = Math.max(40, (machine.recipe().workersPerCycle() * 400) / efficiency);
            if (machine.progress >= machine.maxProgress) {
                for (Map.Entry<String, Integer> input : machine.recipe().inputs().entrySet())
                    machine.extract(inputsOf(machine), input.getKey(), input.getValue());
                for (Map.Entry<String, Integer> output : machine.recipe().outputs().entrySet())
                    machine.insert(outputsOf(machine), output.getKey(), output.getValue());
                machine.progress = 0;
            }
        } else {
            machine.status = "waiting_for_inputs";
            machine.progress = 0;
        }
        if (machine.tickCount() % 20 == 0) machine.setChanged();
    }

    private static Map<String, Integer> inputsOf(FactoryMachineBlockEntity machine) { return machine.inputs; }
    private static Map<String, Integer> outputsOf(FactoryMachineBlockEntity machine) { return machine.outputs; }

    public boolean hasValidRecipe() { return recipe() != null && !recipe().outputs().isEmpty(); }

    public void setProductionState(int progress, int maxProgress, String status) {
        this.progress = Math.max(0, Math.min(Math.max(1, maxProgress), progress));
        this.maxProgress = Math.max(1, maxProgress);
        this.status = status == null || status.isBlank() ? "idle" : status;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString(INDUSTRY_TAG, industryId);
        tag.putBoolean(FORMED_TAG, formed);
        if (workerUuid != null) tag.putUUID("Worker", workerUuid);
        tag.putString(MACHINE_TAG, machineType); tag.putString(RECIPE_TAG, recipeId);
        tag.putInt(PROGRESS_TAG, progress); tag.putInt(MAX_PROGRESS_TAG, maxProgress);
        tag.putString(STATUS_TAG, status);
        saveBuffer(tag, "Inputs", inputs); saveBuffer(tag, "Outputs", outputs);
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        configure(tag.getString(INDUSTRY_TAG), tag.getString(MACHINE_TAG), tag.getString(RECIPE_TAG));
        progress = Math.max(0, tag.getInt(PROGRESS_TAG));
        maxProgress = Math.max(1, tag.getInt(MAX_PROGRESS_TAG));
        progress = Math.min(progress, maxProgress);
        status = tag.getString(STATUS_TAG);
        formed = tag.getBoolean(FORMED_TAG);
        workerUuid = tag.hasUUID("Worker") ? tag.getUUID("Worker") : null;
        if (status.isBlank()) status = "idle";
        loadBuffer(tag, "Inputs", inputs); loadBuffer(tag, "Outputs", outputs);
    }

    private static void saveBuffer(CompoundTag parent, String key, Map<String, Integer> values) {
        CompoundTag data = new CompoundTag();
        values.forEach((item, amount) -> { if (amount > 0) data.putInt(item, amount); });
        parent.put(key, data);
    }

    private static void loadBuffer(CompoundTag parent, String key, Map<String, Integer> target) {
        if (!parent.contains(key, net.minecraft.nbt.Tag.TAG_COMPOUND)) return;
        CompoundTag data = parent.getCompound(key);
        for (String item : data.getAllKeys()) {
            int amount = Math.min(BUFFER_LIMIT, Math.max(0, data.getInt(item)));
            if (!item.isBlank() && amount > 0) target.put(item, amount);
        }
    }

    private long tickCount() { return level == null ? 0L : level.getGameTime(); }
}
