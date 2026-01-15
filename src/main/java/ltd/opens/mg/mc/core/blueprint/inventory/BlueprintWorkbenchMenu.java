package ltd.opens.mg.mc.core.blueprint.inventory;

import ltd.opens.mg.mc.core.registry.MGMCRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;

public class BlueprintWorkbenchMenu extends AbstractContainerMenu {
    private final Container container;

    public BlueprintWorkbenchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(1));
    }

    public BlueprintWorkbenchMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, new SimpleContainer(1));
    }

    public BlueprintWorkbenchMenu(int containerId, Inventory playerInventory, Container container) {
        super(MGMCRegistries.BLUEPRINT_WORKBENCH_MENU.get(), containerId);
        this.container = container;

        // 唯一物品槽位 (左侧放置框) - 适配 256 宽度的 UI，将其放在左侧
        this.addSlot(new Slot(container, 0, 20, 35) {
            @Override
            public void setChanged() {
                super.setChanged();
                BlueprintWorkbenchMenu.this.slotsChanged(container);
            }
        });
        
        // 玩家背包 - 适配 256 宽度的 UI，居中显示 (offset = (256-176)/2 = 40)
        int xOffset = 40;
        int yOffset = 140; // 将背包往下移，给蓝图列表留出空间

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, xOffset + 8 + j * 18, yOffset + i * 18));
            }
        }

        // 玩家快捷栏
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, xOffset + 8 + k * 18, yOffset + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index == 0) {
                if (!this.moveItemStackTo(itemstack1, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
            } else if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.clearContainer(player, this.container);
    }
    
    public ItemStack getTargetItem() {
        return this.container.getItem(0);
    }
}
