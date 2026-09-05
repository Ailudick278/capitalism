package com.ailudick.capitalismmod.business;

import java.util.LinkedHashMap;
import java.util.Map;

/** Built-in, stable business-scope catalog used by sole proprietors and companies. */
public final class BusinessTypes {
    public record Definition(String id, String displayName, String category,
                             boolean requiresPermit, boolean available) {
    }

    private static final Map<String, Definition> DEFINITIONS = new LinkedHashMap<>();

    static {
        add("agriculture", "农林牧渔业", "primary", false, true);
        add("mining", "采矿业", "primary", false, true);
        add("wood_processing", "木材加工", "manufacturing", false, true);
        add("food_manufacturing", "食品制造", "manufacturing", false, true);
        add("general_manufacturing", "一般制造业", "manufacturing", false, true);
        add("general_trade", "普通贸易", "trade", false, true);
        add("transport", "运输服务", "service", false, true);
        add("repair", "维修服务", "service", false, true);
        add("financial_services", "金融服务", "finance", true, false);
        add("medicine", "药品经营", "special", true, false);
        add("hazardous_materials", "危险品经营", "special", true, false);
        add("gambling", "博彩服务", "special", true, false);

        // Official standardized business-scope statements (SAMR catalog snapshot).
        add("A1001", "谷物种植", "011谷物种植", false, true);
        add("A1002", "豆类种植", "0121豆类种植", false, true);
        add("A1003", "油料种植", "0122油料种植", false, true);
        add("A1004", "薯类种植", "0123薯类种植", false, true);
        add("A1005", "棉花种植", "0131棉花种植", false, true);
        add("A1009", "蔬菜种植", "0141蔬菜种植", false, true);
        add("A1010", "食用菌种植", "0142食用菌种植", false, true);
        add("A1011", "花卉种植", "0143花卉种植", false, true);
        add("A1013", "水果种植", "015水果种植", false, true);
        add("A1017", "茶叶种植", "0164茶叶种植", false, true);
        add("A1020", "中草药种植", "0171、0179中药材种植", false, true);
        add("A1028", "农产品的生产、销售、加工、运输、贮藏及其他相关服务", "019其他农业；131、136、137、5111", false, true);
        add("A2001", "人工造林", "0220造林和更新", false, true);
        add("A2002", "森林经营和管护", "0231森林经营和管护", false, true);
        add("A2004", "木材采运", "0241木材采运", true, false);
        add("A2005", "竹材采运", "0242竹材采运", false, true);
        add("A2006", "林产品采集", "025林产品采集", false, true);
        add("A3001", "动物饲养", "031、032、039、8221", true, false);
        add("A3002", "家禽饲养", "032家禽饲养", true, false);
        add("A3003", "牲畜饲养", "031牲畜饲养", true, false);
        add("A4002", "水产养殖", "041水产养殖", true, false);
        add("A4003", "渔业捕捞", "042水产捕捞", true, false);
        add("A5001", "农业机械服务", "0512农业机械活动", false, true);
        add("A5002", "灌溉服务", "0513灌溉活动", false, true);
        add("A5003", "非食用农产品初加工", "0514农产品初加工活动", false, true);
        add("A5004", "农作物病虫害防治服务", "0515农作物病虫害防治活动", false, true);
        add("A5006", "农作物栽培服务", "0519其他农业专业及辅助性活动", false, true);
        add("A5007", "农作物收割服务", "0519其他农业专业及辅助性活动", false, true);
        add("A5017", "智能农业管理", "0519、0529、0539、0549", false, true);
        add("A5018", "畜禽粪污处理", "0532畜禽粪污处理活动", false, true);
        add("A6001", "水产苗种生产", "041水产养殖", true, false);

        // Complete standardized business-scope directory imported from the supplied PDF.
        for (BusinessScopeCatalog.Scope scope : BusinessScopeCatalog.all()) {
            DEFINITIONS.putIfAbsent(scope.code(), new Definition(
                    scope.code(), scope.displayName(), scope.industry(), requiresPermit(scope.sourceText()), true));
        }
    }

    private BusinessTypes() {
    }

    private static void add(String id, String displayName, String category, boolean requiresPermit, boolean available) {
        DEFINITIONS.put(id, new Definition(id, displayName, category, requiresPermit, available));
    }

    private static boolean requiresPermit(String sourceText) {
        return sourceText.contains("许可") || sourceText.contains("审批")
                || sourceText.contains("核发") || sourceText.contains("资质认定");
    }

    public static boolean isValid(String id) {
        return id != null && DEFINITIONS.containsKey(id);
    }

    public static Definition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static String displayName(String id) {
        Definition definition = get(id);
        return definition == null ? id : definition.displayName();
    }

    public static Map<String, Definition> all() {
        return Map.copyOf(DEFINITIONS);
    }
}
