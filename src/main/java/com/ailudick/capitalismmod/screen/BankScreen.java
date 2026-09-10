package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.bank.BankTransaction;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.menu.BankMenu;
import com.ailudick.capitalismmod.network.payload.*;
import com.lowdragmc.lowdraglib2.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Button;
import com.lowdragmc.lowdraglib2.gui.ui.elements.Label;
import com.lowdragmc.lowdraglib2.gui.ui.elements.TextField;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** One LDLib2 tree owns layout and input; the existing BankMenu owns server snapshots. */
public final class BankScreen extends AbstractContainerScreen<BankMenu> {
    private enum Page {
        HOME("账户总览"), DEPOSIT("存入现金"), WITHDRAW("提取现金"), TRANSFER("转账"),
        LOAN("申请贷款"), REPAY("偿还贷款"), TERM("定期存款"), FX("货币兑换"),
        SAVINGS("我的定期"), HISTORY("交易流水"), CARDS("账户管理");
        final String title;
        Page(String title) { this.title = title; }
    }
    private static final int INK=0xFF172B40, MUTED=0xFF64758B, TEAL=0xFF087F8C;
    private Page page=Page.HOME;
    private String accountId, amount="", recipient="", error="";
    private Currency currency=Currencies.CNY, destination=Currencies.USD;
    private int termDays=3, offset;
    private boolean reveal, dirty;
    private boolean choosingAccount;
    private ModularUI ui;
    private UIElement root;
    private Map<String, BankAccount> snapshot;
    private CustomPacketPayload pending;
    private String confirmation="";
    private int contentX, contentW;

