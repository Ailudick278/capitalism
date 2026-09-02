package com.ailudick.capitalismmod.screen;

import com.ailudick.capitalismmod.Config;
import com.ailudick.capitalismmod.bank.BankAccount;
import com.ailudick.capitalismmod.bank.BankCardNumber;
import com.ailudick.capitalismmod.bank.BankTransaction;
import com.ailudick.capitalismmod.client.GuiStyles;
import com.ailudick.capitalismmod.currency.Currencies;
import com.ailudick.capitalismmod.currency.Currency;
import com.ailudick.capitalismmod.currency.ExchangeRates;
import com.ailudick.capitalismmod.currency.ExchangeRateProvider;
import com.ailudick.capitalismmod.currency.Money;
import com.ailudick.capitalismmod.wallet.EconomyHelper;
import com.ailudick.capitalismmod.menu.BankMenu;
import com.ailudick.capitalismmod.network.payload.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.util.List;

/** Standalone bank UI: home -> service -> account -> form -> confirmation. */
public final class BankScreen extends AbstractContainerScreen<BankMenu> {
    private enum Page { HOME,CASH,CREDIT,WEALTH,CROSS_BORDER,MY,ACCOUNT,DEPOSIT,WITHDRAW,TRANSFER,LOAN,REPAY,TERM,BUY_FX,SETTLE_FX,EXCHANGE_HISTORY,OPEN_ACCOUNT,REPLACE,TRANSACTIONS }
    private static final int X=140, W=300, ROWS=5;
    private Page page=Page.HOME, afterAccount=Page.HOME;
    private final List<String> ids=new ArrayList<>();
    private int selected=-1, scroll;
    private boolean reveal;
    private Currency currency=Currencies.USD, from=Currencies.CNY, to=Currencies.USD;
    private int term=3;
    private EditBox amount, exchangeAmount, target;

