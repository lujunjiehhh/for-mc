package ltd.opens.mg.mc.core.blueprint.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import ltd.opens.mg.mc.MaingraphforMC;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;

public class BlueprintEngine {

    private static final ThreadLocal<Integer> RECURSION_DEPTH = ThreadLocal.withInitial(() -> 0);

    public static void execute(Level level, String json, String eventType, String name, String[] args, 
                                String triggerUuid, String triggerName, double tx, double ty, double tz, double speed) {
        execute(level, json, eventType, name, args, triggerUuid, triggerName, tx, ty, tz, speed, "", "", 0.0, "");
    }

    public static void execute(Level level, String json, String eventType, String name, String[] args, 
                                String triggerUuid, String triggerName, double tx, double ty, double tz, double speed,
                                String triggerBlockId, String triggerItemId, double triggerValue, String triggerExtraUuid) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            execute(level, root, eventType, name, args, triggerUuid, triggerName, tx, ty, tz, speed, triggerBlockId, triggerItemId, triggerValue, triggerExtraUuid);
        } catch (Exception e) {
            MaingraphforMC.LOGGER.error("Error parsing blueprint JSON", e);
        }
    }

    public static void execute(Level level, JsonObject root, String eventType, String name, String[] args, 
                                String triggerUuid, String triggerName, double tx, double ty, double tz, double speed) {
        execute(level, root, eventType, name, args, triggerUuid, triggerName, tx, ty, tz, speed, "", "", 0.0, "");
    }

    public static void execute(Level level, JsonObject root, String eventType, String name, String[] args, 
                                String triggerUuid, String triggerName, double tx, double ty, double tz, double speed,
                                String triggerBlockId, String triggerItemId, double triggerValue, String triggerExtraUuid) {
        NodeContext.Builder builder = new NodeContext.Builder(level)
            .eventName(name)
            .args(args)
            .triggerUuid(triggerUuid)
            .triggerName(triggerName)
            .triggerX(tx).triggerY(ty).triggerZ(tz)
            .triggerSpeed(speed)
            .triggerBlockId(triggerBlockId)
            .triggerItemId(triggerItemId)
            .triggerValue(triggerValue)
            .triggerExtraUuid(triggerExtraUuid);
        
        execute(level, root, eventType, builder);
    }

    public static void execute(Level level, JsonObject root, String eventType, NodeContext.Builder contextBuilder) {
        if (RECURSION_DEPTH.get() >= ltd.opens.mg.mc.Config.getMaxRecursionDepth()) {
            return;
        }

        RECURSION_DEPTH.set(RECURSION_DEPTH.get() + 1);
        try {
            if (!root.has("execution") || !root.get("execution").isJsonArray()) {
                return;
            }

            JsonArray executionNodes = root.getAsJsonArray("execution");
            Map<String, JsonObject> nodesMap = new HashMap<>();
            
            for (JsonElement e : executionNodes) {
                if (!e.isJsonObject()) continue;
                JsonObject node = e.getAsJsonObject();
                if (node.has("id")) {
                    nodesMap.put(node.get("id").getAsString(), node);
                }
            }

            int formatVersion = root.has("format_version") ? root.get("format_version").getAsInt() : 1;
            NodeContext ctx = contextBuilder.nodesMap(nodesMap).formatVersion(formatVersion).build();

            for (JsonElement e : executionNodes) {
                if (!e.isJsonObject()) continue;
                JsonObject node = e.getAsJsonObject();
                String type = node.has("type") ? node.get("type").getAsString() : null;
                
                if (type != null) {
                    // 兼容带命名空间和不带命名空间的匹配 (e.g., "mgmc:on_mgrun" vs "on_mgrun")
                    String pureType = type.contains(":") ? type.substring(type.indexOf(":") + 1) : type;
                    String pureEvent = eventType.contains(":") ? eventType.substring(eventType.indexOf(":") + 1) : eventType;
                    
                    if (pureType.equals(pureEvent)) {
                        // Check if the 'name' output matches the requested name
                        String nodeName = TypeConverter.toString(NodeLogicRegistry.evaluateOutput(node, "name", ctx));
                        if (ctx.eventName.isEmpty() || ctx.eventName.equals(nodeName)) {
                            NodeLogicRegistry.triggerExec(node, "exec", ctx);
                        }
                    }
                }
            }
        } catch (Exception e) {
            MaingraphforMC.LOGGER.error("Error executing blueprint", e);
        } finally {
            RECURSION_DEPTH.set(RECURSION_DEPTH.get() - 1);
        }
    }
}
