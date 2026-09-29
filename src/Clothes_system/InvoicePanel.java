package Clothes_system;

import javax.swing.*;
import javax.print.attribute.HashPrintRequestAttributeSet;
import java.awt.*;
import java.awt.print.*;
import java.util.*;
import java.util.List;

/**
 * Invoice printing service. Invoice is intentionally not a separate
 * navigation destination; Orders opens it through the context menu.
 */
public class InvoicePanel extends JPanel {
    private final JComboBox<String> orderBox = new JComboBox<>();
    private final JTextArea preview = new JTextArea();
    private final JComboBox<String> paperBox = new JComboBox<>(new String[]{"A4","58mm","80mm"});

    public InvoicePanel() {
        setLayout(new BorderLayout(15,15));
        setBackground(new Color(248,250,249));
        JPanel head=new JPanel(new BorderLayout(10,10)); head.setOpaque(false);
        JLabel title=new JLabel("Invoice"); title.setFont(new Font("Segoe UI",Font.BOLD,28)); head.add(title,BorderLayout.WEST);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.RIGHT)); controls.setOpaque(false);
        controls.add(new JLabel("Order:")); controls.add(orderBox);
        JButton previewBtn=new JButton("Preview"), printBtn=new JButton("Print"); controls.add(previewBtn); controls.add(printBtn); head.add(controls,BorderLayout.EAST); add(head,BorderLayout.NORTH);
        preview.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12)); preview.setEditable(false); preview.setMargin(new Insets(20,20,20,20)); add(new JScrollPane(preview),BorderLayout.CENTER);
        JPanel bottom=new JPanel(new FlowLayout(FlowLayout.RIGHT)); bottom.setOpaque(false); bottom.add(new JLabel("Paper Size:")); paperBox.setSelectedItem(SettingsManager.getPaperSize()); bottom.add(paperBox); add(bottom,BorderLayout.SOUTH);
        previewBtn.addActionListener(e->refreshPreview()); printBtn.addActionListener(e->{OrdersPanel.Order o=selectedOrder(); if(o!=null) printOrder(o,String.valueOf(paperBox.getSelectedItem()),this);});
        orderBox.addActionListener(e->refreshPreview()); refreshOrders();
    }
    public void refreshData(){refreshOrders();refreshPreview();}
    private void refreshOrders(){String selected=(String)orderBox.getSelectedItem();orderBox.removeAllItems();for(OrdersPanel.Order o:OrdersPanel.getOrders())orderBox.addItem(o.id);if(selected!=null)orderBox.setSelectedItem(selected);if(orderBox.getSelectedIndex()<0&&orderBox.getItemCount()>0)orderBox.setSelectedIndex(orderBox.getItemCount()-1);refreshPreview();}
    private OrdersPanel.Order selectedOrder(){Object id=orderBox.getSelectedItem();if(id==null)return null;for(OrdersPanel.Order o:OrdersPanel.getOrders())if(o.id.equals(id))return o;return null;}
    private void refreshPreview(){OrdersPanel.Order o=selectedOrder();if(o==null){preview.setText("No order selected.");return;}preview.setText(buildInvoiceText(o,false));preview.setCaretPosition(0);}

    public static String buildInvoiceText(OrdersPanel.Order o, boolean packingSlip){
        if(o==null)return "Order not found.";
        StringBuilder s=new StringBuilder();
        s.append(SettingsManager.getStoreName()).append('\n');
        if(!SettingsManager.getStoreType().isBlank()) s.append(SettingsManager.getStoreType()).append('\n');
        if(!SettingsManager.getStorePhone().isBlank()) s.append(SettingsManager.getStorePhone()).append('\n');
        if(!SettingsManager.getStoreAddress().isBlank()) s.append(SettingsManager.getStoreAddress()).append('\n');
        s.append('\n').append(packingSlip ? "PACKING SLIP" : "INVOICE").append('\n');
        s.append("ORDER: ").append(o.id).append('\n');
        s.append("DATE : ").append(o.date).append('\n');
        s.append("CUSTOMER: ").append(o.customer).append('\n');
        s.append("PHONE: ").append(o.phone).append('\n');
        if(o.address!=null && !o.address.isBlank()) s.append("ADDRESS: ").append(o.address).append('\n');
        s.append("-----------------------------------------------\n");
        s.append(String.format("%-16s %-9s %-8s %-6s %-8s%n","PRODUCT","SKU","SIZE","COLOR","QTY"));
        for(OrdersPanel.OrderItem i:o.items){
            String color=i.color==null?"":i.color; String size=i.size==null?"":i.size;
            s.append(String.format(Locale.US,"%-16s %-9s %-8s %-8s %-6d%n",clip(i.name,16),clip(i.sku,9),clip(size,8),clip(color,8),i.quantity));
            if(!packingSlip) s.append(String.format(Locale.US,"   Price: %-10s  Line: %-10s%n",money(i.price),money(i.price*i.quantity)));
        }
        s.append("-----------------------------------------------\n");
        if(!packingSlip){
            s.append("Subtotal: ").append(money(o.getSubtotal())).append('\n');
            s.append("Discount: ").append(money(o.discount)).append('\n');
            s.append("Shipping: ").append(money(o.shipping)).append('\n');
            s.append("TOTAL: ").append(o.total).append('\n');
            s.append("Payment: ").append(o.payment).append('\n');
            s.append("\nReturn Policy: ").append(SettingsManager.getReturnPolicy()).append('\n');
        } else {
            s.append("Payment: ").append(o.payment).append('\n');
            s.append("TOTAL: ").append(o.total).append('\n');
        }
        s.append('\n').append(SettingsManager.getInvoiceFooter());
        return s.toString();
    }
    private static String clip(String s,int n){s=s==null?"":s;return s.length()<=n?s:s.substring(0,Math.max(0,n-1))+"…";}
    private static String money(double v){return String.format(Locale.US,"EGP %,.2f",v);}

    public static void printOrder(OrdersPanel.Order order, String paperSize, Component parent){
        if(order==null){showPrintError(parent,"Order does not exist.");return;}
        if(order.items==null||order.items.isEmpty()){showPrintError(parent,"Order has no items.");return;}
        printDocuments(Collections.singletonList(order),paperSize,false,parent);
    }

    public static void printInvoices(List<OrdersPanel.Order> orders, String paperSize, Component parent){
        if(orders==null||orders.isEmpty()){showPrintError(parent,"No orders to print.");return;}
        List<OrdersPanel.Order> valid=new ArrayList<>();
        for(OrdersPanel.Order o:orders) if(o!=null&&o.items!=null&&!o.items.isEmpty())valid.add(o);
        if(valid.isEmpty()){showPrintError(parent,"The selected/visible orders have no printable items.");return;}
        printDocuments(valid,paperSize,false,parent);
    }

    public static void printPackingSlips(List<OrdersPanel.Order> orders, String paperSize, Component parent){
        if(orders==null||orders.isEmpty()){showPrintError(parent,"No orders selected.");return;}
        List<OrdersPanel.Order> valid=new ArrayList<>();
        for(OrdersPanel.Order o:orders) if(o!=null&&o.items!=null&&!o.items.isEmpty())valid.add(o);
        if(valid.isEmpty()){showPrintError(parent,"Selected orders have no printable items.");return;}
        printDocuments(valid,paperSize,true,parent);
    }

    // =========================================================
    // PAGINATION
    // =========================================================
    /*
     * The renderer used to emit exactly one page per order and stop
     * drawing at the bottom margin, so any invoice longer than a page
     * lost its tail silently. Page layout is now computed up front as
     * pure data, which both fixes the truncation and makes the layout
     * testable without a printer.
     */

    /** One printed page: the order it belongs to and the lines on it. */
    public static class PrintPage {
        public final OrdersPanel.Order order;
        public final int pageNumber;      // 1-based, within this order
        public final int pageCount;       // total pages for this order
        public final List<String> lines;

        PrintPage(OrdersPanel.Order order,int pageNumber,int pageCount,List<String> lines){
            this.order=order; this.pageNumber=pageNumber; this.pageCount=pageCount; this.lines=lines;
        }
    }

    /** Splits one document's text into pages of at most linesPerPage lines. */
    public static List<List<String>> paginate(String document,int linesPerPage){
        List<List<String>> pages=new ArrayList<>();
        if(document==null) document="";
        if(linesPerPage<1) linesPerPage=1;
        String[] all=document.split("\\R",-1);
        // A trailing newline yields a final empty element; it carries no
        // content, so drop it rather than paying for an extra page.
        int count=all.length;
        while(count>1 && all[count-1].isEmpty()) count--;
        for(int i=0;i<count;i+=linesPerPage){
            List<String> page=new ArrayList<>();
            for(int j=i;j<Math.min(i+linesPerPage,count);j++) page.add(all[j]);
            pages.add(page);
        }
        if(pages.isEmpty()) pages.add(new ArrayList<>());
        return pages;
    }

    /** Full page plan for a batch of orders, in order, nothing dropped. */
    public static List<PrintPage> buildPrintPages(List<OrdersPanel.Order> orders,boolean packing,int linesPerPage){
        List<PrintPage> result=new ArrayList<>();
        if(orders==null) return result;
        for(OrdersPanel.Order o:orders){
            if(o==null) continue;
            List<List<String>> pages=paginate(buildInvoiceText(o,packing),linesPerPage);
            for(int i=0;i<pages.size();i++) result.add(new PrintPage(o,i+1,pages.size(),pages.get(i)));
        }
        return result;
    }

    /** Lines that fit on one page for the given imageable height. */
    public static int linesPerPage(double imageableHeight,float lineHeight){
        int n=(int)Math.floor((imageableHeight-16)/lineHeight);
        return Math.max(1,n);
    }

    private static void printDocuments(List<OrdersPanel.Order> orders,String paperSize,boolean packing,Component parent){
        PrinterJob job=PrinterJob.getPrinterJob();
        job.setJobName(packing?"Packing Slips":"Invoice");
        Paper paper=new Paper();
        double width=595,height=842;
        if("58mm".equals(paperSize)){width=164;height=842;} else if("80mm".equals(paperSize)){width=227;height=842;}
        paper.setSize(width,height); paper.setImageableArea(8,8,Math.max(20,width-16),Math.max(20,height-16));
        PageFormat pf=job.defaultPage(); pf.setPaper(paper);
        final float lineHeight=11;
        final List<PrintPage> pages=buildPrintPages(orders,packing,
                linesPerPage(pf.getImageableHeight(),lineHeight));
        job.setPrintable((graphics,format,page)->{
            if(page<0||page>=pages.size())return Printable.NO_SUCH_PAGE;
            PrintPage pp=pages.get(page);
            Graphics2D g=(Graphics2D)graphics.create();
            g.setFont(new Font(Font.MONOSPACED,Font.PLAIN,"A4".equals(paperSize)?9:8));
            float x=(float)format.getImageableX(), y=(float)format.getImageableY()+12;
            for(String text:pp.lines){ g.drawString(text,x,y); y+=lineHeight; }
            if(pp.pageCount>1){
                g.drawString("Page "+pp.pageNumber+" of "+pp.pageCount,
                        x,(float)(format.getImageableY()+format.getImageableHeight()-2));
            }
            g.dispose(); return Printable.PAGE_EXISTS;
        },pf);
        try{
            if(job.printDialog()) job.print(new HashPrintRequestAttributeSet());
        }catch(Exception ex){showPrintError(parent,"Printer unavailable or paper configuration is invalid.\n"+ex.getMessage());}
    }
    private static void showPrintError(Component parent,String message){JOptionPane.showMessageDialog(parent,message,"Print Error",JOptionPane.ERROR_MESSAGE);}
}
