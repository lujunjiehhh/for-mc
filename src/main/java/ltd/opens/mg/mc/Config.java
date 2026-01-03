package ltd.opens.mg.mc;

import net.neoforged.neoforge.common.ModConfigSpec;

// Config class for the mod.
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private static final ModConfigSpec.ConfigValue<Integer> MAX_RECURSION_DEPTH_VAL = BUILDER
            .comment("最大递归执行深度，防止蓝图死循环导致崩溃")
            .define("max_recursion_depth", 10);

    static final ModConfigSpec SPEC = BUILDER.build();

    public static int getMaxRecursionDepth() {
        return MAX_RECURSION_DEPTH_VAL.get();
    }
}
