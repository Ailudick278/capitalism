package com.ailudick.capitalismmod;

import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Currencies;
import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue WORLD_MAP_STANDALONE_ENABLED = BUILDER
            .comment("Whether the world map can be opened through its standalone keybinding; embedded land maps are unaffected.")
            .define("worldMapStandaloneEnabled", false);

    public static final ModConfigSpec.DoubleValue WORLD_MAP_MIN_ZOOM = BUILDER
            .comment("Minimum world-map zoom, measured in screen pixels per world block.")
            .defineInRange("worldMapMinZoom", 0.5, 0.0001, 16.0);

    public static final ModConfigSpec.DoubleValue WORLD_MAP_MAX_ZOOM = BUILDER
            .comment("Maximum world-map zoom, measured in screen pixels per world block.")
            .defineInRange("worldMapMaxZoom", 8.0, 0.0001, 16.0);

    public static final ModConfigSpec.IntValue WORLD_MAP_DISCOVERY_RADIUS = BUILDER
            .comment("Radius in chunks explored around the player for the world map; 8 means 17x17 chunks.")
            .defineInRange("worldMapDiscoveryRadius", 8, 1, 24);

    public static final ModConfigSpec.IntValue MAX_LAND_CLAIMS = BUILDER
            .comment("Maximum number of land chunks one player may own per dimension.")
            .defineInRange("maxLandClaims", 9, 1, 1024);

    public static final ModConfigSpec.BooleanValue REQUIRE_ADJACENT_LAND_CLAIMS = BUILDER
            .comment("Whether new land claims must be directly adjacent to the player's existing land.")
            .define("requireAdjacentLandClaims", false);

    public static final ModConfigSpec.BooleanValue LAND_ADMIN_BYPASS = BUILDER
            .comment("Whether operators with permission level 2 or higher bypass player land protections.")
            .define("landAdminBypass", true);

    public static final ModConfigSpec.LongValue LAND_CLAIM_PRICE = BUILDER
            .comment("CNY minor units charged when claiming one new land chunk; 100 minor units = 1 yuan.")
            .defineInRange("landClaimPrice", 1000L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue LAND_RELEASE_REFUND_RATE = BUILDER
            .comment("Fraction of the claim price refunded when releasing land.")
            .defineInRange("landReleaseRefundRate", 0.8, 0.0, 1.0);

    public static final ModConfigSpec.LongValue LAND_TRANSFER_PRICE = BUILDER
            .comment("CNY minor units charged to the recipient when accepting a land transfer.")
            .defineInRange("landTransferPrice", 2000L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.DoubleValue LAND_TAX_RATE_PER_YEAR = BUILDER
            .comment("Annual land tax rate applied to the suggested land value.")
            .defineInRange("landTaxRatePerYear", 0.05, 0.0, 1.0);

    public static final ModConfigSpec.IntValue LAND_TAX_PERIOD_DAYS = BUILDER
            .comment("Number of Minecraft days between land tax due dates.")
            .defineInRange("landTaxPeriodDays", 30, 1, 3650);

    public static final ModConfigSpec.IntValue LAND_TAX_GRACE_DAYS = BUILDER
            .comment("Grace period in Minecraft days after land tax is due.")
            .defineInRange("landTaxGraceDays", 7, 0, 3650);

    public static final ModConfigSpec.IntValue LAND_TAX_DISPOSAL_DAYS = BUILDER
            .comment("Days after the tax grace period before a land claim enters final disposal review.")
            .defineInRange("landTaxDisposalDays", 14, 1, 3650);

    public static final ModConfigSpec.DoubleValue LAND_AUCTION_START_RATE = BUILDER
            .comment("Auction starting price as a fraction of the suggested land value.")
            .defineInRange("landAuctionStartRate", 0.5, 0.0, 10.0);

    public static final ModConfigSpec.IntValue LAND_AUCTION_DURATION_DAYS = BUILDER
            .comment("Duration of a land auction in Minecraft days.")
            .defineInRange("landAuctionDurationDays", 7, 1, 3650);

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

    public static final ModConfigSpec.DoubleValue INDIVIDUAL_INCOME_TAX_RATE = BUILDER
            .comment("Sole-proprietor income tax rate applied to completed sales.")
            .defineInRange("individualIncomeTaxRate", 0.20, 0.0, 1.0);

    // Operating cost in USD per company level and production cycle.
    public static final ModConfigSpec.LongValue COMPANY_MAINTENANCE_PER_LEVEL = BUILDER
            .comment("Company maintenance cost in USD per level and production cycle; 0 disables it.")
            .defineInRange("companyMaintenancePerLevel", 1L, 0L, Long.MAX_VALUE);

    // Credit limit in base units (yuan fen). 100000 = 1000 yuan.
    public static final ModConfigSpec.LongValue CREDIT_LIMIT = BUILDER
            .comment("Credit limit for credit accounts, in base units (yuan fen). 100000 = 1000 yuan.")
            .defineInRange("creditLimit", 100000L, 0L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue MAX_DEBIT_ACCOUNTS = BUILDER
            .comment("Maximum number of debit accounts one player may open.")
            .defineInRange("maxDebitAccounts", 3, 1, 16);

    public static final ModConfigSpec.IntValue MAX_CREDIT_ACCOUNTS = BUILDER
            .comment("Maximum number of credit accounts one player may open.")
            .defineInRange("maxCreditAccounts", 1, 1, 16);

    public static final ModConfigSpec.ConfigValue<String> CROSS_BORDER_BASE_CURRENCY = BUILDER
            .comment("Base currency used by cross-border services, for example cny, usd, eur, or rub.")
            .define("crossBorderBaseCurrency", "cny");

    public static final ModConfigSpec.ConfigValue<String> DEFAULT_CURRENCY = BUILDER
            .comment("Default accounting currency used by land, tax and other general economic systems.")
            .define("defaultCurrency", "cny");

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

    public static final ModConfigSpec.DoubleValue LOGISTICS_RISK_RATE = BUILDER
            .comment("Base probability of cargo damage when a shipment reaches its destination.")
            .defineInRange("logisticsRiskRate", 0.03, 0.0, 1.0);

    public static final ModConfigSpec.LongValue LOGISTICS_DISRUPTION_TICKS = BUILDER
            .comment("Extra delay caused by a logistics disruption.")
            .defineInRange("logisticsDisruptionTicks", 2400L, 20L, 240000L);

    public static final ModConfigSpec.DoubleValue LOGISTICS_INSURANCE_RATE = BUILDER
            .comment("Insurance premium as a fraction of the declared cargo value.")
            .defineInRange("logisticsInsuranceRate", 0.05, 0.0, 1.0);

    public static final ModConfigSpec.LongValue LOGISTICS_DECLARED_VALUE = BUILDER
            .comment("Default declared value per cargo item in USD for logistics insurance.")
            .defineInRange("logisticsDeclaredValue", 10L, 1L, Long.MAX_VALUE);

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

    public static final ModConfigSpec.LongValue TAX_REFUND_MIN_AMOUNT = BUILDER
            .comment("Minimum tax refund amount in minor currency units.")
            .defineInRange("taxRefundMinAmount", 1L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue TAX_REFUND_AUTO_APPROVAL_LIMIT = BUILDER
            .comment("Maximum tax refund amount in minor units that can be approved automatically.")
            .defineInRange("taxRefundAutoApprovalLimit", 100000L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.LongValue TAX_REFUND_MAX_SINGLE_AMOUNT = BUILDER
            .comment("Maximum amount for one tax refund request in minor currency units.")
            .defineInRange("taxRefundMaxSingleAmount", 10000000L, 1L, Long.MAX_VALUE);

    public static final ModConfigSpec.IntValue TAX_REFUND_MAX_REQUESTS_PER_PERIOD = BUILDER
            .comment("Maximum number of refund requests per player during one refund period.")
            .defineInRange("taxRefundMaxRequestsPerPeriod", 3, 1, 100);

    public static final ModConfigSpec.IntValue TAX_REFUND_PERIOD_DAYS = BUILDER
            .comment("Length of the rolling tax refund period in Minecraft days.")
            .defineInRange("taxRefundPeriodDays", 30, 1, 3650);

    public static final ModConfigSpec.BooleanValue TAX_REFUND_ALLOW_OFFLINE = BUILDER
            .comment("Whether an approved refund may be delivered to an offline player's mailbox.")
            .define("taxRefundAllowOffline", true);

    public static String defaultCurrencyId() {
        String id = DEFAULT_CURRENCY.get();
        return Currencies.exists(id) ? id : Currencies.CNY.id();
    }

    public static Currency defaultCurrency() {
        return Currencies.byId(defaultCurrencyId());
    }

    static final ModConfigSpec SPEC = BUILDER.build();
}
