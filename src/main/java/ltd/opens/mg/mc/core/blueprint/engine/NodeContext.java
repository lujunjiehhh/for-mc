package ltd.opens.mg.mc.core.blueprint.engine;

import com.google.gson.JsonObject;
import java.util.Map;

public class NodeContext {
    public final String eventName;
    public final String[] args;
    public final String triggerUuid;
    public final Map<String, JsonObject> nodesMap;

    public NodeContext(String eventName, String[] args, String triggerUuid, Map<String, JsonObject> nodesMap) {
        this.eventName = eventName;
        this.args = args;
        this.triggerUuid = triggerUuid;
        this.nodesMap = nodesMap;
    }
}
