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
import net.minecraft.resources.Identifier;

import java.util.List;

public class BlueprintWorkbenchScreen extends AbstractContainerScreen<BlueprintWorkbenchMenu> {
    private static final Identifier SLOT_SPRITE = Identifier.parse("minecraft:container/slot");
    private static final Identifier INVENTORY_TEXTURE = Identifier.parse("minecraft:textures/gui/container/inventory.png");
    
    private BlueprintList allBlueprintsList;
    private BoundBlueprintList boundBlueprintsList;

    public BlueprintWorkbenchScreen(BlueprintWorkbenchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 256;
        this.imageHeight = 224;
        this.inventoryLabelY = 130; // 对应 Menu 中的 yOffset - 10
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    @Override
    protected void init() {
        super.init();
        
        int listWidth = 85;
        int listHeight = 90; // 稍微调短一点
        int listY = this.topPos + 35;

        this.boundBlueprintsList = new BoundBlueprintList(this.minecraft, listWidth, listHeight, listY, 20);
        this.boundBlueprintsList.setX(this.leftPos + 55);
        this.addRenderableWidget(this.boundBlueprintsList);

        this.allBlueprintsList = new BlueprintList(this.minecraft, listWidth, listHeight, listY, 20);
        this.allBlueprintsList.setX(this.leftPos + 160);
        this.addRenderableWidget(this.allBlueprintsList);

        this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
            BlueprintEntry selected = allBlueprintsList.getSelected();
            if (selected != null) {
                NetworkService.getInstance().sendWorkbenchAction(WorkbenchActionPayload.Action.BIND, selected.path);
            }
        }).bounds(this.leftPos + 142, this.topPos + 65, 16, 20).build());

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

    private ItemStack lastStack = ItemStack.EMPTY;
    private List<String> lastScripts = null;

    @Override
    public void containerTick() {
        super.containerTick();
        if (this.boundBlueprintsList != null) {
            ItemStack stack = this.menu.getTargetItem();
            List<String> scripts = stack.isEmpty() ? null : stack.get(MGMCRegistries.BLUEPRINT_SCRIPTS.get());
            
            boolean changed = false;
            if (stack.isEmpty() != lastStack.isEmpty()) {
                changed = true;
            } else if (!stack.isEmpty()) {
                if (scripts == null && lastScripts != null) changed = true;
                else if (scripts != null && !scripts.equals(lastScripts)) changed = true;
            }

            if (changed) {
                if (scripts != null) {
                    this.boundBlueprintsList.updateList(scripts);
                } else {
                    this.boundBlueprintsList.clearEntries();
                }
                lastStack = stack.copy();
                lastScripts = scripts == null ? null : List.copyOf(scripts);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        
        // 1. 绘制原版风格的基础背景 (浅灰色)
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFFC6C6C6);
        
        // 2. 绘制 3D 边框
        graphics.fill(x, y, x + this.imageWidth, y + 1, 0xFFFFFFFF); // 顶部高亮
        graphics.fill(x, y, x + 1, y + this.imageHeight, 0xFFFFFFFF); // 左侧高亮
        graphics.fill(x + this.imageWidth - 1, y, x + this.imageWidth, y + this.imageHeight, 0xFF555555); // 右侧阴影
        graphics.fill(x, y + this.imageHeight - 1, x + this.imageWidth, y + this.imageHeight, 0xFF555555); // 底部阴影

        // 3. 绘制单个物品槽位 (Menu 中位置是 20, 35)
        int slotX = x + 20;
        int slotY = y + 35;
        // 使用原版凹陷槽位风格
        graphics.fill(slotX - 1, slotY - 1, slotX + 19, slotY + 19, 0xFF373737); // 左上深色
        graphics.fill(slotX, slotY, slotX + 19, slotY + 19, 0xFFFFFFFF); // 右下白色
        graphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF8B8B8B); // 内部灰色
        
        // 4. 绘制蓝图列表背景面板
        renderPanel(graphics, x + 55, y + 35, 85, 90, "已绑定蓝图");
        renderPanel(graphics, x + 160, y + 35, 85, 90, "蓝图库");
        
        // 5. 绘制背包贴图 (对齐槽位)
        // Menu 中背包槽位于 (48, 140)
        // inventory.png 中背包槽位位于 (7, 83)
        // 贴图渲染位置 = (x + 48 - 7, y + 140 - 83) = (x + 41, y + 57)
        // 但我们只需要下半部分，从 V=82 开始截取 (包含 "Inventory" 文字)
        int invTexX = x + 41;
        int invTexY = y + 132; // 对应 Menu 的 140，向上偏 8 像素显示文字
        
        // blit(texture, x, y, width, height, uOffset, vOffset, uWidth, vHeight)
        graphics.blit(INVENTORY_TEXTURE, invTexX, invTexY, 176, 90, 0f, 82f, 176f, 90f);

        // 6. 绘制标题
        graphics.drawString(this.font, this.title, x + 8, y + 8, 0x404040, false);
    }

    private void renderPanel(GuiGraphics g, int x, int y, int w, int h, String label) {
        // 凹陷面板风格
        g.fill(x - 1, y - 1, x + w, y + h, 0xFF373737); // 深色边
        g.fill(x, y, x + w + 1, y + h + 1, 0xFFFFFFFF); // 白色边
        g.fill(x, y, x + w, y + h, 0xFF8B8B8B); // 内部灰色
        
        g.drawString(this.font, Component.literal(label), x, y - 10, 0x404040, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        // 覆盖父类方法，防止重复绘制 "Inventory" 标签
        // 因为我们在 blit 贴图中已经包含了该文字
    }

    private class BlueprintList extends ObjectSelectionList<BlueprintEntry> {
        public BlueprintList(Minecraft minecraft, int width, int height, int y, int itemHeight) {
            super(minecraft, width, height, y, itemHeight);
        }
        public void add(BlueprintEntry entry) {
            this.addEntry(entry);
        }
        @Override
        public int getRowWidth() { return this.width - 4; }
    }

    private class BlueprintEntry extends ObjectSelectionList.Entry<BlueprintEntry> {
        final String path;
        public BlueprintEntry(String path) { this.path = path; }
        @Override
        public void renderContent(GuiGraphics guiGraphics, int index, int top, boolean isHovered, float partialTick) {
            int x = this.getX();
            int y = this.getY();
            if (y <= 0) y = top;
            
            if (this == allBlueprintsList.getSelected()) {
                guiGraphics.fill(x, y, x + allBlueprintsList.getRowWidth(), y + 18, 0xFFFFFFFF); // White selection
                String name = path;
                if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
                guiGraphics.drawString(minecraft.font, name, x + 4, y + 5, 0x404040, false); // Dark text when selected
            } else {
                if (isHovered) {
                    guiGraphics.fill(x, y, x + allBlueprintsList.getRowWidth(), y + 18, 0x44FFFFFF);
                }
                String name = path;
                if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
                guiGraphics.drawString(minecraft.font, name, x + 4, y + 5, 0xFFFFFF, false);
            }
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
        public int getRowWidth() { return this.width - 4; }
    }

    private class BoundEntry extends ObjectSelectionList.Entry<BoundEntry> {
        final String path;
        public BoundEntry(String path) { this.path = path; }
        @Override
        public void renderContent(GuiGraphics guiGraphics, int index, int top, boolean isHovered, float partialTick) {
            int x = this.getX();
            int width = this.getWidth();
            int y = this.getY();
            if (y <= 0) y = top;

            if (isHovered) {
                guiGraphics.fill(x, y, x + width, y + 18, 0x44FFFFFF);
            }

            String name = path;
            if (name.endsWith(".json")) name = name.substring(0, name.length() - 5);
            guiGraphics.drawString(minecraft.font, name, x + 4, y + 5, 0xFFFFFF, false);
            
            if (isHovered) {
                guiGraphics.drawString(minecraft.font, "✕", x + width - 12, y + 5, 0xFFFF5555, false);
            }
        }
        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean isDouble) {
            if (event.x() > this.getX() + this.getWidth() - 15) {
                NetworkService.getInstance().sendWorkbenchAction(WorkbenchActionPayload.Action.UNBIND, path);
            }
            return true;
        }
        @Override
        public Component getNarration() { return Component.literal(path); }
    }
}
