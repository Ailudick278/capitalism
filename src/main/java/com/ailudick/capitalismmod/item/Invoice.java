package com.ailudick.capitalismmod.item;

import net.minecraft.world.item.Item;

/**
 * An invoice (发票) issued on a purchase. Its amount is stored in a data component
 * and is reserved for a future tax deduction or company reimbursement flow.
 */
public class Invoice extends Item {
    public Invoice(Properties properties) {
        super(properties);
    }
}
