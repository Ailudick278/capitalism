package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.progression.TechnologyEra;
import com.ailudick.capitalismmod.company.MachineType;
import com.ailudick.capitalismmod.network.payload.TechnologySnapshotPayload;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.*;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;

/** An LDLib2 atlas: chrome and interactive cards share the same component tree. */
public final class TechnologyTreeScreen extends Screen {
    private static final int GOLD=0xFFD8BD79, TEXT=0xFFE6E5D8, MUTED=0xFF96AAA9;
    private static final String[] ERAS={"前工业","蒸汽工业","电气化","石化工业","信息时代"};
    private static final String[] CONDITIONS={"基础生产默认可用","获得机器框架","获得电动机","获得原油","获得封装芯片"};
    private static final String[] ICONS={"minecraft:crafting_table","capitalismmod:machine_frame","capitalismmod:electric_motor","capitalismmod:crude_oil","capitalismmod:packaged_chip"};
    private ModularUI ui;
    private UIElement root;
    private int selected, mask=-1, ticks, mapBottom, mapRight, detailScroll;
    private double panX, panY, zoom=1;
    private boolean dirty, details;

    public TechnologyTreeScreen() { super(Component.literal("文明 · 科技图谱")); }
    @Override protected void init() { clamp(); rebuild(); request(); }
    private void request() { if(minecraft.getConnection()!=null) PacketDistributor.sendToServer(new TechnologySnapshotPayload(-1)); }
    public void acceptSnapshot(int value) { if(mask!=value) { mask=value; dirty=true; } }
    private boolean unlocked(int era) { return mask>=0 && (mask & (1<<era))!=0; }
    private String state(int era) { return mask<0 ? "正在同步" : unlocked(era) ? "已解锁" : "尚未解锁"; }
    private int tone(int era) { return unlocked(era)?0xFF79BDA9:MUTED; }
    private int sx(double x) { return (int)Math.round(12+x*zoom-panX); }
    private int sy(double y) { return (int)Math.round(78+y*zoom-panY); }
    private boolean narrow() { return width<620; }
    private void clamp() {
        mapRight=narrow()?width-10:width-218;
        mapBottom=height-31;
        panX=Math.max(0,Math.min(panX,Math.max(0,1080*zoom-(mapRight-12))));
        panY=Math.max(0,Math.min(panY,Math.max(0,330*zoom-(mapBottom-78))));
    }
    private void rebuild() {
        dirty=false;
        if(ui!=null) ui.onRemoved();
        clearWidgets();
        clamp();
        root=new UIElement().layout(l->l.width(width).height(height));
        place(new UIElement().style(s->s.background(new Atlas())),0,0,width,height);
        label("C I V I L I Z A T I O N   /   工业文明",14,9,Math.max(80,width-90),GOLD,9);
        label("科技图谱",14,25,160,TEXT,15);
        button("×",width-34,9,24,22,this::onClose);
        int tabW=Math.max(1,(width-24)/5);
        for(int i=0;i<5;i++) {
            final int era=i;
            button(ERAS[i],12+i*tabW,52,tabW-3,19,()->{
                selected=era; panX=era*216*zoom; panY=0; detailScroll=0; dirty=true;
            });
        }
        // Only fully visible cards receive input; the atlas texture clips the routes.
        for(int i=0;i<5;i++) {
            final int era=i;
            int x=sx(i*216+14), y=sy(45), w=(int)(174*zoom), h=(int)(65*zoom);
            if(x>=12 && x+w<=mapRight && y>=76 && y+h<=mapBottom) {
                Button card=button("     "+ERAS[i]+"\n     "+state(i),x,y,w,h,()->{
                    selected=era; details=true; detailScroll=0; dirty=true;
                });
                card.buttonStyle(s->s.baseTexture(new Frame(era==selected?0xFF304B50:0xFF1D333B,era==selected?GOLD:tone(era)))
                        .hoverTexture(new Frame(0xFF3C5559,GOLD)));
                place(new UIElement().style(s->s.background(new Emblem(ICONS[era]))),x+7,y+h/2-10,20,20);
            }
            int bx=sx(i*216+14), by=sy(175), bw=(int)(174*zoom);
            if(bx>=12 && bx+bw<=mapRight && by>=76 && by+49<=mapBottom) {
                button("产业设备  ·  "+machines(i).size()+" 类",bx,by,bw,43,()->{
                    selected=era; details=true; detailScroll=0; dirty=true;
                });
            }
        }
        button("−",12,height-25,23,19,()->setZoom(zoom-.1));
        button("+",39,height-25,23,19,()->setZoom(zoom+.1));
        button("复位",66,height-25,36,19,()->{panX=panY=0;zoom=1;dirty=true;});
        label(Math.round(zoom*100)+"% · 右键拖动 / 滚轮横移",110,height-20,Math.max(1,width-115),MUTED,8);
        if(!narrow() || details) drawDetails();
        ui=ModularUI.of(UI.of(root)); ui.setScreenAndInit(this); addRenderableWidget(ui.getWidget());
    }
    private java.util.List<MachineType> machines(int era) {
        return java.util.Arrays.stream(MachineType.values()).filter(m->m!=MachineType.NONE &&
                TechnologyEra.forMachine(m.id()).ordinal()==era).toList();
    }
    private void drawDetails() {
        int x=narrow()?12:width-207, y=78, w=narrow()?width-24:195;
        panel(x,y,w,Math.max(1,height-y-33),new Frame(0xFF162B33,0xFF53666A));
        label("时代档案 / 0"+(selected+1),x+12,y+10,w-55,GOLD,9);
        if(narrow()) button("×",x+w-30,y+5,23,20,()->{details=false;dirty=true;});
        java.util.List<String> lines=new java.util.ArrayList<>();
        lines.add(ERAS[selected]+" · "+state(selected));
        lines.add("");
        lines.add("解锁条件："+CONDITIONS[selected]);
        lines.add("共享规则：任一在线玩家完成对应进阶，机器即可使用该时代能力。");
        lines.add("");
        lines.add("关联设备（按当前运行规则）");
        for(MachineType machine:machines(selected)) {
            String key="machine.capitalismmod."+machine.id();
            String name=Component.translatable(key).getString();
            lines.add("• "+(name.equals(key)?machine.id().replace('_',' '):name));
        }
        java.util.List<String> wrapped=new java.util.ArrayList<>();
        for(String line:lines) {
            if(line.isEmpty()) { wrapped.add("");continue; }
            while(!line.isEmpty()) {
                String part=font.plainSubstrByWidth(line,Math.max(8,w-24));
                if(part.isEmpty()) break;
                wrapped.add(part); line=line.substring(part.length());
            }
        }
        int rows=Math.max(1,(height-y-80)/12);
        detailScroll=Math.max(0,Math.min(detailScroll,Math.max(0,wrapped.size()-rows)));
        for(int n=0;n<rows && n+detailScroll<wrapped.size();n++)
            label(wrapped.get(n+detailScroll),x+12,y+33+n*12,w-24,TEXT,9);
        label("滚轮浏览详情",x+12,height-49,w-24,MUTED,8);
    }
    private void setZoom(double value) { zoom=Math.max(.65,Math.min(1.4,value));dirty=true; }
    @Override public void tick() { if(++ticks%100==0) request(); if(dirty) rebuild(); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean mouseDragged(double x,double y,int b,double dx,double dy) {
        if(b==1 && x<mapRight && y>75 && y<mapBottom && !(narrow()&&details)) {
            panX-=dx;panY-=dy;dirty=true;return true;
        }
        return super.mouseDragged(x,y,b,dx,dy);
    }
    @Override public boolean mouseScrolled(double x,double y,double dx,double dy) {
        if((!narrow()&&x>=width-207)||(narrow()&&details)) detailScroll-=(int)Math.signum(dy)*3;
        else if(hasControlDown()) setZoom(zoom+dy*.1);
        else panX-=dy*48+dx*48;
        dirty=true;return true;
    }
    @Override public void removed() { if(ui!=null) {ui.onRemoved();ui=null;} super.removed(); }
    @Override public void renderBackground(GuiGraphics g,int x,int y,float p) {}
    private void place(UIElement e,int x,int y,int w,int h) {
        e.layout(l->l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(w).height(h));root.addChild(e);
    }
    private void panel(int x,int y,int w,int h,ColorRectTexture t) { place(new UIElement().style(s->s.background(t)),x,y,w,h); }
    private void label(String t,int x,int y,int w,int c,int size) {
        place(new Label().setText(Component.literal(t)).textStyle(s->s.textColor(c).fontSize(size).textShadow(false)),x,y,w,size+4);
    }
    private Button button(String t,int x,int y,int w,int h,Runnable action) {
        Button b=new Button().setText(Component.literal(t)).textStyle(s->s.textColor(TEXT).fontSize(9).textShadow(false))
                .buttonStyle(s->s.baseTexture(new Frame(0xFF203840,0xFF526469)).hoverTexture(new Frame(0xFF385258,GOLD)))
                .setOnClick(e->action.run());
        place(b,x,y,w,h);return b;
    }
    private static class Frame extends ColorRectTexture {
        private final int border;
        Frame(int color,int border) {super(color);this.border=border;}
        @Override protected void drawInternal(GuiGraphics g,float mx,float my,float x,float y,float w,float h,float p) {
            int l=(int)x,t=(int)y,r=(int)(x+w),b=(int)(y+h);
            g.fill(l+3,t,r-3,b,border);g.fill(l,t+3,r,b-3,border);
            g.fill(l+4,t+1,r-4,b-1,color);g.fill(l+1,t+4,r-1,b-4,color);
        }
    }
    private static final class Emblem extends ColorRectTexture {
        private final String id;
        Emblem(String id) {super(0);this.id=id;}
        @Override protected void drawInternal(GuiGraphics g,float mx,float my,float x,float y,float w,float h,float p) {
            g.renderItem(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)).getDefaultInstance(),(int)x+2,(int)y+2);
        }
    }
    private final class Atlas extends ColorRectTexture {
        Atlas() {super(0);}
        @Override protected void drawInternal(GuiGraphics g,float mx,float my,float x,float y,float w,float h,float p) {
            g.fillGradient(0,0,width,height,0xFF0C202A,0xFF203F46);
            g.enableScissor(12,76,mapRight,mapBottom);
            for(int i=0;i<5;i++) {
                int left=sx(i*216),right=sx((i+1)*216);
                g.fill(left,76,right,mapBottom,i%2==0?0x201B424B:0x204A6865);
                g.vLine(left,76,mapBottom,0x445D7E80);
                int cx=sx(i*216+101), top=sy(110), bottom=sy(175);
                g.vLine(cx,top,bottom,0xFF688A87);
                if(i<4) {
                    int a=sx(i*216+188),b=sx((i+1)*216+14),ly=sy(77);
                    g.hLine(a,b,ly,unlocked(i+1)?GOLD:0xFF57777A);
                    g.hLine(b-4,b,ly-1,GOLD);g.hLine(b-2,b,ly-2,GOLD);
                }
            }
            g.disableScissor();
            g.fill(0,0,width,48,0xF011252E);
            g.hLine(12,width-12,47,0xFF827958);
        }
    }
}
