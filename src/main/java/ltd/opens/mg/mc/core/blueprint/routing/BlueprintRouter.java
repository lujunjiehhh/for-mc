package ltd.opens.mg.mc.core.blueprint.routing;

import com.google.gson.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 中央调度路由中心
 * 负责维护 Minecraft ID (ResourceLocation) 与蓝图文件路径之间的映射关系
 */
public class BlueprintRouter {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    // 虚拟 ID 定义
    public static final String GLOBAL_ID = "mgmc:global";
    public static final String PLAYERS_ID = "mgmc:players";

    // 内存中的路由表: ID -> 蓝图路径列表
    private static final Map<String, Set<String>> routingTable = new ConcurrentHashMap<>();

    /**
     * 初始化路由表（仅供兼容性保留，实际应使用 load(ServerLevel)）
     */
    public static void init() {
        // 默认不执行任何操作，等待世界加载时由 load() 处理
    }

    /**
     * 从指定世界的路由表文件加载
     */
    public static void load(ServerLevel level) {
        Path filePath = getMappingsPath(level);
        if (!Files.exists(filePath)) {
            routingTable.clear();
            save(level); // 创建初始文件
            return;
        }

        try (FileReader reader = new FileReader(filePath.toFile())) {
            JsonObject json = GSON.fromJson(reader, JsonObject.class);
            routingTable.clear();
            if (json != null) {
                for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                    JsonArray array = entry.getValue().getAsJsonArray();
                    Set<String> blueprints = Collections.newSetFromMap(new ConcurrentHashMap<>());
                    for (JsonElement e : array) {
                        blueprints.add(e.getAsString());
                    }
                    routingTable.put(entry.getKey(), blueprints);
                }
            }
            LOGGER.info("MGMC: Loaded {} ID mappings from {}", routingTable.size(), filePath);
        } catch (IOException e) {
            LOGGER.error("MGMC: Failed to load mappings from " + filePath, e);
        }
    }

    /**
     * 保存路由表到指定世界的路由表文件
     */
    public static synchronized void save(ServerLevel level) {
        Path filePath = getMappingsPath(level);
        try {
            Files.createDirectories(filePath.getParent());
            try (FileWriter writer = new FileWriter(filePath.toFile())) {
                JsonObject json = new JsonObject();
                for (Map.Entry<String, Set<String>> entry : routingTable.entrySet()) {
                    JsonArray array = new JsonArray();
                    entry.getValue().forEach(array::add);
                    json.add(entry.getKey(), array);
                }
                GSON.toJson(json, writer);
            }
        } catch (IOException e) {
            LOGGER.error("MGMC: Failed to save mappings to " + filePath, e);
        }
    }

    private static Path getMappingsPath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("mgmc_blueprints/.routing/mappings.json");
    }

    /**
     * 获取指定 ID 绑定的所有蓝图路径
     */
    public static Set<String> getMappedBlueprints(String id) {
        return routingTable.getOrDefault(id, Collections.emptySet());
    }

    /**
     * 添加映射（注意：此方法目前仅由客户端通过网络请求触发，由 handleSaveMappings 统一处理保存）
     */
    public static void addMapping(String id, String blueprintPath) {
        routingTable.computeIfAbsent(id, k -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                    .add(blueprintPath);
    }

    /**
     * 移除映射
     */
    public static void removeMapping(String id, String blueprintPath) {
        Set<String> blueprints = routingTable.get(id);
        if (blueprints != null) {
            blueprints.remove(blueprintPath);
            if (blueprints.isEmpty()) {
                routingTable.remove(id);
            }
        }
    }

    /**
     * 获取所有已订阅的 ID
     */
    public static Set<String> getAllSubscribedIds() {
        return routingTable.keySet();
    }

    /**
     * 获取完整的路由表快照
     */
    public static Map<String, Set<String>> getFullRoutingTable() {
        Map<String, Set<String>> copy = new HashMap<>();
        routingTable.forEach((k, v) -> copy.put(k, new HashSet<>(v)));
        return copy;
    }

    /**
     * 批量更新路由表并保存
     */
    public static void updateAllMappings(ServerLevel level, Map<String, Set<String>> newMappings) {
        routingTable.clear();
        newMappings.forEach((k, v) -> {
            Set<String> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
            set.addAll(v);
            routingTable.put(k, set);
        });
        save(level);
    }

    /**
     * 客户端专用的内存更新（不保存文件）
     */
    public static void clientUpdateMappings(Map<String, Set<String>> newMappings) {
        routingTable.clear();
        newMappings.forEach((k, v) -> {
            Set<String> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
            set.addAll(v);
            routingTable.put(k, set);
        });
    }
}
