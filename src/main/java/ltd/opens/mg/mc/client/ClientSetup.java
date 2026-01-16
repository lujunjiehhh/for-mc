package ltd.opens.mg.mc.client;

import ltd.opens.mg.mc.client.gui.screens.BlueprintWorkbenchScreen;
import ltd.opens.mg.mc.core.registry.MGMCRegistries;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

public class ClientSetup {
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(MGMCRegistries.BLUEPRINT_WORKBENCH_MENU.get(), BlueprintWorkbenchScreen::new);
    }
}
