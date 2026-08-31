package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.market.Commodities;
import com.ailudick.capitalismmod.menu.ProcurementMenu;
import com.ailudick.capitalismmod.network.payload.PlaceSupplyOrderPayload;
import com.ailudick.capitalismmod.supply.PurchaseOrder;
import com.ailudick.capitalismmod.supply.SupplyOffer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

public class ProcurementScreen extends AbstractContainerScreen<ProcurementMenu> {
    private int selectedCommodity = 0;
    private EditBox quantityField;
    private List<SupplyOffer> builtOffers = new ArrayList<>();
    private List<PurchaseOrder> builtOrders = new ArrayList<>();

    public ProcurementScreen(ProcurementMenu menu, Inventory inventory, Component title) {
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
        builtOffers = new ArrayList<>(menu.getOffers());
        builtOrders = new ArrayList<>(menu.getOrders());

        for (int i = 0; i < Commodities.ALL.size(); i++) {
            final int index = i;
            int row = i / 3;
            int col = i % 3;
            addRenderableWidget(Button.builder(Commodities.get(i).getHoverName(), btn -> this.selectedCommodity = index)
                    .bounds(leftPos + 8 + col * 55, topPos + 4 + row * 22, 50, 20).build());
        }

        this.quantityField = new EditBox(font, leftPos + 8, topPos + 50, 40, 20, Component.literal("1"));
        this.quantityField.setFilter(s -> s.isEmpty() || s.matches("\\d+"));
        this.quantityField.setValue(qty.isEmpty() ? "1" : qty);
        addRenderableWidget(this.quantityField);

        List<SupplyOffer> offers = offersForSelected();
        for (int i = 0; i < Math.min(4, offers.size()); i++) {
            final SupplyOffer offer = offers.get(i);
            addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.place_order"), btn -> placeOrder(offer))
                    .bounds(leftPos + 160, topPos + 86 + i * 18, 30, 16).build());
        }
    }

    private List<SupplyOffer> offersForSelected() {
        String itemId = Commodities.id(Commodities.get(selectedCommodity));
        List<SupplyOffer> result = new ArrayList<>();
        for (SupplyOffer offer : menu.getOffers()) {
            if (offer.itemId().equals(itemId)) {
                result.add(offer);
            }
        }
        return result;
    }

    private void placeOrder(SupplyOffer offer) {
        try {
            int quantity = Integer.parseInt(this.quantityField.getValue());
            if (quantity > 0) {
                PacketDistributor.sendToServer(new PlaceSupplyOrderPayload(offer.id(), quantity));
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
        if (!menu.getOffers().equals(builtOffers) || !menu.getOrders().equals(builtOrders)) {
            refreshWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(font, Component.translatable("gui.capitalismmod.suppliers"),
                leftPos + 8, topPos + 74, GuiStyles.TEXT, false);
        List<SupplyOffer> offers = offersForSelected();
        for (int i = 0; i < Math.min(4, offers.size()); i++) {
            SupplyOffer offer = offers.get(i);
            graphics.drawString(font, Component.literal(offer.companyName() + " $" + offer.price()),
                    leftPos + 8, topPos + 86 + i * 18, GuiStyles.TEXT, false);
        }

        graphics.drawString(font, Component.translatable("gui.capitalismmod.my_orders"),
                leftPos + 8, topPos + 164, GuiStyles.TEXT, false);
        for (int i = 0; i < Math.min(3, menu.getOrders().size()); i++) {
            PurchaseOrder order = menu.getOrders().get(i);
            graphics.drawString(font, Component.literal(order.companyName() + " " + order.remaining() + "x"),
                    leftPos + 8, topPos + 176 + i * 18, GuiStyles.TEXT, false);
        }
    }
}
