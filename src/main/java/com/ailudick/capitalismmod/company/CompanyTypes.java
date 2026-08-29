package com.ailudick.capitalismmod.company;

import java.util.List;

/**
 * The 20 industry categories (门类 A–T) of the Chinese National Economy Industry
 * Classification GB/T 4754-2017 (amended 2023).
 */
public final class CompanyTypes {
    public static final List<String> ALL = List.of(
            "agriculture",       // A 农、林、牧、渔业
            "mining",            // B 采矿业
            "manufacturing",     // C 制造业
            "utilities",         // D 电力、热力、燃气及水生产和供应业
            "construction",      // E 建筑业
            "retail",            // F 批发和零售业
            "transport",         // G 交通运输、仓储和邮政业
            "hospitality",       // H 住宿和餐饮业
            "it_services",       // I 信息传输、软件和信息技术服务业
            "finance",           // J 金融业
            "real_estate",       // K 房地产业
            "business_services", // L 租赁和商务服务业
            "research",          // M 科学研究和技术服务业
            "environment",       // N 水利、环境和公共设施管理业
            "consumer_services", // O 居民服务、修理和其他服务业
            "education",         // P 教育
            "healthcare",        // Q 卫生和社会工作
            "culture",           // R 文化、体育和娱乐业
            "public_admin",      // S 公共管理、社会保障和社会组织
            "intl_org"           // T 国际组织
    );

    private CompanyTypes() {
    }

    public static boolean isValid(String id) {
        return ALL.contains(id);
    }

    /** Full official category name, used in company info and command output. */
    public static String nameKey(String id) {
        return "company_type.capitalismmod." + id;
    }

    /** Short category name, used for the compact selection grid in the UI. */
    public static String shortNameKey(String id) {
        return "company_type_short.capitalismmod." + id;
    }
}
