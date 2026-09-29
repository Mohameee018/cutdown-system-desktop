package Clothes_system;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.*;
import java.util.*;

public class ReportsPanel extends JPanel {
    private final Color BG=new Color(248,250,249), DARK=new Color(10,45,45), TEAL=new Color(25,105,100), TEXT=new Color(35,45,45);
    private final JComboBox<String> periodBox=new JComboBox<>(new String[]{"This Month","Last Month","Last 3 Months","This Year"});
    private final JLabel sales=new JLabel(), expenses=new JLabel(), profit=new JLabel(), orders=new JLabel();
    private final DefaultTableModel monthlyModel=new DefaultTableModel(new String[]{"Month","Sales","Cost","Expenses","Gross Profit","Net Profit","Orders","Returns"},0);
    private final JTable monthlyTable=new JTable(monthlyModel);

    public ReportsPanel(){
        setLayout(new BorderLayout());setBackground(BG);
        JPanel header=new JPanel(new BorderLayout());header.setOpaque(false);header.setBorder(BorderFactory.createEmptyBorder(25,35,20,35));
        JPanel tp=new JPanel();tp.setOpaque(false);tp.setLayout(new BoxLayout(tp,BoxLayout.Y_AXIS));JLabel t=new JLabel("Reports");t.setFont(new Font("Segoe UI",Font.BOLD,30));tp.add(t);JLabel sub=new JLabel("Analyze sales, expenses and store performance");sub.setForeground(new Color(115,125,125));tp.add(sub);header.add(tp,BorderLayout.WEST);
        JPanel filter=new JPanel(new FlowLayout(FlowLayout.RIGHT));filter.setOpaque(false);filter.add(new JLabel("Period:"));filter.add(periodBox);JButton refresh=new JButton("Refresh");refresh.addActionListener(e->refreshData());filter.add(refresh);header.add(filter,BorderLayout.EAST);add(header,BorderLayout.NORTH);
        JPanel content=new JPanel();content.setOpaque(false);content.setLayout(new BoxLayout(content,BoxLayout.Y_AXIS));content.setBorder(BorderFactory.createEmptyBorder(0,35,30,35));
        JPanel cards=new JPanel(new GridLayout(1,4,18,0));cards.setOpaque(false);cards.add(card("Total Sales",sales));cards.add(card("Total Expenses",expenses));cards.add(card("Net Profit",profit));cards.add(card("Total Orders",orders));content.add(cards);content.add(Box.createVerticalStrut(20));
        monthlyTable.setRowHeight(40);monthlyTable.setAutoCreateRowSorter(true);monthlyTable.setShowGrid(false);content.add(panel("Monthly Performance",new JScrollPane(monthlyTable)));
        add(new JScrollPane(content),BorderLayout.CENTER);periodBox.addActionListener(e->refreshData());refreshData();
    }
    private JPanel card(String title,JLabel value){JPanel p=new JPanel(new BorderLayout());p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createEmptyBorder(16,18,16,18));JLabel a=new JLabel(title);a.setForeground(new Color(115,125,125));value.setFont(new Font("Segoe UI",Font.BOLD,22));p.add(a,BorderLayout.NORTH);p.add(value,BorderLayout.CENTER);return p;}
    private JPanel panel(String title,Component c){JPanel p=new JPanel(new BorderLayout(0,12));p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(230,235,233)),BorderFactory.createEmptyBorder(20,22,20,22)));JLabel t=new JLabel(title);t.setFont(new Font("Segoe UI",Font.BOLD,18));t.setForeground(DARK);p.add(t,BorderLayout.NORTH);p.add(c,BorderLayout.CENTER);p.setPreferredSize(new Dimension(900,420));return p;}
    private String money(double x){return String.format(Locale.US,"EGP %,.0f",x);}
    public void refreshData(){DateRange r=range();Metrics m=metrics(r.from,r.to);sales.setText(money(m.sales));expenses.setText(money(m.expenses));profit.setText(money(m.sales-m.cost-m.expenses));orders.setText(String.valueOf(m.orders));fillMonthly();}
    private static class DateRange{Date from,to;DateRange(Date a,Date b){from=a;to=b;}}
    private DateRange range(){Calendar c=Calendar.getInstance();Date now=c.getTime();String s=String.valueOf(periodBox.getSelectedItem());if("Last Month".equals(s)){c.set(Calendar.DAY_OF_MONTH,1);c.add(Calendar.MONTH,-1);Date a=start(c);c.add(Calendar.MONTH,1);c.add(Calendar.DAY_OF_MONTH,-1);return new DateRange(a,end(c));}if("Last 3 Months".equals(s)){c.set(Calendar.DAY_OF_MONTH,1);c.add(Calendar.MONTH,-2);return new DateRange(start(c),end(Calendar.getInstance()));}if("This Year".equals(s)){c.set(Calendar.MONTH,Calendar.JANUARY);c.set(Calendar.DAY_OF_MONTH,1);return new DateRange(start(c),end(Calendar.getInstance()));}c.set(Calendar.DAY_OF_MONTH,1);return new DateRange(start(c),end(Calendar.getInstance()));}
    private static Date start(Calendar c){Calendar x=(Calendar)c.clone();x.set(Calendar.HOUR_OF_DAY,0);x.set(Calendar.MINUTE,0);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);return x.getTime();}
    private static Date end(Calendar c){Calendar x=(Calendar)c.clone();x.set(Calendar.HOUR_OF_DAY,23);x.set(Calendar.MINUTE,59);x.set(Calendar.SECOND,59);x.set(Calendar.MILLISECOND,999);return x.getTime();}
    // Business rule (must match DashboardPanel.refreshData()'s Total
    // Orders KPI): Total Orders = non-cancelled orders within the
    // selected period. Cancelled orders are excluded here too so the
    // two screens never disagree on what counts as an order.
    private Metrics metrics(Date from,Date to){
        Metrics m=new Metrics();for(OrdersPanel.Order o:OrdersPanel.getOrders()){if(o.dateValue==null||o.dateValue.before(from)||o.dateValue.after(to)||"Cancelled".equalsIgnoreCase(o.orderStatus))continue;m.orders++;m.sales+=parse(o.total);for(OrdersPanel.OrderItem i:o.items)m.cost+=i.costPrice*i.quantity;}
        m.expenses=ExpenseManager.totalBetween(from,to);
        m.sales=Math.max(0,m.sales-ReturnsPanel.getReturnedSalesAmountBetween(from,to));
        m.cost=Math.max(0,m.cost-ReturnsPanel.getReturnedCostAmountBetween(from,to));
        return m;
    }
    private double parse(String s){try{return Double.parseDouble(s.replace("EGP","").replace(",","").trim());}catch(Exception e){return 0;}}
    private void fillMonthly(){
        monthlyModel.setRowCount(0);
        Calendar c=Calendar.getInstance();
        c.set(Calendar.DAY_OF_MONTH,1);

        for(int i=0;i<6;i++){
            Calendar monthStart=(Calendar)c.clone();
            Calendar monthEnd=(Calendar)c.clone();
            monthEnd.add(Calendar.MONTH,1);
            monthEnd.set(Calendar.DAY_OF_MONTH,1);
            monthEnd.add(Calendar.DAY_OF_MONTH,-1);

            Date from=start(monthStart);
            Date to=end(monthEnd);
            Metrics m=metrics(from,to);

            monthlyModel.addRow(new Object[]{
                    new SimpleDateFormat("MMMM yyyy",Locale.ENGLISH).format(monthStart.getTime()),
                    money(m.sales),
                    money(m.cost),
                    money(m.expenses),
                    money(m.sales-m.cost),
                    money(m.sales-m.cost-m.expenses),
                    m.orders,
                    ReturnsPanel.getReturnCountBetween(from,to)
            });

            c.add(Calendar.MONTH,-1);
        }
    }
    private static class Metrics{double sales,cost,expenses;int orders;}
}
