package ltd.opens.mg.mc.client.gui.screens;

import ltd.opens.mg.mc.client.network.NetworkService;
import ltd.opens.mg.mc.core.blueprint.inventory.BlueprintWorkbenchMenu;
import ltd.opens.mg.mc.core.registry.MGMCRegistries;
import ltd.opens.mg.mc.network.payloads.WorkbenchActionPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.List;

public class BlueprintWorkbenchScreen extends AbstractContainerScreen<BlueprintWorkbenchMenu> {
    private BlueprintList allBlueprintsList;
    private BoundBlueprintList boundBlueprintsList;

    public BlueprintWorkbenchScreen(BlueprintWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 220;
    }

    @Override
    protected void init() {
        super.init();
        
        // 左侧：已绑定蓝图列表
        this.boundBlueprintsList = new BoundBlueprintList(this.minecraft, 80, 100, this.topPos + 30, 20);
        this.boundBlueprintsList.setX(this.leftPos + 60);
        this.addRenderableWidget(this.boundBlueprintsList);

        // 右侧：全局蓝图库
        this.allBlueprintsList = new BlueprintList(this.minecraft, 80, 100, this.topPos + 30, 20);
        this.allBlueprintsList.setX(this.leftPos + 160);
        this.addRenderableWidget(this.allBlueprintsList);

        // 绑定按钮 (->)
        this.addRenderableWidget(Button.builder(Component.literal("->"), b -> {
            BlueprintEntry selected = allBlueprintsList.getSelected();
            if (selected != null) {
                NetworkService.getInstance().sendWorkbenchAction(WorkbenchActionPayload.Action.BIND, selected.path);
            }
        }).bounds(this.leftPos + 145, this.topPos + 50, 12, 20).build());

        // 请求数据
        NetworkService.getInstance().requestBlueprintList();
    }

    public void updateListFromServer(List<String> blueprints) {
        if (allBlueprintsList != null) {
            allBlueprintsList.clearEntries();
            for (String bp : blueprints) {
                allBlueprintsList.add(new BlueprintEntry(bp));
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xEE333333);
        graphics.renderOutline(this.leftPos, this.topPos, this.imageWidth, this.imageHeight, 0xFFFFFFFF);
        
        // 绘制槽位背景
        graphics.fill(this.leftPos + 20, this.topPos + 35, this.leftPos + 38, this.topPos + 53, 0xFF000000);
        
        graphics.drawString(this.font, this.title, this.leftPos + 8, this.topPos + 6, 0xFFFFFF, false);
        graphics.drawString(this.font, Component.literal("已绑定"), this.leftPos + 60, this.topPos + 20, 0xAAAAAA, false);
        graphics.drawString(this.font, Component.literal("蓝图库"), this.leftPos + 160, this.topPos + 20, 0xAAAAAA, false);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        ItemStack stack = this.menu.getTargetItem();
        List<String> bound = stack.getOrDefault(MGMCRegistries.BLUEPRINT_SCRIPTS.get(), Collections.emptyList());
        this.boundBlueprintsList.updateList(bound);
    }

    // --- 列表组件 ---

    private class BlueprintList extends ObjectSelectionList<BlueprintEntry> {
        public BlueprintList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }
        public void add(BlueprintEntry entry) {
            this.addEntry(entry);
        }
        @Override
        public int getRowWidth() { return this.width; }
    }

    private class BlueprintEntry extends ObjectSelectionList.Entry<BlueprintEntry> {
        final String path;
        public BlueprintEntry(String path) { this.path = path; }
        @Override
        public void renderContent(GuiGraphics g, int index, int top, boolean hover, float p) {
            int left = this.getX();
            int y = this.getY();
            if (y <= 0) y = top;
            g.drawString(minecraft.font, path, left + 5, y + 5, hover ? 0xFFFF00 : 0xFFFFFF);
        }
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
            allBlueprintsList.setSelected(this);
            return true;
        }
        @Override
        public Component getNarration() { return Component.literal(path); }
    }

    private class BoundBlueprintList extends ObjectSelectionList<BoundEntry> {
        public BoundBlueprintList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }
        public void updateList(List<String> blueprints) {
            String selected = this.getSelected() != null ? this.getSelected().path : null;
            this.clearEntries();
            for (String bp : blueprints) {
                BoundEntry entry = new BoundEntry(bp);
                this.addEntry(entry);
                if (bp.equals(selected)) this.setSelected(entry);
            }
        }
        @Override
        public int getRowWidth() { return this.width; }
    }

    private class BoundEntry extends ObjectSelectionList.Entry<BoundEntry> {
        final String path;
        public BoundEntry(String path) { this.path = path; }
        @Override
        public void renderContent(GuiGraphics g, int index, int top, boolean hover, float p) {
            int left = this.getX();
            int width = this.getWidth();
            int y = this.getY();
            if (y <= 0) y = top;
            g.drawString(minecraft.font, path, left + 5, y + 5, 0xFFFFFF);
            if (hover) {
                g.drawString(minecraft.font, "X", left + width - 12, y + 5, 0xFF0000);
            }
        }
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
            // 这里简单处理，如果点到右侧区域就认为是解绑
            if (event.x() > this.getX() + this.getWidth() - 15) {
                NetworkService.getInstance().sendWorkbenchAction(WorkbenchActionPayload.Action.UNBIND, path);
            }
            return true;
        }
        @Override
        public Component getNarration() { return Component.literal(path); }
    }
}
