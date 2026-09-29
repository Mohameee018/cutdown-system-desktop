package Clothes_system.cloud;

import Clothes_system.OrdersPanel;
import Clothes_system.db.PersistenceRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class CutdownCloudOrderSyncService {
    private static final DateTimeFormatter DISPLAY=DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm",Locale.ENGLISH);
    private CutdownCloudOrderSyncService(){}
    public static void syncOrdersAsync(Runnable done){
        if(!CutdownCloudConfig.configured())return;
        Thread t=new Thread(()->{try{
            Object root=MiniJson.parse(new CutdownCloudClient().get("/api/desktop/orders"));
            int count=0;if(root instanceof List<?> list)for(Object x:list)if(x instanceof Map<?,?> m)if(importOne(cast(m)))count++;
            int finalCount=count;javax.swing.SwingUtilities.invokeLater(()->{if(done!=null)done.run();System.out.println("[Cutdown] Website orders synced: "+finalCount);});
        }catch(Exception e){System.err.println("[Cutdown] Website order sync failed: "+e.getMessage());}},"cutdown-order-sync");t.setDaemon(true);t.start();
    }
    private static boolean importOne(Map<String,Object> o){
        String id=str(o,"id");if(id.isBlank())return false;
        String customer=str(o,"customer_name"),phone=str(o,"customer_phone"),address=str(o,"address"),city=str(o,"city");
        if(!city.isBlank())address=city+(address.isBlank()?"":", "+address);
        String payment="online".equalsIgnoreCase(str(o,"payment_method"))?"Online":"Cash";
        String status=mapWebsiteOrderStatus(str(o,"order_status"));
        String delivery=mapWebsiteDeliveryStatus(str(o,"delivery_status"));
        OrdersPanel.Order order=OrdersPanel.Order.persistenceCreate(id,customer,phone,"",address,displayDate(str(o,"created_at")),payment,status,delivery,0,0,"Website order");
        order.persistenceSetTotal(formatMoney(num(o,"total_amount")));
        Object items=o.get("order_items");if(items instanceof List<?> list)for(Object x:list)if(x instanceof Map<?,?>m){Map<String,Object>i=cast(m);
            order.persistenceAddItem(OrdersPanel.OrderItem.persistenceCreate(str(i,"product_name"),str(i,"product_id"),str(i,"sku"),str(i,"category"),str(i,"size"),str(i,"color"),num(i,"unit_price"),num(i,"cost_price"),Math.max(1,(int)num(i,"quantity"))));}
        PersistenceRepository.saveOrder(order);synchronized(OrdersPanel.getOrders()){OrdersPanel.getOrders().removeIf(e->id.equals(e.getId()));OrdersPanel.getOrders().add(order);}return true;
    }
    @SuppressWarnings("unchecked")private static Map<String,Object>cast(Map<?,?>m){return(Map<String,Object>)(Map<?,?>)m;}
    private static String str(Map<String,Object>m,String k){Object v=m.get(k);return v==null?"":String.valueOf(v);}
    private static double num(Map<String,Object>m,String k){Object v=m.get(k);if(v instanceof Number)return((Number)v).doubleValue();try{return Double.parseDouble(str(m,k));}catch(Exception e){return 0;}}
    private static String displayDate(String s){try{return DISPLAY.format(Instant.parse(s).atZone(ZoneId.systemDefault()));}catch(Exception e){return s==null||s.isBlank()?DISPLAY.format(java.time.ZonedDateTime.now()):s;}}
    private static String mapWebsiteOrderStatus(String value){
        String v=value==null?"":value.trim().toLowerCase(Locale.ROOT);
        return switch(v){
            case "preparing" -> "Preparing";
            case "prepared" -> "Prepared";
            case "completed" -> "Completed";
            case "pending","confirmed","" -> "Not Prepared";
            default -> "Not Prepared";
        };
    }
    private static String mapWebsiteDeliveryStatus(String value){
        String v=value==null?"":value.trim().toLowerCase(Locale.ROOT);
        return switch(v){
            case "pending","" -> "Pending";
            case "shipped","with shipping company" -> "With Shipping Company";
            case "out for delivery" -> "Out for Delivery";
            case "delivered" -> "Delivered";
            case "returned" -> "Returned";
            default -> "Pending";
        };
    }
    private static String formatMoney(double v){return Math.abs(v-Math.rint(v))<.005?String.format(Locale.US,"EGP %,.0f",v):String.format(Locale.US,"EGP %,.2f",v);}
    public static void syncStatusAsync(String orderId, String orderStatus, String deliveryStatus) {
        if (!CutdownCloudConfig.configured() || orderId == null || orderId.isBlank()) return;
        Thread t = new Thread(() -> {
            try {
                String json = "{\"order_id\":\"" + escape(orderId) + "\",\"order_status\":\"" + escape(orderStatus) + "\",\"delivery_status\":\"" + escape(deliveryStatus) + "\"}";
                new CutdownCloudClient().postJson("/api/desktop/orders/status", json);
            } catch (Exception e) { System.err.println("[Cutdown] Website order status sync failed: " + e.getMessage()); }
        }, "cutdown-order-status-sync");
        t.setDaemon(true); t.start();
    }
    private static String escape(String value) { if (value == null) return ""; return value.replace("\\\\", "\\\\\\\\").replace("\"", "\\\\\""); }
}
