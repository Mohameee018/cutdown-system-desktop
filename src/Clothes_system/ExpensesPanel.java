package Clothes_system;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.*;
import java.util.*;

public class ExpensesPanel extends JPanel {
    private JTable table;
    private DefaultTableModel model;
    private JLabel totalLabel,monthLabel,weekLabel,countLabel;
    private final SimpleDateFormat fmt=new SimpleDateFormat("dd MMM yyyy",Locale.ENGLISH);
    { fmt.setLenient(false); } // reject invalid dates like "32 Jan 2026" instead of silently rolling them into a different valid date

    public ExpensesPanel(){
        setLayout(new BorderLayout()); setBackground(new Color(248,250,249));
        JPanel head=new JPanel(new BorderLayout()); head.setOpaque(false); head.setBorder(BorderFactory.createEmptyBorder(20,30,15,30));
        JPanel t=new JPanel();t.setOpaque(false);t.setLayout(new BoxLayout(t,BoxLayout.Y_AXIS));
        JLabel title=new JLabel("Expenses");title.setFont(new Font("Arial",Font.BOLD,24));t.add(title);JLabel sub=new JLabel("Track store expenses and business costs");sub.setForeground(new Color(140,150,150));t.add(sub);head.add(t,BorderLayout.WEST);
        JButton add=new JButton("+  New Expense"); add.addActionListener(e->showDialog(null));head.add(add,BorderLayout.EAST);add(head,BorderLayout.NORTH);
        JPanel content=new JPanel(new BorderLayout(0,15));content.setOpaque(false);content.setBorder(BorderFactory.createEmptyBorder(0,30,30,30));
        JPanel cards=new JPanel(new GridLayout(1,4,15,0));cards.setOpaque(false);
        totalLabel=new JLabel();monthLabel=new JLabel();weekLabel=new JLabel();countLabel=new JLabel();
        cards.add(card("Total Expenses",totalLabel));cards.add(card("This Month",monthLabel));cards.add(card("This Week",weekLabel));cards.add(card("Transactions",countLabel));content.add(cards,BorderLayout.NORTH);
        String[] cols={"Expense ID","Date","Category","Description","Payment","Amount","Status"};
        model=new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};
        table=new JTable(model);table.setRowHeight(46);table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPopupMenu menu=new JPopupMenu();JMenuItem edit=new JMenuItem("Edit");JMenuItem del=new JMenuItem("Delete");menu.add(edit);menu.add(del);table.setComponentPopupMenu(menu);
        edit.addActionListener(e->{int r=table.getSelectedRow();if(r>=0)showDialog(ExpenseManager.getExpenses().get(table.convertRowIndexToModel(r)));});
        del.addActionListener(e->{int r=table.getSelectedRow();if(r>=0){ExpenseManager.Expense x=ExpenseManager.getExpenses().get(table.convertRowIndexToModel(r));if(JOptionPane.showConfirmDialog(this,"Delete "+x.getId()+"?","Confirm",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION){ExpenseManager.delete(x);refreshData();}}});
        table.addMouseListener(new java.awt.event.MouseAdapter(){public void mousePressed(java.awt.event.MouseEvent e){if(e.isPopupTrigger())select(e); }public void mouseReleased(java.awt.event.MouseEvent e){if(e.isPopupTrigger())select(e);}private void select(java.awt.event.MouseEvent e){int r=table.rowAtPoint(e.getPoint());if(r>=0)table.setRowSelectionInterval(r,r);}});
        content.add(new JScrollPane(table),BorderLayout.CENTER);add(content,BorderLayout.CENTER);refreshData();
    }
    private JPanel card(String title,JLabel value){JPanel p=new JPanel(new BorderLayout());p.setBackground(Color.WHITE);p.setBorder(BorderFactory.createEmptyBorder(14,18,14,18));JLabel a=new JLabel(title);a.setForeground(new Color(130,145,145));value.setFont(new Font("Arial",Font.BOLD,21));p.add(a,BorderLayout.NORTH);p.add(value,BorderLayout.CENTER);return p;}
    public void refreshData(){
        model.setRowCount(0);for(ExpenseManager.Expense e:ExpenseManager.getExpenses())model.addRow(new Object[]{e.getId(),fmt.format(e.getDate()),e.getCategory(),e.getDescription(),e.getPayment(),String.format(Locale.US,"EGP %,.2f",e.getAmount()),e.getStatus()});
        Calendar c=Calendar.getInstance();Date now=c.getTime();Date monthStart=startOfMonth(now);Date weekStart=startOfDay(now);c.setTime(weekStart);c.add(Calendar.DAY_OF_MONTH,-6);weekStart=c.getTime();
        totalLabel.setText(money(ExpenseManager.total()));monthLabel.setText(money(ExpenseManager.totalBetween(monthStart,endOfDay(now))));weekLabel.setText(money(ExpenseManager.totalBetween(weekStart,endOfDay(now))));countLabel.setText(String.valueOf(ExpenseManager.getExpenses().size()));
    }
    private String money(double x){return String.format(Locale.US,"EGP %,.0f",x);}
    private Date startOfMonth(Date d){Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.DAY_OF_MONTH,1);return startOfDay(c.getTime());}
    private Date startOfDay(Date d){Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);return c.getTime();}
    private Date endOfDay(Date d){Calendar c=Calendar.getInstance();c.setTime(d);c.set(Calendar.HOUR_OF_DAY,23);c.set(Calendar.MINUTE,59);c.set(Calendar.SECOND,59);c.set(Calendar.MILLISECOND,999);return c.getTime();}
    private void showDialog(ExpenseManager.Expense old){
        JTextField date=new JTextField(old==null?fmt.format(new Date()):fmt.format(old.getDate()));
        JTextField cat=new JTextField(old==null?"":old.getCategory());JTextField desc=new JTextField(old==null?"":old.getDescription());
        JComboBox<String> pay=new JComboBox<>(new String[]{"Cash","Bank Transfer","Visa","Other"});if(old!=null)pay.setSelectedItem(old.getPayment());
        JTextField amount=new JTextField(old==null?"":String.valueOf(old.getAmount()));JComboBox<String> status=new JComboBox<>(new String[]{"Paid","Pending"});if(old!=null)status.setSelectedItem(old.getStatus());
        JPanel p=new JPanel(new GridLayout(0,2,8,8));p.add(new JLabel("Date (dd MMM yyyy)"));p.add(date);p.add(new JLabel("Category"));p.add(cat);p.add(new JLabel("Description"));p.add(desc);p.add(new JLabel("Payment"));p.add(pay);p.add(new JLabel("Amount"));p.add(amount);p.add(new JLabel("Status"));p.add(status);
        int result=JOptionPane.showConfirmDialog(this,p,old==null?"New Expense":"Edit Expense",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);if(result!=JOptionPane.OK_OPTION)return;
        try{Date d=fmt.parse(date.getText().trim());double a=Double.parseDouble(amount.getText().replace(",","").replace("EGP","").trim());if(a<=0)throw new Exception();if(old==null)ExpenseManager.add(d,cat.getText(),desc.getText(),String.valueOf(pay.getSelectedItem()),a,String.valueOf(status.getSelectedItem()));else ExpenseManager.update(old,d,cat.getText(),desc.getText(),String.valueOf(pay.getSelectedItem()),a,String.valueOf(status.getSelectedItem()));refreshData();Clothes_system.refreshAllDataViews();}catch(Exception ex){JOptionPane.showMessageDialog(this,"Invalid date, category or amount.","Invalid Expense",JOptionPane.ERROR_MESSAGE);}
    }
}
