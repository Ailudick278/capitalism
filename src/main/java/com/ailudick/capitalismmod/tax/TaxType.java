package com.ailudick.capitalismmod.tax;

/** Tax categories shared by the economic subsystems. */
public enum TaxType {
    LAND("land", "土地持有税"),
    CORPORATE_INCOME("corporate_income", "企业利润所得税"),
    INDIVIDUAL_BUSINESS_INCOME("individual_business_income", "个人经营所得税"),
    VAT("vat", "增值税"),
    LAND_TRANSFER("land_transfer", "土地转让税"),
    STAMP_DUTY("stamp_duty", "证券印花税"),
    CAPITAL_GAINS("capital_gains", "资本利得税"),
    DIVIDEND("dividend", "股息税"),
    RESOURCE("resource", "资源税"),
    CUSTOMS("customs", "关税"),
    INHERITANCE("inheritance", "遗产与赠与税"),
    TRANSACTION("transaction", "交易税（兼容旧规则）"),
    OTHER("other", "其他税费");

    private final String id;
    private final String displayName;

    TaxType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }

    public static TaxType byId(String id) {
        for (TaxType type : values()) if (type.id.equals(id)) return type;
        return OTHER;
    }
}
