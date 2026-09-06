package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.economy.labor.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Player-facing labor market commands. */
public final class LaborCommand {
    private LaborCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        var root = Commands.literal("labor");
        root.then(Commands.literal("profile").executes(c -> profile(c.getSource().getPlayerOrException())));
        root.then(Commands.literal("skill").then(Commands.argument("skill", StringArgumentType.word()).then(Commands.argument("value", IntegerArgumentType.integer(0, 100)).executes(c -> skill(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "skill"), IntegerArgumentType.getInteger(c, "value"))))));
        var wage = Commands.argument("dailyWageMinor", LongArgumentType.longArg(1)).executes(c -> post(
                c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "company"),
                StringArgumentType.getString(c, "role"), 1, LongArgumentType.getLong(c, "dailyWageMinor"),
                "FOUNDATION", 0, 0));
        var role = Commands.argument("role", StringArgumentType.word()).then(wage);
        var company = Commands.argument("company", StringArgumentType.word()).then(role);
        var post = Commands.literal("post").then(company);
        root.then(post);
        root.then(Commands.literal("jobs").executes(c -> jobs(c.getSource().getPlayerOrException())));
        root.then(Commands.literal("hire").then(Commands.argument("offerId", StringArgumentType.word()).then(Commands.argument("worker", EntityArgument.player()).executes(c -> hire(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "offerId"), EntityArgument.getPlayer(c, "worker"))))));
        root.then(Commands.literal("contracts").executes(c -> contracts(c.getSource().getPlayerOrException())));
        root.then(Commands.literal("end").then(Commands.argument("employmentId", StringArgumentType.word()).executes(c -> end(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "employmentId")))));
        d.register(root);
    }

    private static int profile(ServerPlayer p) { LaborMarketService.ensureProfile(p); LaborProfile x = LaborMarketSavedData.get(p.getServer()).profile(LaborMarketService.actor(p)); p.sendSystemMessage(Component.literal("labor profile participation=" + x.participation() + " reservation=" + x.reservationWageMinor() + " skills=" + x.skills())); return 1; }
    private static int skill(ServerPlayer p, String id, int value) { try { LaborSkill s = LaborSkill.valueOf(id.toUpperCase()); LaborMarketService.ensureProfile(p); LaborProfile old = LaborMarketSavedData.get(p.getServer()).profile(LaborMarketService.actor(p)); var m = new java.util.EnumMap<LaborSkill, Integer>(LaborSkill.class); m.putAll(old.skills()); m.put(s, value); LaborMarketSavedData.get(p.getServer()).registerProfile(new LaborProfile(old.actorId(), m, old.participation(), old.reservationWageMinor())); p.sendSystemMessage(Component.literal("skill updated")); return 1; } catch (IllegalArgumentException e) { p.sendSystemMessage(Component.literal("unknown skill: FOUNDATION/TECHNICAL/PROFESSIONAL/ADAPTABILITY")); return 0; } }
    private static int post(ServerPlayer p, String company, String role, int vacancies, long wage, String skill, int min, int days) { try { boolean ok = LaborMarketService.post(p, company, role, vacancies, wage, LaborSkill.valueOf(skill.toUpperCase()), min, days); p.sendSystemMessage(Component.literal(ok ? "job posted" : "job posting failed")); return ok ? 1 : 0; } catch (IllegalArgumentException e) { p.sendSystemMessage(Component.literal("unknown skill or invalid parameter")); return 0; } }
    private static int jobs(ServerPlayer p) { for (JobOffer o : LaborMarketSavedData.get(p.getServer()).openOffers(p.getServer().overworld().getGameTime())) p.sendSystemMessage(Component.literal(o.id().substring(0, 8) + " employer=" + o.employerId() + " role=" + o.role() + " vacancies=" + o.vacancies() + " wage=" + o.dailyWageMinor() + " skill=" + o.requiredSkill() + " min=" + o.minimumSkill())); return 1; }
    private static int hire(ServerPlayer p, String id, ServerPlayer w) { boolean ok = LaborMarketService.hire(p, id, w); p.sendSystemMessage(Component.literal(ok ? "hired" : "hire failed: offer, skill, wage or permission check failed")); return ok ? 1 : 0; }
    private static int contracts(ServerPlayer p) { for (EmploymentRecord e : LaborMarketSavedData.get(p.getServer()).employments()) if (e.workerId().equals(LaborMarketService.actor(p)) || CompanyHelper.getCompanies(p).values().stream().anyMatch(c -> c.companyId().equals(e.employerId()))) p.sendSystemMessage(Component.literal(e.id().substring(0, 8) + " role=" + e.role() + " wage=" + e.dailyWageMinor() + " active=" + e.active())); return 1; }
    private static int end(ServerPlayer p, String id) { boolean ok = LaborMarketService.end(p, id); p.sendSystemMessage(Component.literal(ok ? "employment ended" : "end failed: not found or unauthorized")); return ok ? 1 : 0; }
}
