package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.calendar.PerpetualCalendar;
import com.ailudick.capitalismmod.menu.LandMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.ailudick.capitalismmod.menu.WorldMapMenu;
import com.ailudick.capitalismmod.network.payload.ClaimLandPayload;
import com.ailudick.capitalismmod.network.payload.ReleaseLandPayload;
import com.ailudick.capitalismmod.network.payload.SetLandPurposePayload;
import com.ailudick.capitalismmod.land.LandPurpose;
import com.ailudick.capitalismmod.network.payload.ManageLandTrustPayload;
import com.ailudick.capitalismmod.network.payload.SetLandPermissionsPayload;
import com.ailudick.capitalismmod.network.payload.ClearLandLogsPayload;
import com.ailudick.capitalismmod.land.LandValuationHelper;
import com.ailudick.capitalismmod.land.LandClaim;
import com.ailudick.capitalismmod.land.LandStatus;
import com.ailudick.capitalismmod.network.payload.LeaseLandPayload;
import com.ailudick.capitalismmod.network.payload.UnleaseLandPayload;
import com.ailudick.capitalismmod.network.payload.RequestLandDetailsPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Locale;

public class LandScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    private final boolean standaloneMap;
    protected int mapX = 24;
    protected int mapY = 58;
    protected int mapWidth = 240;
    protected int mapHeight = 180;
    // Four screen pixels per world block keeps individual blocks readable by default.
    protected final WorldMapWidget worldMapWidget = new WorldMapWidget();
    protected final WorldMapViewport viewport = worldMapWidget.viewport();
    private Button claimButton;
    private Button purposeButton;
    private Button trustAddButton;
    private Button trustRemoveButton;
    private Button memberBuildButton;
    private Button memberInteractButton;
    private Button memberContainerButton;
    private Button memberRedstoneButton;
    private Button clearLogsButton;
    private Button previousLogsButton;
    private Button nextLogsButton;
    private int landLogPage;
    private EditBox trustPlayerField;
    private EditBox leasePlayerField;
    private EditBox leaseDaysField;
    private EditBox leaseRentField;
    private Button leaseButton;
    private EditBox transferPlayerField;
    private Button transferButton;
    private Button acceptTransferButton;
    private Button rejectTransferButton;
    private EditBox auctionBidField;
    private Button auctionBidButton;
    private int ownedLandOffset;
    private boolean ownedLandAllDimensions;
    private int ownedLandStatusFilter;

    public LandScreen(T menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        standaloneMap = false;
        worldMapWidget.centerOnPlayer();
        imageWidth = 300;
        imageHeight = 190;
    }

    protected LandScreen(T menu, Inventory inventory, Component title, boolean standaloneMap) {
        super(menu, inventory, title);
        this.standaloneMap = standaloneMap;
        WorldMapClientState.load(viewport);
        worldMapWidget.centerOnPlayer();
        imageWidth = 300;
        imageHeight = 190;
    }

    @Override
    protected void init() {
        imageWidth = width;
        imageHeight = height;
        super.init();
        leftPos = 0;
        topPos = 0;
        if (standaloneMap) {
            mapX = 0;
            mapY = 0;
            mapWidth = width;
            mapHeight = height;
        } else {
            mapX = 24;
            mapY = 58;
            mapWidth = Math.max(240, (int) (width * 0.68F));
            mapHeight = Math.max(160, height - 112);
        }
        worldMapWidget.setBounds(mapX, mapY, mapWidth, mapHeight);
        if (!standaloneMap) {
            clearLogsButton = addRenderableWidget(Button.builder(Component.literal("清理日志"), button -> {
                        PacketDistributor.sendToServer(new ClearLandLogsPayload());
                    }).bounds(leftPos + mapX + mapWidth + 24, height - 30, 120, 20).build());
            previousLogsButton = addRenderableWidget(Button.builder(Component.literal("上一页"), button -> landLogPage--)
                    .bounds(leftPos + mapX + 96, topPos + mapY + mapHeight + 30, 58, 20).build());
            nextLogsButton = addRenderableWidget(Button.builder(Component.literal("下一页"), button -> landLogPage++)
                    .bounds(leftPos + mapX + 158, topPos + mapY + mapHeight + 30, 58, 20).build());
            previousLogsButton.active = false;
            nextLogsButton.active = false;
        }
        if (!standaloneMap) {
            claimButton = addRenderableWidget(Button.builder(Component.literal("认领选中区块"), button -> claimSelectedChunk())
                    .bounds(leftPos + mapX + mapWidth + 24, topPos + mapY + 96, 120, 20).build());
            claimButton.active = false;
            int permissionX = leftPos + mapX + mapWidth + 24;
            memberBuildButton = addRenderableWidget(Button.builder(Component.literal("成员建造：开"), button -> toggleMemberPermission(true))
                    .bounds(permissionX, topPos + mapY + 48, 120, 20).build());
            memberInteractButton = addRenderableWidget(Button.builder(Component.literal("成员交互：开"), button -> toggleMemberPermission(false))
                    .bounds(permissionX, topPos + mapY + 72, 120, 20).build());
            memberContainerButton = addRenderableWidget(Button.builder(Component.literal("容器访问：关"), button -> toggleSpecialPermission(true))
                    .bounds(permissionX, topPos + mapY + 144, 120, 20).build());
            memberRedstoneButton = addRenderableWidget(Button.builder(Component.literal("红石操作：关"), button -> toggleSpecialPermission(false))
                    .bounds(permissionX, topPos + mapY + 168, 120, 20).build());
            memberBuildButton.active = false;
            memberInteractButton.active = false;
            memberContainerButton.active = false;
            memberRedstoneButton.active = false;
            purposeButton = addRenderableWidget(Button.builder(Component.literal("修改用途"), button -> nextPurpose())
                    .bounds(leftPos + mapX + mapWidth + 24, topPos + mapY + 120, 120, 20).build());
            purposeButton.active = false;
            int trustX = leftPos + mapX + mapWidth + 24;
            int trustY = topPos + mapY + 146;
            trustPlayerField = addRenderableWidget(new EditBox(font, trustX, trustY, 120, 20,
                    Component.literal("玩家名称")));
            trustPlayerField.setMaxLength(16);
            trustAddButton = addRenderableWidget(Button.builder(Component.literal("添加信任"), button -> manageTrust(true))
                    .bounds(trustX, trustY + 24, 58, 20).build());
            trustRemoveButton = addRenderableWidget(Button.builder(Component.literal("移除信任"), button -> manageTrust(false))
                    .bounds(trustX + 62, trustY + 24, 58, 20).build());
            trustAddButton.active = false;
            trustRemoveButton.active = false;
            int leaseY = trustY + 48;
            leasePlayerField = addRenderableWidget(new EditBox(font, trustX, leaseY, 120, 20,
                    Component.literal("承租玩家")));
            leasePlayerField.setMaxLength(16);
            leaseDaysField = addRenderableWidget(new EditBox(font, trustX, leaseY + 24, 58, 20,
                    Component.literal("天数")));
            leaseDaysField.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
            leaseRentField = addRenderableWidget(new EditBox(font, trustX + 62, leaseY + 24, 58, 20,
                    Component.literal("租金")));
            leaseRentField.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
            leaseButton = addRenderableWidget(Button.builder(Component.literal("出租土地"), button -> leaseAction())
                    .bounds(trustX, leaseY + 48, 120, 20).build());
            leaseButton.active = false;
            int transferY = leaseY + 72;
            transferPlayerField = addRenderableWidget(new EditBox(font, trustX, transferY, 120, 20,
                    Component.literal("受让玩家")));
            transferPlayerField.setMaxLength(16);
            transferButton = addRenderableWidget(Button.builder(Component.literal("发起转让"), button -> transferLand())
                    .bounds(trustX, transferY + 24, 58, 20).build());
            acceptTransferButton = addRenderableWidget(Button.builder(Component.literal("接受转让"), button -> acceptTransfer())
                    .bounds(trustX + 62, transferY + 24, 58, 20).build());
            rejectTransferButton = addRenderableWidget(Button.builder(Component.literal("拒绝转让"), button -> rejectTransfer())
                    .bounds(trustX + 62, transferY + 48, 58, 20).build());
            transferButton.active = false;
            int bidY = mapY + mapHeight + 6;
            auctionBidField = addRenderableWidget(new EditBox(font, trustX, bidY, 70, 20, Component.literal("出价")));
            auctionBidField.setFilter(value -> value.isEmpty() || value.matches("\\d+"));
            auctionBidButton = addRenderableWidget(Button.builder(Component.literal("提交出价"), button -> placeAuctionBid())
                    .bounds(trustX + 74, bidY, 72, 20).build());
            auctionBidButton.active = false;
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        if (standaloneMap) {
            graphics.fill(0, 0, width, height, 0xFF101820);
            return;
        }
        GuiStyles.drawBackground(graphics, leftPos, topPos, imageWidth, imageHeight);
        graphics.fill(leftPos + 14, topPos + 12, leftPos + imageWidth - 14, topPos + 40, 0xCC14263A);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        if (standaloneMap) worldMapWidget.updateHover(menu, mouseX, mouseY, leftPos, topPos);
        drawMap(graphics);
        if (standaloneMap) {
            return;
        }
        LandMenu landMenu = (LandMenu) menu;
        Minecraft mc = Minecraft.getInstance();
        long currentGameTime = mc.level == null ? 0L : mc.level.getGameTime();
        LandStatus status = LandStatus.resolve(landMenu.taxOwed, landMenu.taxDueAt, landMenu.taxGraceUntil,
                landMenu.auctionActive, currentGameTime);
        boolean landLocked = landMenu.claimed
                && (status == LandStatus.TAX_FROZEN || status == LandStatus.AUCTION);
        if (claimButton != null) {
            boolean ownLand = landMenu.claimed && mc.player != null
                    && landMenu.ownerUuid.equals(mc.player.getUUID().toString());
            claimButton.setMessage(Component.literal(ownLand ? "放弃土地" : "认领选中区块"));
            claimButton.active = landMenu.hasSelectedChunk && (!landMenu.claimed || (ownLand && !landLocked));
            if (purposeButton != null) {
                LandPurpose purpose = LandPurpose.byCode(landMenu.purpose);
                purposeButton.setMessage(Component.literal(purpose == null ? "修改用途" : "用途：" + purpose.name()));
                purposeButton.active = landMenu.hasSelectedChunk && ownLand && !landLocked;
            }
            if (trustAddButton != null) {
                boolean canManageTrust = landMenu.hasSelectedChunk && ownLand && !landLocked
                        && trustPlayerField != null && !trustPlayerField.getValue().isBlank();
                trustAddButton.active = canManageTrust;
                trustRemoveButton.active = canManageTrust;
                if (trustPlayerField != null) trustPlayerField.active = ownLand && !landLocked;
            }
            if (memberBuildButton != null) {
                memberBuildButton.setMessage(Component.literal("成员建造：" + (landMenu.memberBuild ? "开" : "关")));
                memberInteractButton.setMessage(Component.literal("成员交互：" + (landMenu.memberInteract ? "开" : "关")));
                memberContainerButton.setMessage(Component.literal("容器访问：" + (landMenu.memberContainer ? "开" : "关")));
                memberRedstoneButton.setMessage(Component.literal("红石操作：" + (landMenu.memberRedstone ? "开" : "关")));
                memberBuildButton.active = ownLand && !landLocked;
                memberInteractButton.active = ownLand && !landLocked;
                memberContainerButton.active = ownLand && !landLocked;
                memberRedstoneButton.active = ownLand && !landLocked;
            }
            if (leaseButton != null) {
                boolean hasLeaseInput = leasePlayerField != null && !leasePlayerField.getValue().isBlank()
                        && leaseDaysField != null && !leaseDaysField.getValue().isBlank()
                        && leaseRentField != null && !leaseRentField.getValue().isBlank();
                leaseButton.setMessage(Component.literal(landMenu.leased ? "解除出租" : "出租土地"));
                leaseButton.active = landMenu.hasSelectedChunk && ownLand && !landLocked
                        && (landMenu.leased || hasLeaseInput);
                if (leasePlayerField != null) leasePlayerField.active = ownLand && !landLocked;
                if (leaseDaysField != null) leaseDaysField.active = ownLand && !landLocked;
                if (leaseRentField != null) leaseRentField.active = ownLand && !landLocked;
            }
            if (transferButton != null) {
                boolean hasTarget = transferPlayerField != null && !transferPlayerField.getValue().isBlank();
                transferButton.active = landMenu.hasSelectedChunk && ownLand && !landLocked && !landMenu.leased && hasTarget;
                if (transferPlayerField != null) transferPlayerField.active = ownLand && !landLocked;
            }
            acceptTransferButton.active = !landLocked;
            rejectTransferButton.active = !landLocked;
            if (auctionBidButton != null) {
                auctionBidButton.active = landMenu.auctionActive && !ownLand && landMenu.hasSelectedChunk
                        && auctionBidField != null && auctionBidField.getValue().matches("\\d+");
                if (auctionBidField != null) auctionBidField.active = landMenu.auctionActive && !ownLand;
            }
            int maxLogPage = Math.max(0, (landMenu.landLogs.size() - 1) / 4);
            landLogPage = Math.max(0, Math.min(landLogPage, maxLogPage));
            if (previousLogsButton != null) previousLogsButton.active = landLogPage > 0;
            if (nextLogsButton != null) nextLogsButton.active = landLogPage < maxLogPage;
            }
        graphics.drawString(font, Component.literal("土地系统"), 24, 20, GuiStyles.ACCENT, false);
        int x = mapX + mapWidth + 24;
        int y = mapY;
        graphics.drawString(font, Component.literal("当前区块"), x, y, GuiStyles.ACCENT, false);
        y += 14;
        int displayChunkX = landMenu.hasSelectedChunk ? landMenu.selectedChunkX : landMenu.chunkX;
        int displayChunkZ = landMenu.hasSelectedChunk ? landMenu.selectedChunkZ : landMenu.chunkZ;
        graphics.drawString(font, Component.literal("[" + displayChunkX + ", " + displayChunkZ + "]"), x, y, GuiStyles.TEXT, false);
        y += 14;
        if (!landMenu.claimed) {
            graphics.drawString(font, Component.literal("未登记领地"), x, y, GuiStyles.TEXT, false);
            y += 16;
            graphics.drawString(font, Component.literal("使用 /land claim"), x, y, GuiStyles.TEXT_DIM, false);
            return;
        }
        graphics.drawString(font, Component.literal("用途：" + landMenu.purpose), x, y, GuiStyles.TEXT, false);
        y += 14;
        graphics.drawString(font, Component.literal("资源：" + landMenu.resourceType), x, y, GuiStyles.TEXT, false);
        y += 14;
        graphics.drawString(font, Component.literal("储量：" + landMenu.resourceAmount), x, y, GuiStyles.TEXT, false);
        y += 14;
        graphics.drawString(font, Component.literal("土地税：" + landMenu.taxOwed), x, y, GuiStyles.TEXT, false);
        y += 14;
        int taxStateColor = status == LandStatus.TAX_FROZEN || status == LandStatus.AUCTION ? 0xFFFF6666
                : status == LandStatus.NORMAL ? GuiStyles.TEXT : 0xFFFFC266;
        graphics.drawString(font, Component.literal("土地状态：" + status.displayName()), x, y, taxStateColor, false);
        y += 14;
        if (landMenu.taxDueAt > 0L) {
            long untilDue = landMenu.taxDueAt - currentGameTime;
            long untilGrace = landMenu.taxGraceUntil - currentGameTime;
            String taxStatus = untilDue >= 0L
                    ? formatDeadline("税款到期", landMenu.taxDueAt, currentGameTime)
                    : untilGrace >= 0L
                    ? formatDeadline("宽限期截止", landMenu.taxGraceUntil, currentGameTime)
                    : "已超过宽限期";
            graphics.drawString(font, Component.literal(taxStatus), x, y,
                    untilDue >= 0L ? GuiStyles.TEXT_DIM : 0xFFFF6666, false);
            y += 14;
            if (untilGrace < 0L) {
                long disposalAt = landMenu.taxGraceUntil
                        + com.ailudick.capitalismmod.Config.LAND_TAX_DISPOSAL_DAYS.get() * 24000L;
                graphics.drawString(font, Component.literal(formatDeadline("处置时间", disposalAt, currentGameTime)),
                        x, y, 0xFFFF6666, false);
                y += 14;
            }
        }
        boolean isOwner = mc.player != null && landMenu.ownerUuid.equals(mc.player.getUUID().toString());
        boolean adminBypass = mc.player != null && mc.player.hasPermissions(2)
                && com.ailudick.capitalismmod.Config.LAND_ADMIN_BYPASS.get();
        String role = adminBypass ? "管理员" : isOwner ? "所有者" : landMenu.trusted ? "成员" : "访客";
        graphics.drawString(font, Component.literal("当前身份：" + role), x, y, GuiStyles.TEXT, false);
        y += 14;
        String ownerName = landMenu.ownerUuid;
        if (mc.level != null && !landMenu.ownerUuid.isBlank()) {
            try {
                var owner = mc.level.getPlayerByUUID(java.util.UUID.fromString(landMenu.ownerUuid));
                if (owner != null) ownerName = owner.getGameProfile().getName();
            } catch (IllegalArgumentException ignored) {
                // Keep the UUID if a legacy or malformed owner value is encountered.
            }
        }
        if (ownerName.length() > 18) ownerName = ownerName.substring(0, 18);
        graphics.drawString(font, Component.literal("所有者：" + ownerName), x, y, GuiStyles.TEXT, false);
        y += 14;
        if (!landMenu.ownershipHistory.isEmpty()) {
            graphics.drawString(font, Component.literal("历任所有者：" + String.join(" → ", landMenu.ownershipHistory)),
                    x, y, GuiStyles.TEXT_DIM, false);
            y += 14;
        }
        if (mc.level instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            LandClaim displayClaim = new LandClaim(landMenu.id, landMenu.dimension, displayChunkX, displayChunkZ,
                    java.util.UUID.fromString(landMenu.ownerUuid), landMenu.purpose, landMenu.linkedBusinessId,
                    java.util.List.of(), landMenu.resourceType, landMenu.resourceAmount, landMenu.taxOwed,
                    landMenu.leased ? java.util.UUID.fromString(landMenu.leaseeUuid) : null,
                    landMenu.leaseUntil, landMenu.leaseRent, landMenu.leaseDebt, landMenu.leaseGraceUntil,
                    landMenu.taxDueAt, landMenu.taxGraceUntil);
            graphics.drawString(font, Component.literal("建议价格：" + LandValuationHelper.suggestedPrice(clientLevel, displayClaim)),
                    x, y, GuiStyles.TEXT, false);
            y += 14;
        }
        graphics.drawString(font, Component.literal("成员权限 建造:" + (landMenu.memberBuild ? "开" : "关")
                + " 交互:" + (landMenu.memberInteract ? "开" : "关")), x, y, GuiStyles.TEXT_DIM, false);
        y += 14;
        graphics.drawString(font, Component.literal("容器:" + (landMenu.memberContainer ? "开" : "关")
                + " 红石:" + (landMenu.memberRedstone ? "开" : "关")), x, y, GuiStyles.TEXT_DIM, false);
        y += 14;
        graphics.drawString(font, Component.literal("管理员绕过：" + (adminBypass ? "开启" : "关闭")),
                x, y, GuiStyles.TEXT_DIM, false);
        y += 14;
        if (landMenu.saleActive) {
            long now = currentGameTime;
            graphics.drawString(font, Component.literal("出售中：" + landMenu.saleTarget + " / 价格：" + landMenu.salePrice),
                    x, y, 0xFFFFC266, false);
            y += 14;
            graphics.drawString(font, Component.literal(formatDeadline("出售截止", landMenu.saleExpiresAt, now)),
                    x, y, GuiStyles.TEXT_DIM, false);
            y += 14;
        }
        if (landMenu.auctionActive) {
            graphics.drawString(font, Component.literal("土地拍卖中 起拍价：" + landMenu.auctionStartPrice),
                    x, y, 0xFFFFC266, false);
            y += 14;
            boolean myBid = mc.player != null && !landMenu.auctionBidder.isBlank()
                    && landMenu.auctionBidder.equals(mc.player.getGameProfile().getName());
            graphics.drawString(font, Component.literal(myBid ? "我的状态：当前最高出价者" : "我的状态：未领先"),
                    x, y, myBid ? 0xFF66DD88 : GuiStyles.TEXT_DIM, false);
            y += 14;
            graphics.drawString(font, Component.literal("当前最高价：" + landMenu.auctionHighestBid),
                    x, y, GuiStyles.TEXT, false);
            y += 14;
            if (!myBid) {
                long nextBid = landMenu.auctionHighestBid == Long.MAX_VALUE ? Long.MAX_VALUE : landMenu.auctionHighestBid + 1L;
                graphics.drawString(font, Component.literal("领先最低出价：" + Math.max(landMenu.auctionStartPrice, nextBid)),
                        x, y, GuiStyles.TEXT_DIM, false);
                y += 14;
            }
            graphics.drawString(font, Component.literal("最高出价者：" + landMenu.auctionBidder),
                    x, y, GuiStyles.TEXT_DIM, false);
            y += 14;
            graphics.drawString(font, Component.literal(formatDeadline("拍卖结束", landMenu.auctionEndsAt, currentGameTime)),
                    x, y, GuiStyles.TEXT_DIM, false);
            y += 14;
        }
        graphics.drawString(font, Component.literal(landMenu.leased ? "状态：已出租" : "状态：未出租"), x, y, GuiStyles.TEXT, false);
        if (landMenu.leased) {
            y += 14;
            String leasee = landMenu.leaseeUuid;
            if (leasee.length() > 12) leasee = leasee.substring(0, 12);
            graphics.drawString(font, Component.literal("租客：" + leasee), x, y, GuiStyles.TEXT, false);
            y += 14;
            long now = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
            graphics.drawString(font, Component.literal(formatDeadline("租赁到期", landMenu.leaseUntil, now)), x, y, GuiStyles.TEXT, false);
            y += 14;
            graphics.drawString(font, Component.literal("周期租金：" + landMenu.leaseRent), x, y, GuiStyles.TEXT, false);
        }
        if (landMenu.leased && landMenu.leaseDebt > 0L) {
            y += 14;
            if (landMenu.leaseGraceUntil > 0L) {
                y += 14;
                long now = Minecraft.getInstance().level == null ? 0L : Minecraft.getInstance().level.getGameTime();
                graphics.drawString(font, Component.literal(formatDeadline("租金宽限截止", landMenu.leaseGraceUntil, now)),
                        x, y, 0xFFFFC266, false);
            }
            y += 14;
            graphics.drawString(font, Component.literal("欠租：" + landMenu.leaseDebt), x, y, 0xFFFF8066, false);
        }
        y += 16;
        graphics.drawString(font, Component.literal("信任玩家：" + landMenu.trustedPlayerNames.size()),
                x, y, GuiStyles.TEXT, false);
        for (int i = 0; i < Math.min(landMenu.trustedPlayerNames.size(), 4); i++) {
            y += 12;
            String name = landMenu.trustedPlayerNames.get(i);
            if (name.length() > 18) name = name.substring(0, 18);
            graphics.drawString(font, Component.literal("- " + name), x, y, GuiStyles.TEXT_DIM, false);
        }
        drawOwnedLandListV2(graphics, landMenu);
        drawLandLogs(graphics, landMenu);
    }

    private void toggleMemberPermission(boolean build) {
        if (!(menu instanceof LandMenu landMenu) || !landMenu.claimed || !landMenu.hasSelectedChunk) return;
        if (build) landMenu.memberBuild = !landMenu.memberBuild;
        else landMenu.memberInteract = !landMenu.memberInteract;
        sendMemberPermissions(landMenu);
    }

    private void toggleSpecialPermission(boolean container) {
        if (!(menu instanceof LandMenu landMenu) || !landMenu.claimed || !landMenu.hasSelectedChunk) return;
        if (container) landMenu.memberContainer = !landMenu.memberContainer;
        else landMenu.memberRedstone = !landMenu.memberRedstone;
        sendMemberPermissions(landMenu);
    }

    private void sendMemberPermissions(LandMenu landMenu) {
        PacketDistributor.sendToServer(new SetLandPermissionsPayload(landMenu.dimension,
                landMenu.selectedChunkX, landMenu.selectedChunkZ, landMenu.memberBuild, landMenu.memberInteract,
                landMenu.memberContainer, landMenu.memberRedstone));
    }

    private void drawLandLogs(GuiGraphics graphics, LandMenu landMenu) {
        int x = mapX;
        int y = mapY + mapHeight + 36;
        graphics.drawString(font, Component.literal("最近土地操作"), x, y, GuiStyles.ACCENT, false);
        int from = Math.min(landLogPage * 4, landMenu.landLogs.size());
        for (int i = 0; i < 4 && from + i < landMenu.landLogs.size(); i++) {
            String log = landMenu.landLogs.get(from + i);
            if (log.length() > 42) log = log.substring(0, 42);
            graphics.drawString(font, Component.literal(log), x, y + 12 + i * 11, GuiStyles.TEXT_DIM, false);
        }
    }

    private void drawOwnedLandList(GuiGraphics graphics, LandMenu landMenu) {
        int x = mapX + mapWidth + 24;
        int y = height - 88;
        graphics.fill(x - 4, y - 4, x + 124, height - 12, 0xAA14263A);
        graphics.drawString(font, Component.literal("我的土地 (" + landMenu.ownedLands.size() + ")"), x, y, GuiStyles.ACCENT, false);
        for (int i = 0; i < Math.min(landMenu.ownedLands.size(), 5); i++) {
            var land = landMenu.ownedLands.get(i);
            String text = "[" + land.chunkX() + "," + land.chunkZ() + "]";
            if (land.leased()) text += land.debt() > 0 ? " 欠租" : " 出租";
            if (land.dimension().length() > 12) text = "…" + text;
            int color = land.chunkX() == landMenu.selectedChunkX && land.chunkZ() == landMenu.selectedChunkZ
                    ? GuiStyles.ACCENT : GuiStyles.TEXT;
            graphics.drawString(font, Component.literal(text), x, y + 13 + i * 12, color, false);
        }
        if (landMenu.ownedLands.size() > 5) {
            graphics.drawString(font, Component.literal("还有 " + (landMenu.ownedLands.size() - 5) + " 块..."),
                    x, y + 13 + 5 * 12, GuiStyles.TEXT_DIM, false);
        }
    }

    private boolean clickOwnedLandList(LandMenu landMenu, double mouseX, double mouseY) {
        int x = mapX + mapWidth + 20;
        int y = height - 88;
        if (mouseX < x || mouseX > x + 128 || mouseY < y + 8 || mouseY >= y + 8 + Math.min(landMenu.ownedLands.size(), 5) * 12) return false;
        int index = (int) ((mouseY - (y + 8)) / 12);
        if (index < 0 || index >= landMenu.ownedLands.size() || index >= 5) return false;
        var land = landMenu.ownedLands.get(index);
        landMenu.selectedChunkX = land.chunkX();
        landMenu.selectedChunkZ = land.chunkZ();
        landMenu.hasSelectedChunk = true;
        viewport.centerOn(land.chunkX() * 16.0 + 8.0, land.chunkZ() * 16.0 + 8.0);
        worldMapWidget.requestVisibleTiles(landMenu);
        PacketDistributor.sendToServer(new RequestLandDetailsPayload(land.dimension(), land.chunkX(), land.chunkZ()));
        return true;
    }

    private void drawOwnedLandListV2(GuiGraphics graphics, LandMenu landMenu) {
        int x = mapX + mapWidth + 24;
        int y = height - 106;
        var filtered = filteredOwnedLandsV2(landMenu);
        int maxOffset = Math.max(0, filtered.size() - 5);
        ownedLandOffset = Math.min(ownedLandOffset, maxOffset);
        graphics.fill(x - 4, y - 4, x + 124, height - 12, 0xAA14263A);
        graphics.drawString(font, Component.literal("我的土地 (" + filtered.size() + ")"), x, y, GuiStyles.ACCENT, false);
        graphics.drawString(font, Component.literal((ownedLandAllDimensions ? "全部维度" : "当前维度")
                + " · " + statusFilterNameV2()), x, y + 11, GuiStyles.TEXT_DIM, false);
        int visible = Math.min(filtered.size() - ownedLandOffset, 5);
        for (int i = 0; i < visible; i++) {
            var land = filtered.get(i + ownedLandOffset);
            String text = "[" + land.chunkX() + "," + land.chunkZ() + "]";
            if (land.leased()) text += land.debt() > 0 ? " 欠租" : " 出租";
            if (ownedLandAllDimensions && !land.dimension().equals(landMenu.dimension)) text = "*" + text;
            int color = land.dimension().equals(landMenu.dimension)
                    && land.chunkX() == landMenu.selectedChunkX && land.chunkZ() == landMenu.selectedChunkZ
                    ? GuiStyles.ACCENT : GuiStyles.TEXT;
            graphics.drawString(font, Component.literal(text), x, y + 24 + i * 12, color, false);
        }
        if (filtered.size() > 5) {
            graphics.drawString(font, Component.literal("滚轮浏览 " + (ownedLandOffset + 1) + "-"
                    + Math.min(ownedLandOffset + 5, filtered.size()) + "/" + filtered.size()),
                    x, y + 24 + 5 * 12, GuiStyles.TEXT_DIM, false);
        }
    }

    private java.util.List<com.ailudick.capitalismmod.network.payload.SyncOwnedLandsPayload.LandEntry> filteredOwnedLandsV2(LandMenu landMenu) {
        return landMenu.ownedLands.stream()
                .filter(land -> ownedLandAllDimensions || land.dimension().equals(landMenu.dimension))
                .filter(land -> ownedLandStatusFilter == 0
                        || (ownedLandStatusFilter == 1 && !land.leased())
                        || (ownedLandStatusFilter == 2 && land.leased() && land.debt() <= 0)
                        || (ownedLandStatusFilter == 3 && land.leased() && land.debt() > 0))
                .toList();
    }

    private String statusFilterNameV2() {
        return switch (ownedLandStatusFilter) {
            case 1 -> "未出租";
            case 2 -> "已出租";
            case 3 -> "欠租";
            default -> "全部状态";
        };
    }

    private boolean clickOwnedLandListV2(LandMenu landMenu, double mouseX, double mouseY, int button) {
        int x = mapX + mapWidth + 20;
        int y = height - 106;
        if (mouseX < x || mouseX > x + 128) return false;
        if (mouseY >= y - 2 && mouseY < y + 10 && button == 0) {
            ownedLandAllDimensions = !ownedLandAllDimensions;
            ownedLandOffset = 0;
            return true;
        }
        if (mouseY >= y + 10 && mouseY < y + 22 && button == 0) {
            ownedLandStatusFilter = (ownedLandStatusFilter + 1) % 4;
            ownedLandOffset = 0;
            return true;
        }
        if (button != 0) return false;
        var filtered = filteredOwnedLandsV2(landMenu);
        if (mouseY < y + 22 || mouseY >= y + 22 + Math.min(filtered.size() - ownedLandOffset, 5) * 12) return false;
        int index = (int) ((mouseY - (y + 22)) / 12) + ownedLandOffset;
        if (index < 0 || index >= filtered.size() || index >= ownedLandOffset + 5) return false;
        var land = filtered.get(index);
        if (!land.dimension().equals(landMenu.dimension)) return true;
        landMenu.selectedChunkX = land.chunkX();
        landMenu.selectedChunkZ = land.chunkZ();
        landMenu.hasSelectedChunk = true;
        viewport.centerOn(land.chunkX() * 16.0 + 8.0, land.chunkZ() * 16.0 + 8.0);
        worldMapWidget.requestVisibleTiles(landMenu);
        PacketDistributor.sendToServer(new RequestLandDetailsPayload(land.dimension(), land.chunkX(), land.chunkZ()));
        return true;
    }

    private void transferLand() {
        if (!(menu instanceof LandMenu landMenu) || transferPlayerField == null
                || transferPlayerField.getValue().isBlank() || !landMenu.hasSelectedChunk
                || Minecraft.getInstance().player == null) return;
        Minecraft.getInstance().player.connection.sendCommand("land transfer " + transferPlayerField.getValue().trim()
                + " " + landMenu.selectedChunkX + " " + landMenu.selectedChunkZ);
    }

    private void acceptTransfer() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed && Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.connection.sendCommand("land accepttransfer");
                }
                Minecraft.getInstance().setScreen(this);
            }, Component.literal("确认接受土地转让？"), Component.literal("接受后将支付出售价格并获得土地所有权")));
        }
    }

    private void rejectTransfer() {
        if (Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.connection.sendCommand("land rejecttransfer");
        }
    }

    private void placeAuctionBid() {
        if (auctionBidField == null || !auctionBidField.getValue().matches("\\d+")
                || !(menu instanceof LandMenu landMenu) || !landMenu.auctionActive) return;
        long price;
        try {
            price = Long.parseLong(auctionBidField.getValue());
        } catch (NumberFormatException ignored) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendCommand("land bid " + landMenu.selectedChunkX + " "
                    + landMenu.selectedChunkZ + " " + price);
            auctionBidField.setValue("");
        }
    }

    private static String formatDeadline(String label, long deadline, long now) {
        if (deadline <= 0L) return label + "：未设置";
        long remaining = Math.max(0L, deadline - now);
        return label + "：" + PerpetualCalendar.formatMinecraftTicks(deadline)
                + "（剩余 " + formatRemainingTime(remaining) + "）";
    }

    private static String formatRemainingTime(long ticks) {
        long days = ticks / PerpetualCalendar.TICKS_PER_DAY;
        long hours = (ticks % PerpetualCalendar.TICKS_PER_DAY) / 1_000L;
        long minutes = (ticks % 1_000L) * 60L / 1_000L;
        long seconds = (ticks % 1_000L) * 60L * 60L / 1_000L;
        if (days > 0L) return days + "天" + hours + "小时";
        if (hours > 0L) return hours + "小时" + minutes + "分钟";
        if (minutes > 0L) return minutes + "分钟" + seconds + "秒";
        return seconds + "秒";
    }

    private void drawMap(GuiGraphics graphics) {
        worldMapWidget.centerOnPlayer();
        worldMapWidget.requestVisibleTiles(menu);
        if (!standaloneMap) {
            graphics.drawString(font, Component.literal("地形地图"), mapX, mapY - 18, GuiStyles.TEXT, false);
        }
        graphics.fill(mapX - 2, mapY - 2, mapX + mapWidth + 2,
                mapY + mapHeight + 2, 0xCC101820);
        graphics.enableScissor(leftPos + mapX, topPos + mapY,
                leftPos + mapX + mapWidth, topPos + mapY + mapHeight);
        drawTerrain(graphics);
        if (!standaloneMap) {
            worldMapWidget.drawChunkGrid(graphics, (LandMenu) menu);
            worldMapWidget.drawLandOverlay(graphics, (LandMenu) menu);
        } else if (menu instanceof WorldMapMenu worldMapMenu) {
            worldMapWidget.drawWorldMapLandOverlay(graphics, worldMapMenu,
                    worldMapWidget.hoveredChunkX(), worldMapWidget.hoveredChunkZ(),
                    worldMapWidget.hasHoveredChunk());
        }
        worldMapWidget.drawPlayerMarker(graphics);
        graphics.disableScissor();
        graphics.drawString(font,
                Component.literal(String.format(Locale.ROOT, "缩放：×%.4f", viewport.zoom())),
                mapX + 6, mapY + 6, GuiStyles.TEXT, true);
        if (standaloneMap && menu instanceof WorldMapMenu worldMapMenu) {
            worldMapWidget.drawSelectionInfo(graphics, font, worldMapMenu,
                    worldMapWidget.hoveredChunkX(), worldMapWidget.hoveredChunkZ(),
                    worldMapWidget.hasHoveredChunk());
        }
        if (!standaloneMap) {
            graphics.drawString(font, Component.literal("地形 · 绿色：我的  红色：他人"), mapX, mapY + mapHeight + 10, GuiStyles.TEXT_DIM, false);
            graphics.drawString(font, Component.literal("滚轮缩放 · 左键拖拽 · 未加载区块为灰色"), mapX, mapY + mapHeight + 22, GuiStyles.TEXT_DIM, false);
        }
    }

    private void drawTerrain(GuiGraphics graphics) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            graphics.fill(mapX, mapY, mapX + mapWidth, mapY + mapHeight, 0xFF263746);
            return;
        }
        if (standaloneMap && menu instanceof WorldMapMenu worldMapMenu) {
            worldMapWidget.drawDiscoveredTerrain(graphics, worldMapMenu);
        } else if (!standaloneMap && menu instanceof LandMenu landMenu) {
            worldMapWidget.drawDiscoveredTerrain(graphics, landMenu);
        }
    }

    private void claimSelectedChunk() {
        if (!(menu instanceof LandMenu landMenu) || !landMenu.hasSelectedChunk) return;
        boolean ownLand = landMenu.claimed && Minecraft.getInstance().player != null
                && landMenu.ownerUuid.equals(Minecraft.getInstance().player.getUUID().toString());
        if (landMenu.claimed) {
            if (!ownLand) return;
            int chunkX = landMenu.selectedChunkX;
            int chunkZ = landMenu.selectedChunkZ;
            String dimension = landMenu.dimension;
            Minecraft.getInstance().setScreen(new ConfirmScreen(confirmed -> {
                if (confirmed) {
                    PacketDistributor.sendToServer(new ReleaseLandPayload(dimension, chunkX, chunkZ));
                }
                Minecraft.getInstance().setScreen(this);
            }, Component.literal("确认放弃土地"),
                    Component.literal("确定要放弃区块 [" + chunkX + ", " + chunkZ + "] 吗？土地数据将被删除。")));
        } else {
            PacketDistributor.sendToServer(new ClaimLandPayload(landMenu.dimension,
                    landMenu.selectedChunkX, landMenu.selectedChunkZ));
        }
    }

    private void nextPurpose() {
        if (!(menu instanceof LandMenu landMenu) || !landMenu.hasSelectedChunk
                || !landMenu.claimed || Minecraft.getInstance().player == null
                || !landMenu.ownerUuid.equals(Minecraft.getInstance().player.getUUID().toString())) return;
        var purposes = LandPurpose.all();
        int current = 0;
        for (int i = 0; i < purposes.size(); i++) {
            if (purposes.get(i).code().equals(landMenu.purpose)) {
                current = i;
                break;
            }
        }
        String next = purposes.get((current + 1) % purposes.size()).code();
        PacketDistributor.sendToServer(new SetLandPurposePayload(landMenu.dimension,
                landMenu.selectedChunkX, landMenu.selectedChunkZ, next));
    }

    private void manageTrust(boolean add) {
        if (!(menu instanceof LandMenu landMenu) || trustPlayerField == null
                || !landMenu.hasSelectedChunk || !landMenu.claimed
                || trustPlayerField.getValue().isBlank()) return;
        PacketDistributor.sendToServer(new ManageLandTrustPayload(landMenu.dimension,
                landMenu.selectedChunkX, landMenu.selectedChunkZ, trustPlayerField.getValue().trim(), add));
    }

    private void leaseAction() {
        if (menu instanceof LandMenu landMenu && landMenu.leased) {
            confirmUnlease(landMenu);
            return;
        }
        leaseLand();
    }

    private void confirmUnlease(LandMenu landMenu) {
        int chunkX = landMenu.selectedChunkX;
        int chunkZ = landMenu.selectedChunkZ;
        String dimension = landMenu.dimension;
        Minecraft.getInstance().setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                PacketDistributor.sendToServer(new UnleaseLandPayload(dimension, chunkX, chunkZ));
            }
            Minecraft.getInstance().setScreen(this);
        }, Component.literal("确认解除出租"),
                Component.literal("确定要解除区块 [" + chunkX + ", " + chunkZ + "] 的租约吗？")));
    }

    private void leaseLand() {
        if (!(menu instanceof LandMenu landMenu) || leasePlayerField == null || leaseDaysField == null
                || leaseRentField == null || !landMenu.hasSelectedChunk || !landMenu.claimed
                || landMenu.leased || leasePlayerField.getValue().isBlank()) return;
        try {
            long days = Long.parseLong(leaseDaysField.getValue());
            long rent = Long.parseLong(leaseRentField.getValue());
            if (days <= 0 || rent < 0) return;
            PacketDistributor.sendToServer(new LeaseLandPayload(landMenu.dimension,
                    landMenu.selectedChunkX, landMenu.selectedChunkZ,
                    leasePlayerField.getValue().trim(), days, rent));
        } catch (NumberFormatException ignored) {
        }
    }





    @Override
    public void removed() {
        if (standaloneMap) WorldMapClientState.save(viewport);
        worldMapWidget.close();
        super.removed();
    }

    protected boolean isInsideMap(double mouseX, double mouseY) {
        return worldMapWidget.contains(mouseX, mouseY, leftPos, topPos);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!standaloneMap && menu instanceof LandMenu landMenu
                && mouseX >= mapX + mapWidth + 20 && mouseX <= mapX + mapWidth + 152
                && mouseY >= height - 106 && mouseY < height - 12) {
            int maxOffset = Math.max(0, filteredOwnedLandsV2(landMenu).size() - 5);
            ownedLandOffset = Math.max(0, Math.min(maxOffset, ownedLandOffset + (scrollY < 0 ? 1 : -1)));
            return true;
        }
        if (worldMapWidget.mouseScrolled(menu, mouseX, mouseY, scrollY, leftPos, topPos)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!standaloneMap && menu instanceof LandMenu landMenu && clickOwnedLandListV2(landMenu, mouseX, mouseY, button)) return true;
        if (worldMapWidget.mouseClicked(menu, mouseX, mouseY, button, leftPos, topPos)) return true;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (worldMapWidget.mouseReleased(menu, mouseX, mouseY, button, leftPos, topPos)) return true;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (worldMapWidget.mouseDragged(mouseX, mouseY, button)) return true;
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }
}
