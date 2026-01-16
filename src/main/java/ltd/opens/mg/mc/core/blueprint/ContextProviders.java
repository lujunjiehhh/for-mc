package ltd.opens.mg.mc.core.blueprint;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;
import net.neoforged.neoforge.event.entity.EntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 事件上下文提供者注册表
 */
public class ContextProviders {
    private static final Map<Class<? extends Event>, EventContextProvider<?>> PROVIDERS = new ConcurrentHashMap<>();

    /**
     * 注册一个新的事件上下文提供者
     */
    public static <T extends Event> void register(Class<T> eventClass, EventContextProvider<T> provider) {
        PROVIDERS.put(eventClass, provider);
    }

    /**
     * 获取指定事件的上下文提供者
     */
    @SuppressWarnings("unchecked")
    public static <T extends Event> EventContextProvider<T> getProvider(Class<T> eventClass) {
        // 首先尝试直接匹配
        EventContextProvider<T> provider = (EventContextProvider<T>) PROVIDERS.get(eventClass);
        if (provider != null) return provider;

        // 尝试匹配父类
        for (Map.Entry<Class<? extends Event>, EventContextProvider<?>> entry : PROVIDERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(eventClass)) {
                return (EventContextProvider<T>) entry.getValue();
            }
        }

        // 默认兜底逻辑（保持向后兼容，包含原 EventDispatcher 中的硬编码逻辑）
        return (EventContextProvider<T>) DEFAULT_PROVIDER;
    }

    private static final EventContextProvider<Event> DEFAULT_PROVIDER = new EventContextProvider<>() {
        @Override
        public Level getLevel(Event event) {
            if (event instanceof ItemEntityPickupEvent iepe) return iepe.getPlayer().level();
            if (event instanceof LevelEvent le) {
                if (le.getLevel() instanceof Level l) return l;
            }
            if (event instanceof BlockEvent be) {
                if (be.getLevel() instanceof Level l) return l;
            }
            if (event instanceof EntityEvent ee) return ee.getEntity().level();
            if (event instanceof PlayerTickEvent pte) return pte.getEntity().level();
            if (event instanceof PlayerEvent pe) return pe.getEntity().level();
            return null;
        }

        @Override
        public Player getPlayer(Event event) {
            if (event instanceof PlayerEvent pe) return pe.getEntity();
            if (event instanceof ItemEntityPickupEvent iepe) return iepe.getPlayer();
            if (event instanceof BlockEvent be) {
                if (be instanceof BlockEvent.BreakEvent bre) return bre.getPlayer();
                if (be instanceof BlockEvent.EntityPlaceEvent epe) {
                    if (epe.getEntity() instanceof Player p) return p;
                }
            }
            return null;
        }
    };
}
