package com.ailudick.capitalismmod;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Deposit interest rate per Minecraft year (365 days), compounded daily (0.05 = 5% per year).
    public static final ModConfigSpec.DoubleValue DEPOSIT_RATE_PER_YEAR = BUILDER
            .comment("Deposit interest rate per Minecraft year (365 days), compounded daily (0.05 = 5% per year).")
            .defineInRange("depositRatePerYear", 0.05, 0.0, 1.0);

    // Loan interest rate per Minecraft year (365 days), compounded daily (0.10 = 10% per year).
    public static final ModConfigSpec.DoubleValue LOAN_RATE_PER_YEAR = BUILDER
            .comment("Loan interest rate per Minecraft year (365 days), compounded daily (0.10 = 10% per year).")
            .defineInRange("loanRatePerYear", 0.10, 0.0, 1.0);

    // Term deposit (fixed-term) interest rate per Minecraft year (0.08 = 8% per year).
    public static final ModConfigSpec.DoubleValue TERM_DEPOSIT_RATE_PER_YEAR = BUILDER
            .comment("Term deposit interest rate per Minecraft year (0.08 = 8% per year).")
            .defineInRange("termDepositRatePerYear", 0.08, 0.0, 1.0);

    // Finance-company annual return on treasury, distributed once per production cycle.
    public static final ModConfigSpec.DoubleValue FINANCE_RATE_PER_YEAR = BUILDER
            .comment("Annual return for finance companies, distributed over Minecraft days and production cycles.")
            .defineInRange("financeRatePerYear", 0.05, 0.0, 1.0);

    // Corporate income tax rate applied to company income (0.25 = 25%).
    public static final ModConfigSpec.DoubleValue INCOME_TAX_RATE = BUILDER
            .comment("Corporate income tax rate applied to company income (0.25 = 25%).")
            .defineInRange("incomeTaxRate", 0.25, 0.0, 1.0);

    // Operating cost in USD per company level and production cycle.
    public static final ModConfigSpec.LongValue COMPANY_MAINTENANCE_PER_LEVEL = BUILDER
            .comment("Company maintenance cost in USD per level and production cycle; 0 disables it.")
            .defineInRange("companyMaintenancePerLevel", 1L, 0L, Long.MAX_VALUE);

    // Fraction of invoice value refunded at the tax bureau (0.10 = 10%).
    public static final ModConfigSpec.DoubleValue INVOICE_REFUND_RATE = BUILDER
            .comment("Fraction of invoice value refunded at the tax bureau (0.10 = 10%).")
            .defineInRange("invoiceRefundRate", 0.10, 0.0, 1.0);

    // Credit limit in base units (yuan fen). 100000 = 1000 yuan.
    public static final ModConfigSpec.LongValue CREDIT_LIMIT = BUILDER
            .comment("Credit limit for credit accounts, in base units (yuan fen). 100000 = 1000 yuan.")
            .defineInRange("creditLimit", 100000L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue LOAN_TERM_DAYS = BUILDER
            .comment("Number of Minecraft days before a bank loan becomes overdue.")
            .defineInRange("loanTermDays", 30, 1, 3650);

    public static final ModConfigSpec.IntValue TERM_DEPOSIT_MAX_DAYS = BUILDER
            .comment("Maximum number of Minecraft days for a fixed-term deposit.")
            .defineInRange("termDepositMaxDays", 3650, 1, 3650);

    public static final ModConfigSpec.DoubleValue TRANSFER_FEE_RATE = BUILDER
            .comment("Inter-account transfer fee as a fraction of the amount (0.001 = 0.1%).")
            .defineInRange("transferFeeRate", 0.001, 0.0, 1.0);

    // Width/depth of one trade region in blocks.
    public static final ModConfigSpec.IntValue TRADE_REGION_SIZE = BUILDER
            .comment("Trade region size in blocks used for regional logistics.")
            .defineInRange("tradeRegionSize", 512, 32, 16384);

    // Transport time per region crossed, in ticks; 12000 ticks = 10 Minecraft minutes.
    public static final ModConfigSpec.LongValue REGIONAL_SHIPPING_TICKS = BUILDER
            .comment("Transport time per region crossed in ticks; 12000 ticks is 10 Minecraft minutes.")
            .defineInRange("regionalShippingTicks", 12000L, 20L, 240000L);

    // Daily commodity price limit band as a fraction of the previous close (0.10 = ±10%).
    public static final ModConfigSpec.DoubleValue COMMODITY_PRICE_LIMIT = BUILDER
            .comment("Commodity price limit band as a fraction of the previous close (0.10 = ±10%).")
            .defineInRange("commodityPriceLimit", 0.10, 0.0, 1.0);

    // Daily stock price limit band as a fraction of the previous close (0.10 = 10%).
    public static final ModConfigSpec.DoubleValue STOCK_PRICE_LIMIT = BUILDER
            .comment("Stock price limit band as a fraction of the previous close (0.10 = 10%).")
            .defineInRange("stockPriceLimit", 0.10, 0.0, 1.0);

    // Futures margin rate (fraction of notional value required as margin). 0.10 = 10x leverage.
    public static final ModConfigSpec.DoubleValue FUTURES_MARGIN_RATE = BUILDER
            .comment("Futures margin rate (fraction of notional value required as margin). 0.10 = 10x leverage.")
            .defineInRange("futuresMarginRate", 0.10, 0.01, 1.0);

    // Number of Minecraft days until a futures contract expires.
    public static final ModConfigSpec.IntValue FUTURES_EXPIRY_DAYS = BUILDER
            .comment("Number of Minecraft days until a futures contract expires.")
            .defineInRange("futuresExpiryDays", 7, 1, 365);

    // Face value (USD) of one government bond.
    public static final ModConfigSpec.LongValue BOND_FACE_VALUE = BUILDER
            .comment("Face value (USD) of one government bond.")
            .defineInRange("bondFaceValue", 100L, 1L, Long.MAX_VALUE);

    // Bond annual coupon rate (fraction, 0.05 = 5%).
    public static final ModConfigSpec.DoubleValue BOND_RATE_PER_YEAR = BUILDER
            .comment("Bond annual coupon rate (fraction, 0.05 = 5%).")
            .defineInRange("bondRatePerYear", 0.05, 0.0, 1.0);

    // Bond term in Minecraft days.
    public static final ModConfigSpec.IntValue BOND_MATURITY_DAYS = BUILDER
            .comment("Bond term in Minecraft days.")
            .defineInRange("bondMaturityDays", 30, 1, 3650);

    static final ModConfigSpec SPEC = BUILDER.build();
}
