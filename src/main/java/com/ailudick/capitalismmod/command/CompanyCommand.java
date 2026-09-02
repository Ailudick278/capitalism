package com.ailudick.capitalismmod.command;

import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.company.AcquisitionSavedData;
import com.ailudick.capitalismmod.company.CompanyHelper;
import com.ailudick.capitalismmod.company.PublicTakeoverSavedData;
import com.ailudick.capitalismmod.company.CompanyTypes;
import com.ailudick.capitalismmod.economy.EconomySavedData;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.MinecraftServer;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class CompanyCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var root = Commands.literal("company");
        root.then(Commands.literal("list").executes(ctx -> list(ctx.getSource())));
        root.then(Commands.literal("upgrade")
                .then(Commands.argument("name", StringArgumentType.word())
                        .executes(ctx -> upgrade(ctx.getSource(), StringArgumentType.getString(ctx, "name")))));
        root.then(Commands.literal("withdraw")
                .then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("currency", StringArgumentType.word())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> withdraw(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name"),
                                                StringArgumentType.getString(ctx, "currency"),
                                                LongArgumentType.getLong(ctx, "amount")))))));
        root.then(Commands.literal("acquire")
                .then(Commands.argument("seller", EntityArgument.player())
                        .then(Commands.argument("company", StringArgumentType.word())
                                .then(Commands.argument("price", LongArgumentType.longArg(1))
                                        .executes(ctx -> createOffer(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "seller"),
                                                StringArgumentType.getString(ctx, "company"),
                                                LongArgumentType.getLong(ctx, "price")))))));
        root.then(Commands.literal("takeover")
                .then(Commands.literal("list").executes(ctx -> listOffers(ctx.getSource())))
                .then(Commands.literal("accept")
                        .then(Commands.argument("offer", StringArgumentType.word())
                                .executes(ctx -> acceptOffer(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "offer")))))
                .then(Commands.literal("reject")
                        .then(Commands.argument("offer", StringArgumentType.word())
                                .executes(ctx -> rejectOffer(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "offer")))))
                .then(Commands.literal("publicoffer")
                        .then(Commands.argument("stock", StringArgumentType.word())
                                .then(Commands.argument("shareholder", EntityArgument.player())
                                        .then(Commands.argument("price", LongArgumentType.longArg(1))
                                                .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> createPublicOffer(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "shareholder"),
                                                                StringArgumentType.getString(ctx, "stock"),
                                                                LongArgumentType.getLong(ctx, "price"),
                                                                IntegerArgumentType.getInteger(ctx, "quantity"))))))))
                .then(Commands.literal("publiclist").executes(ctx -> listPublicOffers(ctx.getSource())))
                .then(Commands.literal("publicaccept")
                        .then(Commands.argument("offer", StringArgumentType.word())
                                .executes(ctx -> acceptPublicOffer(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "offer")))))
                .then(Commands.literal("publicreject")
                        .then(Commands.argument("offer", StringArgumentType.word())
                                .executes(ctx -> rejectPublicOffer(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "offer"))))));
        root.then(Commands.literal("control")
                .then(Commands.argument("stock", StringArgumentType.word())
                        .executes(ctx -> showControl(ctx.getSource(),
                                StringArgumentType.getString(ctx, "stock")))));
        root.then(Commands.literal("merge")
                .then(Commands.argument("source", StringArgumentType.word())
                        .then(Commands.argument("target", StringArgumentType.word())
                                .executes(ctx -> merge(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "source"),
                                        StringArgumentType.getString(ctx, "target"))))));
        dispatcher.register(root);
    }

    private static int list(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        Map<String, Company> companies = CompanyHelper.getCompanies(player);
        if (companies.isEmpty()) {
            source.sendFailure(Component.translatable("command.capitalismmod.no_companies"));
            return 0;
        }
        for (Company company : companies.values()) {
            player.sendSystemMessage(Component.translatable("command.capitalismmod.company_line",
                    company.level(), company.name(),
                    Component.translatable(CompanyTypes.nameKey(company.type())),
                    company.treasuryOf("usd"), Component.translatable(Currencies.USD.nameKey())));
        }
        return companies.size();
    }

    private static int upgrade(CommandSourceStack source, String name) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (CompanyHelper.getCompany(player, name) == null) {
            source.sendFailure(Component.translatable("command.capitalismmod.company_not_found", name));
            return 0;
        }
        if (!CompanyHelper.upgrade(player, name)) {
            source.sendFailure(Component.translatable("command.capitalismmod.insufficient"));
            return 0;
        }
        player.sendSystemMessage(Component.translatable("command.capitalismmod.company_upgraded",
                name, CompanyHelper.getCompany(player, name).level()));
        return 1;
    }

    private static int withdraw(CommandSourceStack source, String name, String currencyId, long amount) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!Currencies.exists(currencyId)) {
            source.sendFailure(Component.translatable("command.capitalismmod.unknown_currency"));
            return 0;
        }
        if (!CompanyHelper.withdraw(player, name, currencyId, amount)) {
            source.sendFailure(Component.translatable("command.capitalismmod.insufficient"));
            return 0;
        }
        Currency currency = Currencies.byId(currencyId);
        player.sendSystemMessage(Component.translatable("command.capitalismmod.company_withdrawn",
                name, amount, Component.translatable(currency.nameKey())));
        return 1;
    }

    private static int createOffer(CommandSourceStack source, ServerPlayer seller, String company, long price)
            throws CommandSyntaxException {
        ServerPlayer buyer = source.getPlayerOrException();
        if (seller.getUUID().equals(buyer.getUUID()) || CompanyHelper.getCompany(seller, company) == null
                || CompanyHelper.isListed(seller, company)) {
            source.sendFailure(Component.literal("Only an online, unlisted company can receive an acquisition offer."));
            return 0;
        }
        AcquisitionSavedData data = AcquisitionSavedData.get(source.getServer());
        AcquisitionSavedData.Offer offer = new AcquisitionSavedData.Offer(
                java.util.UUID.randomUUID().toString().substring(0, 8), buyer.getUUID(), seller.getUUID(), company,
                price, source.getServer().overworld().getGameTime());
        data.add(offer);
        buyer.sendSystemMessage(Component.literal("Acquisition offer sent: " + offer.id()));
        seller.sendSystemMessage(Component.literal("You received an acquisition offer " + offer.id()
                + " for " + company + " at USD " + price + ". Use /company takeover accept " + offer.id()));
        return 1;
    }

    private static int listOffers(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        int count = 0;
        for (AcquisitionSavedData.Offer offer : AcquisitionSavedData.get(source.getServer()).offers()) {
            if (offer.sellerUuid().equals(player.getUUID()) || offer.buyerUuid().equals(player.getUUID())) {
                source.sendSuccess(() -> Component.literal("Offer " + offer.id() + " | " + offer.companyName()
                        + " | USD " + offer.price() + " | "
                        + (offer.sellerUuid().equals(player.getUUID()) ? "incoming" : "outgoing")), false);
                count++;
            }
        }
        if (count == 0) {
            source.sendSuccess(() -> Component.literal("No acquisition offers."), false);
        }
        return count;
    }

    private static int acceptOffer(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer seller = source.getPlayerOrException();
        AcquisitionSavedData data = AcquisitionSavedData.get(source.getServer());
        AcquisitionSavedData.Offer offer = data.find(id);
        ServerPlayer buyer = offer == null ? null : source.getServer().getPlayerList().getPlayer(offer.buyerUuid());
        if (offer == null || !offer.sellerUuid().equals(seller.getUUID()) || buyer == null
                || !CompanyHelper.acquire(seller, buyer, offer)) {
            source.sendFailure(Component.literal("Offer invalid, buyer offline, company changed, or funds are insufficient."));
            return 0;
        }
        data.remove(id);
        seller.sendSystemMessage(Component.literal("Acquisition completed."));
        buyer.sendSystemMessage(Component.literal("You acquired " + offer.companyName() + "."));
        return 1;
    }

    private static int rejectOffer(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer seller = source.getPlayerOrException();
        AcquisitionSavedData data = AcquisitionSavedData.get(source.getServer());
        AcquisitionSavedData.Offer offer = data.find(id);
        if (offer == null || !offer.sellerUuid().equals(seller.getUUID())) {
            source.sendFailure(Component.literal("Offer not found or you are not the seller."));
            return 0;
        }
        data.remove(id);
        source.sendSuccess(() -> Component.literal("Acquisition offer rejected."), false);
        return 1;
    }

    private static int merge(CommandSourceStack source, String sourceName, String targetName)
            throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!CompanyHelper.merge(player, sourceName, targetName)) {
            source.sendFailure(Component.literal("Companies must be yours, unlisted, and use the same industry."));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Company " + sourceName + " merged into " + targetName + "."), false);
        return 1;
    }

    private static int createPublicOffer(CommandSourceStack source, ServerPlayer seller, String stockId,
                                         long price, int quantity) throws CommandSyntaxException {
        ServerPlayer buyer = source.getPlayerOrException();
        EconomySavedData economy = EconomySavedData.get(source.getServer());
        if (seller.getUUID().equals(buyer.getUUID()) || price <= 0 || quantity <= 0 || !economy.isListed(stockId)
                || economy.holdings(stockId, seller.getUUID()) < quantity) {
            source.sendFailure(Component.literal("The stock is not listed or the shareholder lacks enough shares."));
            return 0;
        }
        PublicTakeoverSavedData.Offer offer = new PublicTakeoverSavedData.Offer(
                java.util.UUID.randomUUID().toString().substring(0, 8), buyer.getUUID(), seller.getUUID(), stockId,
                price, quantity, source.getServer().overworld().getGameTime());
        PublicTakeoverSavedData.get(source.getServer()).add(offer);
        seller.sendSystemMessage(Component.literal("Public takeover offer " + offer.id() + " received for "
                + quantity + " shares at USD " + price + " each."));
        buyer.sendSystemMessage(Component.literal("Public takeover offer sent: " + offer.id()));
        return 1;
    }

    private static int listPublicOffers(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        long now = source.getServer().overworld().getGameTime();
        int count = 0;
        for (PublicTakeoverSavedData.Offer offer : PublicTakeoverSavedData.get(source.getServer()).offers()) {
            if (offer.buyerUuid().equals(player.getUUID()) || offer.sellerUuid().equals(player.getUUID())) {
                long left = Math.max(0L, PublicTakeoverSavedData.OFFER_TTL - (now - offer.createdTick()));
                source.sendSuccess(() -> Component.literal("Public offer " + offer.id() + " | " + offer.stockId()
                        + " | " + offer.quantity() + " shares @ USD " + offer.pricePerShare()
                        + " | " + left + " ticks left"), false);
                count++;
            }
        }
        if (count == 0) source.sendSuccess(() -> Component.literal("No public takeover offers."), false);
        return count;
    }

    private static int acceptPublicOffer(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer seller = source.getPlayerOrException();
        PublicTakeoverSavedData data = PublicTakeoverSavedData.get(source.getServer());
        PublicTakeoverSavedData.Offer offer = data.find(id);
        ServerPlayer buyer = offer == null ? null : source.getServer().getPlayerList().getPlayer(offer.buyerUuid());
        long now = source.getServer().overworld().getGameTime();
        if (offer == null || buyer == null || now - offer.createdTick() > PublicTakeoverSavedData.OFFER_TTL
                || !offer.sellerUuid().equals(seller.getUUID())
                || !CompanyHelper.acceptPublicOffer(seller, buyer, offer)) {
            source.sendFailure(Component.literal("Public offer expired, invalid, buyer offline, or funds are insufficient."));
            return 0;
        }
        data.remove(id);
        seller.sendSystemMessage(Component.literal("Public share sale completed."));
        buyer.sendSystemMessage(Component.literal("Public share purchase completed."));
        return 1;
    }

    private static int rejectPublicOffer(CommandSourceStack source, String id) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        PublicTakeoverSavedData data = PublicTakeoverSavedData.get(source.getServer());
        PublicTakeoverSavedData.Offer offer = data.find(id);
        if (offer == null || (!offer.sellerUuid().equals(player.getUUID()) && !offer.buyerUuid().equals(player.getUUID()))) {
            source.sendFailure(Component.literal("Public offer not found."));
            return 0;
        }
        data.remove(id);
        source.sendSuccess(() -> Component.literal("Public offer cancelled."), false);
        return 1;
    }

    private static int showControl(CommandSourceStack source, String stockId) {
        EconomySavedData data = EconomySavedData.get(source.getServer());
        if (!data.isListed(stockId)) {
            source.sendFailure(Component.literal("Listed company stock not found."));
            return 0;
        }
        java.util.UUID controller = CompanyHelper.controller(source.getServer(), stockId);
        String message = controller == null ? "No shareholder currently controls " + stockId
                : "Controlling shareholder of " + stockId + ": " + controller;
        source.sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
