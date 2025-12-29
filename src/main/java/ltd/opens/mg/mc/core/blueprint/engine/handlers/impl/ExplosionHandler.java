package ltd.opens.mg.mc.core.blueprint.engine.handlers.impl;

import com.google.gson.JsonObject;
import ltd.opens.mg.mc.core.blueprint.engine.NodeContext;
import ltd.opens.mg.mc.core.blueprint.engine.NodeHandler;
import ltd.opens.mg.mc.core.blueprint.engine.NodeLogicRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.phys.Vec3;

public class ExplosionHandler implements NodeHandler {
    @Override
    public void execute(JsonObject node, NodeContext ctx) {
        String xStr = NodeLogicRegistry.evaluateInput(node, "x", ctx);
        String yStr = NodeLogicRegistry.evaluateInput(node, "y", ctx);
        String zStr = NodeLogicRegistry.evaluateInput(node, "z", ctx);
        String radiusStr = NodeLogicRegistry.evaluateInput(node, "radius", ctx);

        try {
            double x = xStr.isEmpty() ? 0 : Double.parseDouble(xStr);
            double y = yStr.isEmpty() ? 0 : Double.parseDouble(yStr);
            double z = zStr.isEmpty() ? 0 : Double.parseDouble(zStr);
            float radius = radiusStr.isEmpty() ? 3.0f : Float.parseFloat(radiusStr);

            Minecraft mc = Minecraft.getInstance();
            if (mc.level != null) {
                // Client-side: Level.explode() often doesn't do much because it expects server packets.
                // We manually trigger the explosion effects (sound and particles) that a TNT would produce.
                
                // 1. Create the explosion object for calculation if needed, 
                // but for client-side visual, we can just use the level's methods.
                
                // 2. Play Sound (Vanilla TNT explosion sound)
                mc.level.playLocalSound(x, y, z, 
                    net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), 
                    net.minecraft.sounds.SoundSource.BLOCKS, 
                    4.0F, 
                    (1.0F + (mc.level.random.nextFloat() - mc.level.random.nextFloat()) * 0.2F) * 0.7F, 
                    false);

                // 3. Add Particles (Vanilla explosion particles)
                if (radius > 2.0F) {
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1.0D, 0.0D, 0.0D);
                } else {
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.EXPLOSION, x, y, z, 1.0D, 0.0D, 0.0D);
                }
                
                // Note: Client-side explosion won't break blocks or hurt entities 
                // as that requires server-side logic.
            }
        } catch (Exception e) {
            // Ignore errors
        }

        NodeLogicRegistry.triggerExec(node, "exec", ctx);
    }
}
