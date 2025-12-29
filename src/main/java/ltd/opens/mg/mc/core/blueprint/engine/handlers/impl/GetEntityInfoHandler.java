package ltd.opens.mg.mc.core.blueprint.engine.handlers.impl;

import com.google.gson.JsonObject;
import ltd.opens.mg.mc.core.blueprint.engine.NodeContext;
import ltd.opens.mg.mc.core.blueprint.engine.NodeHandler;
import ltd.opens.mg.mc.core.blueprint.engine.NodeLogicRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import java.util.UUID;

public class GetEntityInfoHandler implements NodeHandler {
    @Override
    public String getValue(JsonObject node, String pinId, NodeContext ctx) {
        String uuidStr = NodeLogicRegistry.evaluateInput(node, "uuid", ctx);
        if (uuidStr == null || uuidStr.isEmpty()) return "";

        try {
            UUID uuid = UUID.fromString(uuidStr);
            Entity entity = null;
            
            if (Minecraft.getInstance().level != null) {
                // For client side, we can iterate through entities for rendering or players
                for (Entity e : Minecraft.getInstance().level.entitiesForRendering()) {
                    if (e.getUUID().equals(uuid)) {
                        entity = e;
                        break;
                    }
                }
            }

            if (entity != null) {
                if (pinId.equals("name")) {
                    return entity.getName().getString();
                } else if (pinId.equals("type")) {
                    return entity.getType().getDescription().getString();
                }
            }
        } catch (Exception e) {
            // Ignore errors
        }
        
        return "";
    }
}
