package ltd.opens.mg.mc.core.blueprint.engine;

import java.util.UUID;

/**
 * 转换引擎 - 负责蓝图系统中各种数据类型的安全转换
 */
public class TypeConverter {

    public static String cast(String value, String targetType) {
        if (value == null) value = "";
        if (targetType == null) return value;
        
        targetType = targetType.toUpperCase();
        
        switch (targetType) {
            case "STRING":
                return value;
                
            case "FLOAT":
                try {
                    if (value.isEmpty()) return "0.0";
                    // 处理可能的科学计数法或非标准格式
                    return String.valueOf(Double.parseDouble(value));
                } catch (Exception e) {
                    return "0.0";
                }
                
            case "BOOLEAN":
                if (value.equalsIgnoreCase("true") || value.equals("1")) return "true";
                if (value.equalsIgnoreCase("false") || value.equals("0")) return "false";
                // 只有明确的 true 相关值才返回 true，避免意外
                return "false";
                
            case "UUID":
                try {
                    if (value.isEmpty()) return "";
                    return UUID.fromString(value).toString();
                } catch (Exception e) {
                    return "";
                }
                
            case "INT":
                try {
                    if (value.isEmpty()) return "0";
                    return String.valueOf((int) Math.round(Double.parseDouble(value)));
                } catch (Exception e) {
                    return "0";
                }

            case "LIST":
                // 列表底层就是字符串，确保不为 null 即可
                return value;

            default:
                return value;
        }
    }
}
