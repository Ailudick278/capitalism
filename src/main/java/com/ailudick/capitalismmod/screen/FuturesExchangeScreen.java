package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.futures.Position;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.menu.FuturesExchangeMenu;
import com.ailudick.capitalismmod.network.payload.CloseFuturesPositionPayload;
import com.ailudick.capitalismmod.network.payload.DepositMarginPayload;
import com.ailudick.capitalismmod.network.payload.OpenFuturesPositionPayload;
import com.ailudick.capitalismmod.network.payload.WithdrawMarginPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class FuturesExchangeScreen extends AbstractContainerScreen<FuturesExchangeMenu> {
    private int selectedCommodity = 0;
    private EditBox quantityField;
    private List<Position> builtPositions = new ArrayList<>();

    public FuturesExchangeScreen(FuturesExchangeMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 200;
        this.imageHeight = 230;
    }

    @Override
    protected void init() {
        super.init();
        refreshWidgets();
    }

    private void refreshWidgets() {
        String qty = this.quantityField != null ? this.quantityField.getValue() : "1";
        clearWidgets();
        builtPositions = new ArrayList<>(menu.getPositions());

        for (int i = 0; i < Commodities.ALL.size(); i++) {
            final int index = i;
            int row = i / 3;
            int col = i % 3;
            addRenderableWidget(Button.builder(Commodities.get(i).getHoverName(), btn -> this.selectedCommodity = index)
                    .bounds(leftPos + 8 + col * 55, topPos + 4 + row * 22, 50, 20).build());
        }

        this.quantityField = new EditBox(font, leftPos + 8, topPos + 168, 40, 20, Component.literal("1"));
        this.quantityField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.quantityField.setValue(qty.isEmpty() ? "1" : qty);
        addRenderableWidget(this.quantityField);

        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.open_long"), btn -> open(true))
                .bounds(leftPos + 52, topPos + 168, 40, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.open_short"), btn -> open(false))
                .bounds(leftPos + 96, topPos + 168, 40, 20).build());

        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.deposit"), btn -> deposit())
                .bounds(leftPos + 8, topPos + 190, 40, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.withdraw"), btn -> withdraw())
                .bounds(leftPos + 52, topPos + 190, 40, 20).build());

        for (int i = 0; i < Math.min(4, menu.getPositions().size()); i++) {
            final Position position = menu.getPositions().get(i);
            addRenderableWidget(Button.builder(Component.literal("X"), btn -> close(position.id()))
                    .bounds(leftPos + 160, topPos + 92 + i * 18, 20, 16).build());
        }
    }

    private void open(boolean longSide) {
        try {
            int quantity = Integer.parseInt(this.quantityField.getValue());
            if (quantity > 0) {
                PacketDistributor.sendToServer(new OpenFuturesPositionPayload(selectedCommodity, quantity, longSide));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void close(String positionId) {
        PacketDistributor.sendToServer(new CloseFuturesPositionPayload(positionId));
    }

    private void deposit() {
        try {
            long amount = Long.parseLong(this.quantityField.getValue());
            if (amount > 0) {
                PacketDistributor.sendToServer(new DepositMarginPayload(amount));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void withdraw() {
        try {
            long amount = Long.parseLong(this.quantityField.getValue());
            if (amount > 0) {
                PacketDistributor.sendToServer(new WithdrawMarginPayload(amount));
            }
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        GuiStyles.drawBackground(graphics, leftPos, topPos, imageWidth, imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!menu.getPositions().equals(builtPositions)) {
            refreshWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        String itemId = Commodities.id(Commodities.get(selectedCommodity));
        long price = menu.getPrices().getOrDefault(itemId, 0L);
        long days = menu.getDaysToExpiry().getOrDefault(itemId, 0L);

        graphics.drawString(font, Component.translatable("gui.capitalismmod.futures_price", price),
                leftPos + 8, topPos + 50, GuiStyles.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.capitalismmod.days_to_expiry", days),
                leftPos + 120, topPos + 50, GuiStyles.TEXT, false);
        graphics.drawString(font, Component.translatable("gui.capitalismmod.margin_balance", menu.getMarginBalance()),
                leftPos + 8, topPos + 64, GuiStyles.TEXT, false);

        graphics.drawString(font, Component.translatable("gui.capitalismmod.positions"),
                leftPos + 8, topPos + 80, GuiStyles.TEXT, false);
        for (int i = 0; i < Math.min(4, menu.getPositions().size()); i++) {
            Position position = menu.getPositions().get(i);
            long markPrice = menu.getPrices().getOrDefault(position.itemId(), position.entryPrice());
            long diff = position.longSide() ? markPrice - position.entryPrice() : position.entryPrice() - markPrice;
            long pnl = (long) position.quantity() * diff;
            String label = (position.longSide() ? "L " : "S ") + position.quantity() + " @" + position.entryPrice()
                    + "  " + (pnl >= 0 ? "+" : "") + pnl;
            graphics.drawString(font, Component.literal(label), leftPos + 8, topPos + 92 + i * 18, GuiStyles.TEXT, false);
        }
    }
}
