package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.company.Company;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.init.ModDataComponents;
import com.ailudick.capitalismmod.item.Invoice;
import com.ailudick.capitalismmod.menu.TaxBureauMenu;
import com.ailudick.capitalismmod.network.payload.PayTaxPayload;
import com.ailudick.capitalismmod.network.payload.RedeemInvoicesPayload;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Map;

public class TaxBureauScreen extends AbstractContainerScreen<TaxBureauMenu> {
    private long lastTaxTotal = -1;

    public TaxBureauScreen(TaxBureauMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageHeight = 200;
    }

    @Override
    protected void init() {
        super.init();
        refreshWidgets();
    }

    private void refreshWidgets() {
        clearWidgets();
        int i = 0;
        for (Map.Entry<String, Company> entry : menu.getCompanies().entrySet()) {
            Company company = entry.getValue();
            if (company.taxOwed() > 0) {
                String name = entry.getKey();
                addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.pay_tax"), btn -> payTax(name))
                        .bounds(leftPos + 128, topPos + 28 + i * 24, 40, 20).build());
            }
            i++;
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.capitalismmod.redeem_invoices"), btn -> redeem())
                .bounds(leftPos + 8, topPos + 170, 100, 20).build());
        lastTaxTotal = taxTotal();
    }

    private long taxTotal() {
        long total = 0;
        for (Company company : menu.getCompanies().values()) {
            total += company.taxOwed();
        }
        return total;
    }

    private void payTax(String name) {
        PacketDistributor.sendToServer(new PayTaxPayload(name));
    }

    private void redeem() {
        PacketDistributor.sendToServer(new RedeemInvoicesPayload());
    }

    private long invoiceTotal() {
        long total = 0;
        if (minecraft == null || minecraft.player == null) {
            return 0;
        }
        for (ItemStack stack : minecraft.player.getInventory().items) {
            if (stack.getItem() instanceof Invoice && stack.has(ModDataComponents.INVOICE_AMOUNT.get())) {
                total += stack.get(ModDataComponents.INVOICE_AMOUNT.get()) * stack.getCount();
            }
        }
        return total;
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
        if (taxTotal() != lastTaxTotal) {
            refreshWidgets();
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);

        graphics.drawString(font, title, leftPos + 8, topPos + 6, GuiStyles.ACCENT, false);

        int i = 0;
        for (Map.Entry<String, Company> entry : menu.getCompanies().entrySet()) {
            Company company = entry.getValue();
            if (company.taxOwed() > 0) {
                String taxOwed = Component.translatable("gui.capitalismmod.tax_owed").getString();
                graphics.drawString(font, entry.getKey() + " Lv." + company.level() + " · " + taxOwed + " $" + company.taxOwed(),
                        leftPos + 8, topPos + 30 + i * 24, GuiStyles.TEXT, false);
            }
            i++;
        }

        graphics.drawString(font, Component.translatable("gui.capitalismmod.invoice_total", Money.format(invoiceTotal())),
                leftPos + 8, topPos + 150, GuiStyles.TEXT, false);
    }
}
