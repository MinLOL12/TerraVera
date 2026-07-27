package com.terravera.common.container;
import com.terravera.common.blocks.TerraVeraBlocks;
import com.terravera.common.climate.ClimateControlSystem;
import com.terravera.common.items.TerraVeraItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
/** Menu is also the programming console: settings are never client-only and circuit installation is explicit. */
public class ClimateControllerMenu extends AbstractContainerMenu {
    public static final int TEMP_DOWN=0,TEMP_UP=1,SPEED_DOWN=2,SPEED_UP=3,INSTALL=4,SERVICE=5;
    private final BlockPos pos; private final ContainerLevelAccess access;
    private final DataSlot target=DataSlot.standalone(), speed=DataSlot.standalone(), programmed=DataSlot.standalone(), maintenance=DataSlot.standalone();
    public ClimateControllerMenu(int id, Inventory inv, BlockPos pos) { super(TerraVeraContainers.CLIMATE_CONTROLLER.get(),id); this.pos=pos; access=ContainerLevelAccess.create(inv.player.level(),pos); sync(); addDataSlot(target);addDataSlot(speed);addDataSlot(programmed);addDataSlot(maintenance); addPlayer(inv); }
    public static ClimateControllerMenu fromNetwork(int id, Inventory inv, RegistryFriendlyByteBuf buf) { return new ClimateControllerMenu(id,inv,buf.readBlockPos()); }
    private void sync(){ var c=ClimateControlSystem.get(pos);target.set(c.target());speed.set(c.speed());programmed.set(c.programmed()?1:0);maintenance.set(Math.round(c.maintenance()*100)); }
    public int target(){return target.get();} public int speed(){return speed.get();} public boolean programmed(){return programmed.get()!=0;} public int maintenance(){return maintenance.get();}
    @Override public boolean clickMenuButton(Player player,int id) { var c=ClimateControlSystem.get(pos); switch(id) { case TEMP_DOWN->c=c.target(c.target()-1);case TEMP_UP->c=c.target(c.target()+1);case SPEED_DOWN->c=c.speed(c.speed()-1);case SPEED_UP->c=c.speed(c.speed()+1); case INSTALL->{ if (!c.programmed() && take(player,TerraVeraItems.PROGRAMMED_CIRCUIT.get())) c=c.program(); } case SERVICE->{ if (take(player,TerraVeraItems.AIR_FILTER.get()) && take(player,TerraVeraItems.REFRIGERANT_CANISTER.get())) c=c.service(); } default->{return false;} } ClimateControlSystem.put(pos,c);sync();return true; }
    private boolean take(Player p, net.minecraft.world.item.Item item){ for(ItemStack s:p.getInventory().items) if(s.is(item)){s.shrink(1);return true;} p.displayClientMessage(Component.translatable("terravera.climate.missing_material"),true);return false; }
    private void addPlayer(Inventory inv){for(int r=0;r<3;r++)for(int c=0;c<9;c++)addSlot(new Slot(inv,c+r*9+9,8+c*18,100+r*18));for(int c=0;c<9;c++)addSlot(new Slot(inv,c,8+c*18,158));}
    @Override public boolean stillValid(Player p){return stillValid(access,p, TerraVeraBlocks.AIR_CONDITIONER.get());}
    @Override public ItemStack quickMoveStack(Player p,int i){return ItemStack.EMPTY;}
}
