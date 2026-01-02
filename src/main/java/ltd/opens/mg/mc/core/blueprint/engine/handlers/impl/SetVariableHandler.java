package ltd.opens.mg.mc.core.blueprint.engine.handlers.impl;

import com.google.gson.JsonObject;
import ltd.opens.mg.mc.core.blueprint.engine.NodeContext;
import ltd.opens.mg.mc.core.blueprint.engine.NodeHandler;
import ltd.opens.mg.mc.core.blueprint.engine.NodeLogicRegistry;

public class SetVariableHandler implements NodeHandler {
    @Override
    public void execute(JsonObject node, NodeContext ctx) {
        String name = NodeLogicRegistry.evaluateInput(node, "name", ctx);
        String value = NodeLogicRegistry.evaluateInput(node, "value", ctx);
        
        if (name != null && !name.isEmpty()) {
            ctx.variables.put(name, value);
        }
        
        NodeLogicRegistry.triggerExec(node, "exec", ctx);
    }

    @Override
    public String getValue(JsonObject node, String pinId, NodeContext ctx) {
        if (pinId.equals("value")) {
            return NodeLogicRegistry.evaluateInput(node, "value", ctx);
        }
        return "";
    }
}