    public BankScreen(BankMenu menu, Inventory inventory, Component title) { super(menu,inventory,title); imageWidth=460; imageHeight=280; }
    @Override protected void init(){super.init(); rebuild();}
    private void rebuild(){clearWidgets(); amount=null; exchangeAmount=null; target=null; sidebar(); switch(page){
        case HOME->home(); case CASH->cashMenu(); case CREDIT->creditMenu(); case WEALTH->wealthMenu(); case CROSS_BORDER->crossMenu(); case MY->myMenu();
        case ACCOUNT->accounts(); case DEPOSIT->cashForm(true); case WITHDRAW->cashForm(false); case TRANSFER->transfer(); case LOAN->creditForm(false); case REPAY->creditForm(true); case TERM->term(); case BUY_FX->exchange(false); case SETTLE_FX->exchange(true); case EXCHANGE_HISTORY->history(true); case OPEN_ACCOUNT->open(); case REPLACE->replace(); case TRANSACTIONS->history(false); }}
    private void sidebar(){
    }
    private void home(){service("gui.capitalismmod.cash_services",X,68,()->go(Page.CASH));service("gui.capitalismmod.transfer",X+152,68,()->pick(Page.TRANSFER));service("gui.capitalismmod.credit_services",X,116,()->go(Page.CREDIT));service("gui.capitalismmod.wealth_management",X+152,116,()->pick(Page.TERM));service("gui.capitalismmod.cross_border",X,164,()->go(Page.CROSS_BORDER));}
    private void service(String k,int x,int y,Runnable r){button(k,x,y,140,34,r);}
    private void button(String k,int x,int y,int w,int h,Runnable r){addRenderableWidget(Button.builder(Component.translatable(k),b->r.run()).bounds(leftPos+x,topPos+y,w,h).build());}
    private void cashMenu(){button("gui.capitalismmod.deposit",X,76,140,30,()->pick(Page.DEPOSIT));button("gui.capitalismmod.withdraw",X+152,76,140,30,()->pick(Page.WITHDRAW));back(Page.HOME);}
    private void creditMenu(){button("gui.capitalismmod.loan",X,76,140,30,()->pick(Page.LOAN));button("gui.capitalismmod.repay",X+152,76,140,30,()->pick(Page.REPAY));back(Page.HOME);}
    private void wealthMenu(){button("gui.capitalismmod.term_deposit",X,76,140,30,()->pick(Page.TERM));back(Page.HOME);}
    private void crossMenu(){button("gui.capitalismmod.buy_foreign_currency",X,76,140,30,()->pick(Page.BUY_FX));button("gui.capitalismmod.settle_foreign_currency",X+152,76,140,30,()->pick(Page.SETTLE_FX));button("gui.capitalismmod.exchange_transactions",X,118,W,24,()->pick(Page.EXCHANGE_HISTORY));back(Page.HOME);}
    private void myMenu(){button("gui.capitalismmod.open_account",X,76,140,30,()->go(Page.OPEN_ACCOUNT));button("gui.capitalismmod.report_loss",X+152,76,140,30,()->pick(Page.REPLACE));button("gui.capitalismmod.transactions",X,118,140,30,()->pick(Page.TRANSACTIONS));back(Page.HOME);}
    private void accounts(){refresh();int end=Math.min(ids.size(),scroll+ROWS);for(int i=scroll;i<end;i++){String id=ids.get(i);BankAccount a=menu.getAccounts().get(id);Component t=Component.translatable(a!=null&&a.credit()?"gui.capitalismmod.credit":"gui.capitalismmod.debit");buttonText(Component.empty().append(t).append(Component.literal("    ")).append(Component.literal(spaced(id))),X,76+(i-scroll)*28,W,22,()->choose(id));}back(Page.HOME);}
    private void buttonText(Component c,int x,int y,int w,int h,Runnable r){addRenderableWidget(Button.builder(c,b->r.run()).bounds(leftPos+x,topPos+y,w,h).build());}
    private void pick(Page p){afterAccount=p;scroll=0;go(Page.ACCOUNT);}
    private void choose(String id){refresh();selected=ids.indexOf(id);if(selected>=0)go(afterAccount);}
    private void header(){button("gui.capitalismmod.change_account",308,42,132,20,()->pick(page));button(reveal?"gui.capitalismmod.hide_account_number":"gui.capitalismmod.show_account_number",308,66,132,20,()->{reveal=!reveal;rebuild();});}
    private void cashForm(boolean deposit){currency=Currencies.CNY;header();amount=amount(108);for(int i=0;i<3;i++){int value=new int[]{100,500,1000}[i];buttonText(Component.literal(String.valueOf(value)),X+i*52,138,48,20,()->setAmount(value));}buttonText(Component.literal("全部"),X+156,138,48,20,()->setAmount(allCashAmount(deposit)));action(deposit?"gui.capitalismmod.deposit":"gui.capitalismmod.withdraw",218,108,()->transaction(deposit));back(Page.CASH);}
    private void creditForm(boolean repay){header();currencies(92);amount=amount(140);action(repay?"gui.capitalismmod.repay":"gui.capitalismmod.loan",218,140,()->loan(repay));back(Page.CREDIT);}
    private void transfer(){header();currencies(92);target=new EditBox(font,leftPos+X,topPos+126,180,20,Component.translatable("gui.capitalismmod.target_account"));target.setMaxLength(19);target.setFilter(s->s.matches("\\d*"));addRenderableWidget(target);amount=amount(154);action("gui.capitalismmod.transfer",218,154,this::sendTransfer);back(Page.HOME);}
    private void term(){header();currencies(92);amount=amount(140);int[] ds={3,7,30};for(int i=0;i<3;i++){int d=ds[i];buttonText(Component.translatable("gui.capitalismmod.days",d),X+i*52,168,48,20,()->term=d);}action("gui.capitalismmod.open_term",X,196,this::openTerm);back(Page.WEALTH);}
    private void exchange(boolean settle){header();Currency base=base();from=settle?Currencies.USD:base;to=settle?base:Currencies.USD;buttonText(line("gui.capitalismmod.source_currency",from),X,92,W,24,()->cycle(true));buttonText(line("gui.capitalismmod.target_currency",to),X,122,W,24,()->cycle(false));exchangeAmount=new EditBox(font,leftPos+X,topPos+152,130,22,Component.translatable("gui.capitalismmod.exchange_amount"));exchangeAmount.setFilter(s->s.isEmpty()||s.matches("\\d+"));exchangeAmount.setValue("1");addRenderableWidget(exchangeAmount);action("gui.capitalismmod.exchange",278,152,this::sendExchange);if(!settle)buttonText(Component.literal("当前余额最多可购买: "+availableExchangeAmount()+" "+Component.translatable(to.nameKey()).getString()),X,180,W,20,()->{});back(Page.CROSS_BORDER);}
    private Component line(String k,Currency c){return Component.translatable(k).append("    ").append(Component.translatable(c.nameKey())).append("  >");}
    private void cycle(boolean source){if(source){from=next(from,to);}else{to=next(to,from);}rebuild();}
    private Currency next(Currency c,Currency other){int n=Currencies.ALL.indexOf(c);for(int i=1;i<=Currencies.ALL.size();i++){Currency x=Currencies.ALL.get((n+i)%Currencies.ALL.size());if(!x.equals(other))return x;}return c;}
    private void history(boolean fx){header();back(Page.CROSS_BORDER);}
    private void open(){button("gui.capitalismmod.open_debit",X,82,140,30,()->openAccount(false));button("gui.capitalismmod.open_credit",X+152,82,140,30,()->openAccount(true));back(Page.MY);}
    private void replace(){header();action("gui.capitalismmod.report_loss",X,92,()->confirm(Component.translatable("gui.capitalismmod.report_loss"),Component.translatable("gui.capitalismmod.report_loss_confirm",spaced(selectedAccountId())),()->PacketDistributor.sendToServer(new ReplaceCardPayload(selectedAccountId()))));back(Page.MY);}
    private void currencies(int y){for(int i=0;i<Currencies.ALL.size();i++){Currency c=Currencies.ALL.get(i);buttonText(Component.translatable(c.nameKey()),X+i*52,y,48,20,()->currency=c);}}
    private EditBox amount(int y){EditBox b=new EditBox(font,leftPos+X,topPos+y,70,22,Component.translatable("gui.capitalismmod.amount"));b.setFilter(s->s.isEmpty()||s.matches("\\d+"));b.setValue("1");addRenderableWidget(b);return b;}
    private void action(String k,int x,int y,Runnable r){button(k,x,y,112,22,r);}
    private void back(Page p){button("gui.capitalismmod.back",X,244,92,20,()->go(p));}
    private void go(Page p){page=p;rebuild();}
    private String selectedAccountId(){refresh();return selected>=0&&selected<ids.size()?ids.get(selected):null;}
    private void transaction(boolean dep){String id=selectedAccountId();Long n=number(amount);if(id==null||n==null||n<=0)return;confirm(Component.translatable(dep?"gui.capitalismmod.deposit":"gui.capitalismmod.withdraw"),Component.translatable("gui.capitalismmod.transaction_confirm",Component.translatable(dep?"gui.capitalismmod.deposit":"gui.capitalismmod.withdraw"),n,Component.translatable(currency.nameKey())),()->PacketDistributor.sendToServer(new BankTransactionPayload(id,currency.id(),n,dep)));}
    private void loan(boolean repay){String id=selectedAccountId();Long n=number(amount);if(id==null||n==null||n<=0)return;String k=repay?"gui.capitalismmod.repay":"gui.capitalismmod.loan";confirm(Component.translatable(k),Component.translatable(repay?"gui.capitalismmod.repay_confirm":"gui.capitalismmod.loan_confirm",n,Component.translatable(currency.nameKey())),()->PacketDistributor.sendToServer(new LoanPayload(id,currency.id(),n,repay)));}
    private void sendTransfer(){String id=selectedAccountId();Long n=number(amount);if(id==null||n==null||n<=0||target==null||target.getValue().isEmpty())return;confirm(Component.translatable("gui.capitalismmod.transfer"),Component.translatable("gui.capitalismmod.transfer_confirm",n,Component.translatable(currency.nameKey()),target.getValue()),()->PacketDistributor.sendToServer(new TransferPayload(id,target.getValue(),currency.id(),n)));}
    private void openTerm(){String id=selectedAccountId();Long n=number(amount);if(id==null||n==null||n<=0)return;confirm(Component.translatable("gui.capitalismmod.open_term"),Component.translatable("gui.capitalismmod.open_term_confirm",n,Component.translatable(currency.nameKey()),term),()->PacketDistributor.sendToServer(new OpenTermDepositPayload(id,currency.id(),n,term)));}
    private void sendExchange(){String id=selectedAccountId();Long n=number(exchangeAmount);if(id==null||n==null||n<=0||from.equals(to))return;confirm(Component.translatable("gui.capitalismmod.exchange"),Component.translatable("gui.capitalismmod.exchange_confirm",n,Component.translatable(from.nameKey()),Component.translatable(to.nameKey())),()->PacketDistributor.sendToServer(new ExchangePayload(id,from.id(),to.id(),n)));}
    private void openAccount(boolean credit){confirm(Component.translatable(credit?"gui.capitalismmod.open_credit":"gui.capitalismmod.open_debit"),Component.translatable(credit?"gui.capitalismmod.open_credit_confirm":"gui.capitalismmod.open_debit_confirm"),()->PacketDistributor.sendToServer(new OpenAccountPayload(credit)));}
    private void confirm(Component t,Component m,Runnable r){Minecraft.getInstance().setScreen(new ConfirmScreen(ok->{if(ok)r.run();Minecraft.getInstance().setScreen(this);},t,m));}
    private Long number(EditBox b){if(b==null)return null;try{return Long.parseLong(b.getValue());}catch(NumberFormatException e){return null;}}
    private void refresh(){ids.clear();ids.addAll(menu.getAccounts().keySet());if(ids.isEmpty())selected=-1;else if(selected<0)selected=0;else selected=Math.min(selected,ids.size()-1);}
    private Currency base(){String id=Config.CROSS_BORDER_BASE_CURRENCY.get();return Currencies.exists(id)?Currencies.byId(id):Currencies.CNY;}
    private String spaced(String id){return BankCardNumber.format(id).replace('-', ' ');}
    private String shown(String id){if(reveal)return spaced(id);String digits=id.replaceAll("\\D","");if(digits.length()<8)return "****";return digits.substring(0,4)+" **** **** "+digits.substring(digits.length()-4);}
    private void setAmount(long value){if(amount!=null)amount.setValue(String.valueOf(Math.max(0,value)));}
    private long allCashAmount(boolean deposit){long minor;if(deposit){if(Minecraft.getInstance().player==null)return 0;minor=EconomyHelper.countItems(Minecraft.getInstance().player,Currencies.CNY);}else{BankAccount a=selectedAccount();minor=a==null?0:a.getBalance(Currencies.CNY.id());}return minor/Money.MINOR_UNITS_PER_UNIT;}
    @Override public boolean mouseClicked(double x,double y,int button){if(x>=leftPos+8&&x<leftPos+116&&y>=topPos+52&&y<topPos+76){go(Page.HOME);return true;}if(x>=leftPos+8&&x<leftPos+116&&y>=topPos+82&&y<topPos+106){go(Page.MY);return true;}return super.mouseClicked(x,y,button);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(page==Page.ACCOUNT&&ids.size()>ROWS&&sy!=0){refresh();scroll=Math.max(0,Math.min(ids.size()-ROWS,scroll-(int)Math.signum(sy)));rebuild();return true;}return super.mouseScrolled(x,y,sx,sy);}
    @Override protected void renderLabels(GuiGraphics g,int mx,int my){g.drawString(font,Component.translatable("gui.capitalismmod.my_services"),18,82,GuiStyles.ACCENT,false);}
    @Override protected void renderBg(GuiGraphics g,float p,int mx,int my){GuiStyles.drawBackground(g,leftPos,topPos,imageWidth,imageHeight);g.fill(leftPos+5,topPos+5,leftPos+imageWidth-5,topPos+34,0xCC14263A);g.fill(leftPos+5,topPos+40,leftPos+124,topPos+imageHeight-5,0x6620202A);g.fill(leftPos+128,topPos+40,leftPos+imageWidth-5,topPos+imageHeight-5,0x4420202A);}
    @Override public void render(GuiGraphics g,int mx,int my,float p){super.render(g,mx,my,p);g.drawString(font,Component.literal("CAPITAL BANK"),leftPos+16,topPos+13,GuiStyles.ACCENT,false);g.drawString(font,Component.translatable("gui.capitalismmod.bank_home"),leftPos+190,topPos+13,GuiStyles.TEXT,false);g.drawString(font,Component.translatable(pageTitle()),leftPos+X,topPos+50,GuiStyles.ACCENT,false);g.drawString(font,Component.translatable("gui.capitalismmod.service_center"),leftPos+18,topPos+54,GuiStyles.ACCENT,false);if(page!=Page.HOME&&page!=Page.ACCOUNT&&page!=Page.CASH&&page!=Page.CREDIT&&page!=Page.WEALTH&&page!=Page.CROSS_BORDER&&page!=Page.MY&&page!=Page.OPEN_ACCOUNT){String id=selectedAccountId();String accountText=id==null?Component.translatable("gui.capitalismmod.no_account").getString():shown(id);g.drawString(font,Component.translatable("gui.capitalismmod.selected_account",accountText),leftPos+X,topPos+58,GuiStyles.TEXT_DIM,false);if(id!=null)g.drawString(font,Component.translatable("gui.capitalismmod.selected_account_balance",balanceSummary(id)),leftPos+X,topPos+78,GuiStyles.TEXT_DIM,false);}if(page==Page.DEPOSIT||page==Page.WITHDRAW)g.drawString(font,Component.literal("操作币种：人民币"),leftPos+X,topPos+92,GuiStyles.TEXT,false);if(page==Page.ACCOUNT&&ids.isEmpty())g.drawString(font,Component.translatable("gui.capitalismmod.no_account"),leftPos+X,topPos+82,GuiStyles.TEXT_DIM,false);if(page==Page.CROSS_BORDER)rates(g);if(page==Page.TRANSACTIONS||page==Page.EXCHANGE_HISTORY)transactions(g);renderTooltip(g,mx,my);}
    private String pageTitle(){return switch(page){case HOME->"gui.capitalismmod.bank_home";case CASH,DEPOSIT,WITHDRAW->"gui.capitalismmod.cash_services";case CREDIT,LOAN,REPAY->"gui.capitalismmod.credit_services";case WEALTH,TERM->"gui.capitalismmod.wealth_management";case CROSS_BORDER,BUY_FX,SETTLE_FX,EXCHANGE_HISTORY->"gui.capitalismmod.cross_border";case MY,OPEN_ACCOUNT,REPLACE,TRANSACTIONS->"gui.capitalismmod.my_services";case ACCOUNT->"gui.capitalismmod.choose_account";case TRANSFER->"gui.capitalismmod.transfer";};}
    private void rates(GuiGraphics g){Currency b=base();String status=ExchangeRateProvider.isLive()?"实时汇率":"内置汇率";g.drawString(font,Component.literal("汇率更新时间: "+ExchangeRateProvider.lastUpdated()+"（"+status+"）"),leftPos+X,topPos+144,GuiStyles.TEXT_DIM,false);g.drawString(font,Component.literal("基准货币: "+Component.translatable(b.nameKey()).getString()),leftPos+X,topPos+158,GuiStyles.TEXT_DIM,false);g.drawString(font,Component.literal("币种    参考    买入    卖出"),leftPos+X,topPos+176,GuiStyles.ACCENT,false);int y=192;for(Currency c:Currencies.ALL)if(!c.equals(b)){long q=ExchangeRates.convert(100,c,b);long buy=Math.max(1,Math.round(q*.98));long sell=Math.max(1,Math.round(q*1.02));g.drawString(font,Component.literal(Component.translatable(c.nameKey()).getString()+"   "+Money.format(q)+"   "+Money.format(buy)+"   "+Money.format(sell)),leftPos+X,topPos+y,GuiStyles.TEXT,false);y+=14;}}
    private void transactions(GuiGraphics g){BankAccount a=selectedAccount();List<BankTransaction> l=a==null?List.of():a.transactions().stream().filter(t->page==Page.TRANSACTIONS||t.type().equals("exchange")).toList();if(l.isEmpty()){g.drawString(font,Component.translatable("gui.capitalismmod.no_transactions"),leftPos+X,topPos+88,GuiStyles.TEXT_DIM,false);return;}int y=84;for(int i=Math.max(0,l.size()-10);i<l.size();i++){BankTransaction t=l.get(i);g.drawString(font,Component.literal(t.type()+" "+(t.amount()>=0?"+":"")+Money.format(t.amount())+" "+t.currencyId()),leftPos+X,topPos+y,GuiStyles.TEXT,false);y+=13;}}
    private String balanceSummary(String id){BankAccount a=menu.getAccounts().get(id);if(a==null)return "-";StringBuilder s=new StringBuilder();for(Currency c:Currencies.ALL){long value=a.getBalance(c.id());if(value!=0){if(s.length()>0)s.append("，");s.append(Component.translatable(c.nameKey()).getString()).append(" ").append(Money.format(value));}}return s.length()==0?"人民币 0":s.toString();}
    private String availableExchangeAmount(){BankAccount a=selectedAccount();if(a==null||from.equals(to))return "0";return Money.format(ExchangeRates.convert(a.getBalance(from.id()),from,to));}
    private BankAccount selectedAccount(){String id=selectedAccountId();return id==null?null:menu.getAccounts().get(id);}
}
