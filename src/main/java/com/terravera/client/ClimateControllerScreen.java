package com.terravera.client;
import com.terravera.common.climate.ClimateControlSystem;
import com.terravera.common.container.ClimateControllerMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
/** Compact commissioning GUI; deliberately reports demand, shell requirements, programming and service state. */
public class ClimateControllerScreen extends AbstractContainerScreen<ClimateControllerMenu> {
 public ClimateControllerScreen(ClimateControllerMenu m, Inventory i, Component t){super(m,i,t);imageWidth=176;imageHeight=184;inventoryLabelY=88;}
 protected void init(){super.init(); addRenderableWidget(Button.builder(Component.literal("−"),b->click(0)).bounds(leftPos+20,topPos+25,20,18).build());addRenderableWidget(Button.builder(Component.literal("+"),b->click(1)).bounds(leftPos+136,topPos+25,20,18).build());addRenderableWidget(Button.builder(Component.literal("−"),b->click(2)).bounds(leftPos+20,topPos+48,20,18).build());addRenderableWidget(Button.builder(Component.literal("+"),b->click(3)).bounds(leftPos+136,topPos+48,20,18).build());addRenderableWidget(Button.builder(Component.translatable("terravera.climate.install_circuit"),b->click(4)).bounds(leftPos+20,topPos+70,136,18).build());addRenderableWidget(Button.builder(Component.translatable("terravera.climate.service"),b->click(5)).bounds(leftPos+20,topPos+90,136,18).build());}
 private void click(int id){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
 protected void renderBg(GuiGraphics g,float p,int mx,int my){g.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xffc6a97b);g.fill(leftPos+2,topPos+2,leftPos+imageWidth-2,topPos+20,0xff6b4f2e);}
 protected void renderLabels(GuiGraphics g,int x,int y){g.drawString(font,title,8,7,0xffffffff);g.drawString(font,Component.translatable("terravera.climate.target",menu.target()),46,30,0xff3b2510,false);g.drawString(font,Component.translatable("terravera.climate.speed",menu.speed()),46,53,0xff3b2510,false);g.drawString(font,Component.translatable(menu.programmed()?"terravera.climate.programmed":"terravera.climate.unprogrammed"),8,113,menu.programmed()?0xff226622:0xff992222,false);g.drawString(font,Component.translatable("terravera.climate.maintenance",menu.maintenance()),8,124,0xff3b2510,false);g.drawString(font,Component.translatable("terravera.climate.shell_hint"),8,135,0xff3b2510,false);g.drawString(font,playerInventoryTitle,8,88,0xff3b2510,false);}
}
