package Clothes_system.cloud;

import Clothes_system.OrdersPanel;
import Clothes_system.ReturnsPanel;
import java.util.List;

public final class CutdownCloudReturnSyncService {
    private CutdownCloudReturnSyncService() {}

    public static void syncReturnAsync(OrdersPanel.Order order) {
        if (!CutdownCloudConfig.configured() || order == null || order.getId() == null || order.getId().isBlank()) return;

        Thread t = new Thread(() -> {
            try {
                List<ReturnsPanel.ReturnSnapshot> rows = ReturnsPanel.getReturnSnapshotsForOrder(order.getId());
                if (rows.isEmpty()) return;

                ReturnsPanel.ReturnSnapshot first = rows.get(0);
                StringBuilder json = new StringBuilder();
                json.append("{");
                json.append("\"order_id\":").append(q(order.getId())).append(",");
                json.append("\"reason\":").append(q(first.reason())).append(",");
                json.append("\"disposition\":").append(q(first.disposition())).append(",");
                json.append("\"amount\":").append(sumAmount(rows)).append(",");
                json.append("\"loss\":").append(sumLoss(rows));
                json.append("}");

                new CutdownCloudClient().postJson("/api/desktop/orders/return", json.toString());
            } catch (Exception e) {
                System.err.println("[Cutdown] Website return sync failed: " + e.getMessage());
            }
        }, "cutdown-return-sync");

        t.setDaemon(true);
        t.start();
    }

    private static double sumAmount(List<ReturnsPanel.ReturnSnapshot> rows) {
        double total = 0;
        for (ReturnsPanel.ReturnSnapshot r : rows) total += Math.max(0, r.amount());
        return Math.round(total * 100.0) / 100.0;
    }

    private static double sumLoss(List<ReturnsPanel.ReturnSnapshot> rows) {
        double total = 0;
        for (ReturnsPanel.ReturnSnapshot r : rows) total += Math.max(0, r.loss());
        return Math.round(total * 100.0) / 100.0;
    }

    private static String q(String value) {
        if (value == null) return "\"\"";
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "\\r").replace("\n", "\\n") + "\"";
    }
}
