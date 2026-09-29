package Clothes_system.cloud;

import Clothes_system.OrdersPanel;
import java.util.*;

public final class CutdownCloudReturnSyncService {
    private CutdownCloudReturnSyncService() {}

    public static void syncReturnAsync(OrdersPanel.Order order) {
        if (!CutdownCloudConfig.configured() || order == null || order.id == null || order.id.isBlank()) return;
        Thread t = new Thread(() -> {
            try {
                List<ReturnsPanelSnapshot> rows = new ArrayList<>();
                for (ReturnsPanel.ReturnSnapshot r : getSnapshotsForOrder(order.id)) rows.add(new ReturnsPanelSnapshot(r));
                StringBuilder json = new StringBuilder("{"order_id":"").append(escape(order.id)).append("","items":[");
                for (int n=0;n<rows.size();n++) {
                    if(n>0)json.append(',');
                    ReturnsPanelSnapshot r=rows.get(n);
                    json.append("{"return_id":"").append(escape(r.id)).append("","product_id":"").append(escape(r.productId))
                        .append("","product_name":"").append(escape(r.product)).append("","size":"").append(escape(r.size))
                        .append("","color":"").append(escape(r.color)).append("","quantity":").append(r.quantity)
                        .append(","amount":").append(r.amount).append(","reason":"").append(escape(r.reason))
                        .append("","disposition":"").append(escape(r.disposition)).append("","loss":").append(r.loss).append('}');
                }
                json.append("]}");
                new CutdownCloudClient().postJson("/api/desktop/orders/return", json.toString());
            } catch (Exception e) { System.err.println("[Cutdown] Website return sync failed: " + e.getMessage()); }
        }, "cutdown-return-sync");
        t.setDaemon(true); t.start();
    }

    private static List<ReturnsPanel.ReturnSnapshot> getSnapshotsForOrder(String id) {
        List<ReturnsPanel.ReturnSnapshot> out = new ArrayList<>();
        for (ReturnsPanel.ReturnSnapshot r : ReturnsPanel.getReturnSnapshots()) if (id.equals(r.orderId())) out.add(r);
        return out;
    }

    private static String escape(String s) {
        if(s==null)return "";
        return s.replace("\","\\").replace(""","\"");
    }
    private record ReturnsPanelSnapshot(String id,String product,String productId,String size,String color,int quantity,double amount,String reason,String disposition,double loss) {
        ReturnsPanelSnapshot(ReturnsPanel.ReturnSnapshot r){this(r.id(),r.product(),r.productId(),r.size(),r.color(),r.quantity(),r.amount(),r.reason(),r.disposition(),r.loss());}
    }
}
