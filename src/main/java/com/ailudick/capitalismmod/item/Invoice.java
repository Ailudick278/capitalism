package com.ailudick.capitalismmod.item;

import net.minecraft.world.item.Item;

/**
 * An invoice (发票) issued on a purchase. Its amount is stored in a data component
 * and can be reimbursed (报销) at the tax bureau for a partial refund.
 */
public class Invoice extends Item {
    public Invoice(Properties properties) {
        super(properties);
    }
}
