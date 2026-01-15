package ltd.opens.mg.mc.core.registry;

import ltd.opens.mg.mc.MaingraphforMC;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import ltd.opens.mg.mc.core.blueprint.inventory.BlueprintWorkbenchMenu;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import com.mojang.serialization.Codec;

import java.util.List;
import java.util.function.Supplier;

public class MGMCRegistries {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES = 
        DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, MaingraphforMC.MODID);

    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
        DeferredRegister.create(Registries.MENU, MaingraphforMC.MODID);

    // 存储蓝图路径列表的组件
    public static final Supplier<DataComponentType<List<String>>> BLUEPRINT_SCRIPTS = 
        DATA_COMPONENT_TYPES.register("scripts", () -> DataComponentType.<List<String>>builder()
            .persistent(Codec.STRING.listOf())
            .networkSynchronized(ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()))
            .build());

    // 蓝图工作台菜单类型
    public static final Supplier<MenuType<BlueprintWorkbenchMenu>> BLUEPRINT_WORKBENCH_MENU = 
        MENU_TYPES.register("blueprint_workbench", () -> IMenuTypeExtension.create(BlueprintWorkbenchMenu::new));

    public static void register(IEventBus modEventBus) {
        DATA_COMPONENT_TYPES.register(modEventBus);
        MENU_TYPES.register(modEventBus);
    }
}