    public BankScreen(BankMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected void init() {
        imageWidth=Math.max(300, Math.min(600, width-12));
        imageHeight=Math.max(226, Math.min(360, height-12));
        super.init();
        rebuild();
    }

    private void rebuild() {
        dirty=false;
        if (ui!=null) ui.onRemoved();
        clearWidgets();
        snapshot=menu.getAccounts();
        if (accountId==null || !snapshot.containsKey(accountId))
            accountId=snapshot.keySet().stream().sorted().findFirst().orElse(null);
        root=new UIElement().layout(l->l.width(imageWidth).height(imageHeight))
                .style(s->s.background(new ColorRectTexture(0xFFF3F6FA)));
        int nav=imageWidth<420?76:106;
        contentX=nav+14;
        contentW=imageWidth-contentX-14;
        panel(0,0,nav,imageHeight,0xFF102B3F);
        label("CAPITAL",10,13,nav-16,0xFF73D4CA,11);
        label("BANK",10,29,nav-16,0xFFFFFFFF,9);
        Page[] pages={Page.HOME,Page.DEPOSIT,Page.TRANSFER,Page.LOAN,Page.TERM,Page.FX,Page.HISTORY,Page.CARDS};
        String[] names={"账户总览","现金存取","转账付款","贷款还款","定期储蓄","货币兑换","交易记录","账户管理"};
        int row=Math.min(31,(imageHeight-58)/pages.length);
        for(int i=0;i<pages.length;i++) {
            Page p=pages[i];
            boolean active=p==section();
            Button item=button(names[i],6,51+i*row,nav-12,row-3,active,()->navigate(p));
            if(!active) item.textStyle(s->s.textColor(0xFFBACCD8))
                    .buttonStyle(s->s.baseTexture(new ColorRectTexture(0xFF102B3F))
                            .hoverTexture(new BankSurface(0xFF254557)));
            else panel(6,55+i*row,2,row-11,0xFF79E5C1);
        }
        label(page.title,contentX,13,contentW-34,INK,13);
        button("×",imageWidth-32,8,24,24,false,this::onClose);
        if(pending!=null) {
            confirmation();
        } else if(choosingAccount) {
            accountPicker();
        } else {
            String card=accountId==null?"暂无账户 · 点击开户":
                    (account().credit()?"信用账户  ":"借记账户  ")+masked(accountId);
            button(card+"  ▾",contentX,39,contentW-42,24,false,()->{choosingAccount=true;dirty=true;});
            button(reveal?"隐藏":"显示",imageWidth-52,39,38,24,false,()->{reveal=!reveal; dirty=true;});
            if(page==Page.CARDS) cards();
            else if(account()==null) {
                label("开立账户后即可使用银行服务",contentX,88,contentW,MUTED,9);
                button("开立我的账户",contentX,113,contentW,27,true,()->navigate(Page.CARDS));
            } else switch(page) {
                case HOME -> overview();
                case HISTORY -> history();
                case SAVINGS -> savings();
                default -> form();
            }
            label(error.isEmpty()?"CAPITAL BANK  /  个人银行":error,
                    contentX,imageHeight-19,contentW,error.isEmpty()?MUTED:
                            error.startsWith("请求已提交")?TEAL:0xFFBA3E3E,9);
        }
        ui=ModularUI.of(UI.of(root));
        ui.setScreenAndInit(this);
        addRenderableWidget(ui.getWidget());
    }

    private void navigate(Page next) {
        page=next; offset=0; error=""; pending=null; choosingAccount=false; dirty=true;
        if(next==Page.DEPOSIT || next==Page.WITHDRAW) currency=Currencies.CNY;
    }

    private Page section() {
        return switch(page) {
            case WITHDRAW -> Page.DEPOSIT;
            case REPAY -> Page.LOAN;
            case SAVINGS -> Page.TERM;
            default -> page;
        };
    }

    private void accountPicker() {
        List<String> ids=menu.getAccounts().keySet().stream().sorted().toList();
        label("选择办理业务的账户",contentX,48,contentW,INK,11);
        int rows=Math.max(1,(imageHeight-113)/29);
        offset=Math.min(offset,Math.max(0,ids.size()-rows));
        for(int i=offset;i<Math.min(ids.size(),offset+rows);i++) {
            String id=ids.get(i);
            BankAccount a=menu.getAccounts().get(id);
            button((a.credit()?"信用":"借记")+"  "+masked(id)+(id.equals(accountId)?"  · 当前":""),
                    contentX,72+(i-offset)*29,contentW,25,id.equals(accountId),()->{
                        accountId=id;choosingAccount=false;offset=0;error="";dirty=true;
                    });
        }
        if(ids.isEmpty()) label("还没有账户，先开立一张银行卡",contentX,84,contentW,MUTED,9);
        button("返回",contentX,imageHeight-37,contentW/2-4,24,false,()->{choosingAccount=false;offset=0;dirty=true;});
        button("开立账户",contentX+contentW/2+4,imageHeight-37,contentW/2-4,24,true,()->navigate(Page.CARDS));
    }

    private BankAccount account() { return accountId==null?null:menu.getAccounts().get(accountId); }

    private boolean buyingForeign() {
        String base=Config.CROSS_BORDER_BASE_CURRENCY.get();
        return currency.id().equals(Currencies.exists(base)?base:Currencies.CNY.id());
    }

    private void overview() {
        int w=(contentW-8)/2;
        currencyTabs(71);
        panel(contentX,99,contentW,58,0xFFDFF1EB);
        label("当前账户可用余额",contentX+12,107,contentW-24,TEAL,9);
        label(Money.format(account().getBalance(currency.id()))+" "+currency.id().toUpperCase(),
                contentX+12,125,contentW-24,INK,17);
        button("转账付款",contentX,166,w,27,true,()->navigate(Page.TRANSFER));
        button("存入现金",contentX+w+8,166,w,27,false,()->navigate(Page.DEPOSIT));
        if(imageHeight>=290) {
            panel(contentX,201,w,43,0xFFFFFFFF);
            panel(contentX+w+8,201,w,43,0xFFFFFFFF);
            label("待还贷款 · "+currency.id(),contentX+9,208,w-18,MUTED,9);
            label(Money.format(account().getDebt(currency.id())),contentX+9,224,w-18,INK,10);
            label("我的定期",contentX+w+17,208,w-18,MUTED,9);
            button(account().termDeposits().size()+" 笔  ›",contentX+w+17,223,w-18,18,false,()->navigate(Page.SAVINGS));
        }
        if(imageHeight>=330) {
            label("最近交易",contentX,256,contentW-76,INK,10);
            button("查看全部",imageWidth-82,251,68,22,false,()->navigate(Page.HISTORY));
            transactions(279,Math.max(1,(imageHeight-306)/25));
        }
    }

    private void currencyTabs(int y) {
        int count=Currencies.ALL.size(), w=(contentW-(count-1)*4)/count;
        for(int i=0;i<count;i++) {
            Currency c=Currencies.ALL.get(i);
            button(c.id().toUpperCase(),contentX+i*(w+4),y,w,22,c.equals(currency),()->{
                currency=c;
                if(destination.equals(c)) destination=next(destination,c);
                dirty=true;
            });
        }
    }

    private int serviceTabs() {
        Page left=null,right=null;
        if(section()==Page.DEPOSIT) { left=Page.DEPOSIT;right=Page.WITHDRAW; }
        if(section()==Page.LOAN) { left=Page.LOAN;right=Page.REPAY; }
        if(section()==Page.TERM) { left=Page.TERM;right=Page.SAVINGS; }
        if(left==null) return 73;
        final Page a=left,b=right;
        button(a.title,contentX,71,contentW/2-4,22,page==a,()->navigate(a));
        button(b.title,contentX+contentW/2+4,71,contentW/2-4,22,page==b,()->navigate(b));
        return 99;
    }

    private void form() {
        int start=serviceTabs();
        boolean cash=page==Page.DEPOSIT || page==Page.WITHDRAW;
        if(cash) label("人民币 CNY",contentX,start+6,contentW/2-4,TEAL,9);
        else button("币种  "+currency.id().toUpperCase()+"  ›",contentX,start,contentW/2-4,23,false,this::nextCurrency);
        if(page==Page.TERM && imageHeight<270)
            button("期限 "+termDays+" 天  ›",contentX+contentW/2+4,start,contentW/2-4,23,false,()->{
                termDays=termDays==3?7:termDays==7?30:3;dirty=true;
            });
        else label("余额 "+Money.format(account().getBalance(currency.id())),
                    contentX+contentW/2+4,start+7,contentW/2-4,MUTED,9);
        int y=start+30;
        if(page==Page.TRANSFER) {
            field("收款卡号",recipient,contentX,y,contentW,v->recipient=v,true);
            y+=38;
        } else if(page==Page.FX) {
            button("到账币种  "+destination.id().toUpperCase()+"  ›",contentX,y,contentW,24,false,()->{
                destination=next(destination,currency); dirty=true;
            });
            y+=34;
        } else if(page==Page.TERM && imageHeight>=270) {
            int bw=(contentW-8)/3;
            for(int i=0;i<3;i++) {
                int days=new int[]{3,7,30}[i];
                button(days+" 天",contentX+i*(bw+4),y,bw,22,days==termDays,()->{termDays=days;dirty=true;});
            }
            y+=30;
        } else if((page==Page.LOAN || page==Page.REPAY) && imageHeight>=260) {
            label("待还 "+Money.format(account().getDebt(currency.id()))+" · "+
                    (account().loanDaysRemaining()<0?"已逾期":account().loanDaysRemaining()+" 天"),
                    contentX,y,contentW,MUTED,9);
            y+=20;
        }
        field(page==Page.FX?(buyingForeign()?"到账金额（"+destination.id()+"）":"支出金额（"+currency.id()+"）"):
                "金额（整单位）",amount,contentX,y,contentW,v->amount=v,false);
        y+=39;
        button("下一步 · 核对"+page.title,contentX,y,contentW,25,true,this::prepare);
        if(imageHeight-y>=93) {
            panel(contentX,y+38,contentW,32,0xFFE8EEF4);
            label(page==Page.TERM?"到期自动结算；提前支取不计利息":
                    cash?"现金直接与背包中的人民币物品兑换":
                    page==Page.TRANSFER?"下一步会显示完整收款卡号，请仔细核对":
                    page==Page.FX?"按当前汇率办理，请核对金额对应的币种":"贷款与还款以当前账户状态为准",
                    contentX+8,y+49,contentW-16,MUTED,9);
        }
    }

    private void savings() {
        serviceTabs();
        var deposits=account().termDeposits();
        label("提前支取将放弃利息",contentX,102,contentW,0xFFA66A24,9);
        int rows=Math.max(1,(imageHeight-163)/36);
        offset=Math.min(offset,Math.max(0,deposits.size()-1));
        if(deposits.isEmpty()) label("暂无定期，点击「定期存款」开始储蓄",contentX,131,contentW,MUTED,9);
        for(int i=offset;i<Math.min(deposits.size(),offset+rows);i++) {
            int index=i,y=122+(i-offset)*36;
            var d=deposits.get(i);
            panel(contentX,y,contentW,32,0xFFFFFFFF);
            label(Money.format(d.principal())+" "+d.currencyId(),contentX+7,y+3,contentW-69,INK,9);
            label("剩余 "+d.daysRemaining()+" 天",contentX+7,y+17,contentW-69,MUTED,8);
            button("支取",imageWidth-67,y+4,46,24,false,()->request(
                    new WithdrawTermDepositPayload(accountId,index),
                    "提前支取 "+Money.format(d.principal())+" "+d.currencyId()+"，放弃利息"));
        }
        pagination(deposits.size(),rows);
    }

    private void cards() {
        label("银行自动发放绑定账户的银行卡",contentX,78,contentW,MUTED,9);
        button("开立借记账户",contentX,100,contentW,27,true,()->request(
                new OpenAccountPayload(false),"开立借记账户（最多 3 个）"));
        button("开立信用账户",contentX,134,contentW,27,false,()->request(
                new OpenAccountPayload(true),"开立信用账户（最多 1 个）"));
        if(account()!=null) button("挂失并补办当前银行卡",contentX,171,contentW,25,false,
                ()->request(new ReplaceCardPayload(accountId),"挂失并补办卡号 "+masked(accountId)));
    }

    private void history() {
        label("最近 "+account().transactions().size()+" 笔 · 最新在前",contentX,77,contentW,MUTED,9);
        int rows=Math.max(1,(imageHeight-151)/25);
        transactions(98,rows);
        int count=account().transactions().size();
        pagination(count,rows);
    }

    private void pagination(int count,int rows) {
        int w=(contentW-60)/2;
        button("上一页",contentX,imageHeight-49,w,23,false,()->{
            offset=Math.max(0,offset-rows);dirty=true;
        }).setActive(offset>0);
        label((offset/rows+1)+" / "+Math.max(1,(count+rows-1)/rows),contentX+w+6,imageHeight-42,48,MUTED,9);
        button("下一页",imageWidth-14-w,imageHeight-49,w,23,false,()->{
            if(offset+rows<count) offset+=rows;dirty=true;
        }).setActive(offset+rows<count);
    }

    private void transactions(int y,int rows) {
        List<BankTransaction> list=account().transactions();
        if(list.isEmpty()) { label("暂无交易记录",contentX,y,contentW,MUTED,9); return; }
        offset=Math.min(offset,Math.max(0,list.size()-1));
        for(int i=0;i<rows && list.size()-1-offset-i>=0;i++) {
            var t=list.get(list.size()-1-offset-i);
            String day=t.occurredAt()<0?"旧记录":"第"+(t.occurredAt()/24000+1)+"天";
            int rowY=y+i*25, moneyW=Math.min(contentW*3/5,180);
            panel(contentX,rowY,contentW,24,i%2==0?0xFFFFFFFF:0xFFEDF2F6);
            label(transactionName(t.type()),contentX+6,rowY+2,contentW-moneyW-12,INK,9);
            label(day,contentX+6,rowY+14,contentW-moneyW-12,MUTED,7);
            String money=(t.amount()>0?"+":"")+Money.format(t.amount())+" "+t.currencyId().toUpperCase();
            label(money,imageWidth-20-moneyW,rowY+7,moneyW,t.amount()>=0?TEAL:INK,9);
        }
    }

    private String transactionName(String type) {
        return switch(type) {
            case "deposit" -> "现金存入";
            case "withdraw" -> "现金取出";
            case "transfer_in" -> "转账收入";
            case "transfer_out" -> "转账支出";
            case "exchange" -> "货币兑换";
            case "loan" -> "贷款发放";
            case "repay" -> "贷款还款";
            case "interest", "deposit_interest" -> "存款利息";
            case "loan_interest" -> "贷款利息";
            case "term_deposit", "term_open" -> "存入定期";
            case "term_maturity" -> "定期到期";
            case "term_withdraw" -> "定期支取";
            default -> type;
        };
    }

    private void prepare() {
        try {
            long n=Long.parseLong(amount);
            if(n<=0 || n>Long.MAX_VALUE/Money.MINOR_UNITS_PER_UNIT) throw new NumberFormatException();
            if(account()==null) { error="请选择账户";dirty=true;return; }
            String id=accountId, c=currency.id();
            if(page==Page.TRANSFER && (!recipient.matches("[0-9]{19}") || recipient.equals(id))) {
                error="请输入其他账户的 19 位卡号";dirty=true;return;
            }
            if(page==Page.LOAN && !account().credit()) {
                error="请先切换至信用账户";dirty=true;return;
            }
            CustomPacketPayload payload=switch(page) {
                case DEPOSIT, WITHDRAW -> new BankTransactionPayload(id,c,n,page==Page.DEPOSIT);
                case TRANSFER -> new TransferPayload(UUID.randomUUID(),id,recipient,c,n);
                case LOAN, REPAY -> new LoanPayload(id,c,n,page==Page.REPAY);
                case TERM -> new OpenTermDepositPayload(id,c,n,termDays);
                case FX -> new ExchangePayload(id,c,destination.id(),n);
                default -> null;
            };
            if(payload!=null) request(payload,page.title+" "+n+" "+(page==Page.FX && buyingForeign()?destination.id():c)+
                    (page==Page.FX && buyingForeign()?"（到账金额，扣款币种 "+c+"）":"")+
                    (page==Page.TRANSFER?" → "+recipient:page==Page.FX?" → "+destination.id():
                            page==Page.TERM?" · "+termDays+"天":""));
        } catch(NumberFormatException e) { error="请输入有效的正整数金额";dirty=true; }
    }

    private void request(CustomPacketPayload payload,String summary) {
        pending=payload; confirmation=summary;error="";dirty=true;
    }

    private void confirmation() {
        label("填写信息   /   02 核对并提交",contentX,45,contentW,TEAL,9);
        panel(contentX,68,contentW,91,0xFFFFFFFF);
        String rest=confirmation;
        for(int i=0;i<4 && !rest.isEmpty();i++) {
            String line=font.plainSubstrByWidth(rest,contentW-20);
            label(line,contentX+10,79+i*15,contentW-20,INK,9);
            rest=rest.substring(line.length());
        }
        label("操作账户 "+(accountId==null?"新账户":masked(accountId)),contentX+10,143,contentW-20,MUTED,9);
        button("返回修改",contentX,imageHeight-57,contentW/2-4,28,false,()->{pending=null;dirty=true;});
        button("确认提交",contentX+contentW/2+4,imageHeight-57,contentW/2-4,28,true,()->{
            CustomPacketPayload payload=pending;
            pending=null; dirty=true; error="请求已提交，请查看服务器操作提示";
            if(payload!=null) PacketDistributor.sendToServer(payload);
        });
    }

    private Currency next(Currency current,Currency exclude) {
        int index=Currencies.ALL.indexOf(current);
        for(int i=1;i<=Currencies.ALL.size();i++) {
            Currency c=Currencies.ALL.get((index+i)%Currencies.ALL.size());
            if(!c.equals(exclude)) return c;
        }
        return current;
    }
    private void nextCurrency() { currency=next(currency,page==Page.FX?destination:null);dirty=true; }
    private String masked(String id) { return reveal?id:"•••• "+id.substring(Math.max(0,id.length()-4)); }

    private void place(UIElement element,int x,int y,int w,int h) {
        element.layout(l->l.positionType(TaffyPosition.ABSOLUTE).left(x).top(y).width(w).height(h));
        root.addChild(element);
    }
    private void panel(int x,int y,int w,int h,int color) {
        place(new UIElement().style(s->s.background(new BankSurface(color))),x,y,w,h);
    }
    private void label(String text,int x,int y,int w,int color,int size) {
        String visible=font.plainSubstrByWidth(text,Math.max(1,w*9/size));
        place(new Label().setText(Component.literal(visible)).textStyle(s->s.textColor(color).fontSize(size).textShadow(false)),
                x,y,w,size+5);
    }
    private Button button(String text,int x,int y,int w,int h,boolean primary,Runnable action) {
        Button b=new Button().setText(Component.literal(text))
                .textStyle(s->s.textColor(primary?0xFFFFFFFF:INK).fontSize(9).textShadow(false))
                .buttonStyle(s->s.baseTexture(new BankSurface(primary?TEAL:0xFFE3EBF2))
                        .hoverTexture(new BankSurface(primary?0xFF129DA7:0xFFCDDFEB))
                        .pressedTexture(new BankSurface(0xFF6DB8BC)))
                .setOnClick(e->action.run());
        place(b,x,y,w,h);
        return b;
    }
    private void field(String title,String value,int x,int y,int w,
                       java.util.function.Consumer<String> responder,boolean card) {
        label(title,x,y,w,MUTED,9);
        TextField field=new TextField().setText(value)
                .textFieldStyle(s->s.textColor(INK).fontSize(10))
                .setTextValidator(s->s.matches(card?"[0-9]{0,19}":"[0-9]{0,18}"))
                .setTextResponder(responder);
        field.style(s->s.background(new BankSurface(0xFFFFFFFF)));
        place(field,x,y+13,w,22);
    }

    @Override protected void containerTick() {
        super.containerTick();
        if(snapshot!=menu.getAccounts()) dirty=true;
        if(dirty) rebuild();
    }
    @Override public void removed() {
        if(ui!=null) { ui.onRemoved();ui=null; }
        super.removed();
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy) {
        if(choosingAccount && sy!=0) {
            offset=Math.max(0,offset-(int)Math.signum(sy));dirty=true;return true;
        }
        if(page==Page.SAVINGS && account()!=null && sy!=0) {
            offset=Math.max(0,Math.min(account().termDeposits().size()-1,offset-(int)Math.signum(sy)));
            dirty=true;return true;
        }
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override protected void renderBg(GuiGraphics graphics,float tick,int x,int y) {}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y) {}

    /** Subtle pixel-rounded surfaces, drawn at the same scale as the UI. */
    private static final class BankSurface extends ColorRectTexture {
        BankSurface(int color) { super(color); }
        @Override protected void drawInternal(GuiGraphics graphics,float mouseX,float mouseY,
                                               float x,float y,float width,float height,float partialTick) {
            int l=Math.round(x),t=Math.round(y),r=Math.round(x+width),b=Math.round(y+height);
            int radius=Math.min(3,Math.min((r-l)/2,(b-t)/2));
            graphics.fill(l+radius,t,r-radius,b,color);
            graphics.fill(l,t+radius,r,b-radius,color);
            if(radius>1) graphics.fill(l+1,t+1,r-1,b-1,color);
        }
    }
}
