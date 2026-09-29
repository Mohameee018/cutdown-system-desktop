package Clothes_system.db;

import Clothes_system.CustomersPanel;
import Clothes_system.ExpenseManager;
import Clothes_system.InventoryPanel;
import Clothes_system.OrdersPanel;
import Clothes_system.Product;
import Clothes_system.ProductManager;
import Clothes_system.ReturnsPanel;
import Clothes_system.SettingsManager;

import javax.swing.table.DefaultTableModel;
import java.awt.Color;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

/**
 * Central persistence facade. Swing/business classes call this facade rather
 * than opening JDBC connections themselves.
 */
public final class PersistenceRepository {
    private PersistenceRepository() {}

    private static Connection c() { return DatabaseManager.getConnection(); }

    public static synchronized void loadProducts(ProductManager manager) {
        try {
            if (!isTableEmpty("products")) return;
            // no-op: caller will seed through its existing seed method
        } catch (SQLException e) { throw db(e); }
    }

    public static synchronized List<Product> readProducts() {
        List<Product> out = new ArrayList<>();
        try (PreparedStatement ps=c().prepareStatement(
                "SELECT id,sku,name,category,description,image_path,sale_price,cost_price,stock_quantity,minimum_stock,is_active FROM products ORDER BY rowid");
             ResultSet rs=ps.executeQuery()) {
            while(rs.next()) {
                Product p=new Product(rs.getString(1),rs.getString(2),rs.getString(3),
                        rs.getString(4),rs.getString(5),rs.getString(6),
                        rs.getDouble(7),rs.getDouble(8),rs.getInt(9),rs.getInt(10));
                if(!rs.getBoolean(11)) p.deactivate();
                p.getVariants().clear();
                loadVariants(p);
                loadPriceHistory(p);
                loadInventoryHistory(p);
                loadWarehouseStock(p);
                out.add(p);
            }
        } catch(SQLException e){throw db(e);}
        return out;
    }

    private static void loadVariants(Product p) throws SQLException {
        try(PreparedStatement ps=c().prepareStatement("SELECT variant_id,color,size,stock_quantity FROM product_variants WHERE product_id=? ORDER BY rowid")){
            ps.setString(1,p.getId());
            try(ResultSet rs=ps.executeQuery()){while(rs.next()) p.addVariant(new Product.ProductVariant(rs.getString(1),rs.getString(2),rs.getString(3),rs.getInt(4)));}
        }
    }
    private static void loadPriceHistory(Product p) throws SQLException {
        try(PreparedStatement ps=c().prepareStatement("SELECT new_sale_price,new_cost_price,changed_at,COALESCE(changed_by,'Price history') FROM product_price_history WHERE product_id=? ORDER BY id")){
            ps.setString(1,p.getId());
            try(ResultSet rs=ps.executeQuery()){
                p.getPriceHistory().clear();
                while(rs.next()) p.getPriceHistory().add(new Product.PriceHistoryEntry(rs.getDouble(1),rs.getDouble(2),new Date(rs.getLong(3)),rs.getString(4)));
            }
        }
    }
    private static void loadInventoryHistory(Product p) throws SQLException {
        try(PreparedStatement ps=c().prepareStatement("SELECT change_qty,resulting_stock,reference_type,reason,created_at FROM product_inventory_history WHERE product_id=? ORDER BY id")){
            ps.setString(1,p.getId());
            try(ResultSet rs=ps.executeQuery()){
                p.getInventoryHistory().clear();
                while(rs.next()) p.getInventoryHistory().add(new Product.InventoryHistoryEntry(rs.getInt(1),rs.getInt(2),rs.getString(3),rs.getString(4),new Date(rs.getLong(5))));
            }
        }
    }
    private static void loadWarehouseStock(Product p) throws SQLException {
        Map<String,Integer> wanted=new LinkedHashMap<>();
        try(PreparedStatement ps=c().prepareStatement("SELECT warehouse_id,quantity FROM product_warehouse_stock WHERE product_id=? ORDER BY rowid")){
            ps.setString(1,p.getId());
            try(ResultSet rs=ps.executeQuery()){while(rs.next()) wanted.put(rs.getString(1),rs.getInt(2));}
        }
        p.loadWarehouseStock(wanted);
    }

    public static synchronized void saveProduct(Product p) {
        if(p==null) return;
        try {
            c().setAutoCommit(false);
            saveProductBody(p);
            c().commit(); c().setAutoCommit(true);
        } catch(SQLException e){rollback();throw db(e);}
    }

    // Raw work of saveProduct with no transaction boundary of its own, so
    // saveAll() can run it as one statement inside a single larger
    // transaction instead of committing per product.
    private static void saveProductBody(Product p) throws SQLException {
            try(PreparedStatement ps=c().prepareStatement("""
                INSERT INTO products(id,sku,name,category,description,image_path,cost_price,sale_price,stock_quantity,minimum_stock,is_active,updated_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET sku=excluded.sku,name=excluded.name,category=excluded.category,
                description=excluded.description,image_path=excluded.image_path,cost_price=excluded.cost_price,
                sale_price=excluded.sale_price,stock_quantity=excluded.stock_quantity,minimum_stock=excluded.minimum_stock,
                is_active=excluded.is_active,updated_at=excluded.updated_at""")){
                int i=1; ps.setString(i++,p.getId());ps.setString(i++,p.getSku());ps.setString(i++,p.getName());
                ps.setString(i++,p.getCategory());ps.setString(i++,p.getDescription());ps.setString(i++,p.getImagePath());
                ps.setDouble(i++,p.getCostPrice());ps.setDouble(i++,p.getSellingPrice());ps.setInt(i++,p.getStockQuantity());
                ps.setInt(i++,p.getMinimumStock());ps.setInt(i++,p.isActive()?1:0);ps.setLong(i,System.currentTimeMillis());ps.executeUpdate();
            }
            try(PreparedStatement d=c().prepareStatement("DELETE FROM product_variants WHERE product_id=?")){d.setString(1,p.getId());d.executeUpdate();}
            try(PreparedStatement ins=c().prepareStatement("INSERT INTO product_variants(variant_id,product_id,color,size,stock_quantity) VALUES(?,?,?,?,?)")){
                for(Product.ProductVariant v:p.getVariants()){ins.setString(1,v.getId());ins.setString(2,p.getId());ins.setString(3,v.getColor());ins.setString(4,v.getSize());ins.setInt(5,v.getStockQuantity());ins.addBatch();}
                ins.executeBatch();
            }
            try(PreparedStatement d=c().prepareStatement("DELETE FROM product_warehouse_stock WHERE product_id=?")){d.setString(1,p.getId());d.executeUpdate();}
            try(PreparedStatement ins=c().prepareStatement("INSERT INTO product_warehouse_stock(product_id,warehouse_id,quantity,updated_at) VALUES(?,?,?,?)")){
                for(Map.Entry<String,Integer> e:p.getWarehouseBreakdown().entrySet()){ins.setString(1,p.getId());ins.setString(2,e.getKey());ins.setInt(3,e.getValue());ins.setLong(4,System.currentTimeMillis());ins.addBatch();}
                ins.executeBatch();
            }
            try(PreparedStatement d=c().prepareStatement("DELETE FROM product_price_history WHERE product_id=?")){d.setString(1,p.getId());d.executeUpdate();}
            try(PreparedStatement ins=c().prepareStatement("INSERT INTO product_price_history(product_id,new_sale_price,new_cost_price,changed_at,changed_by) VALUES(?,?,?,?,?)")){
                for(Product.PriceHistoryEntry h:p.getPriceHistory()){ins.setString(1,p.getId());ins.setDouble(2,h.getSellingPrice());ins.setDouble(3,h.getCostPrice());ins.setLong(4,h.getDate().getTime());ins.setString(5,h.getReason());ins.addBatch();}
                ins.executeBatch();
            }
            try(PreparedStatement d=c().prepareStatement("DELETE FROM product_inventory_history WHERE product_id=?")){d.setString(1,p.getId());d.executeUpdate();}
            try(PreparedStatement ins=c().prepareStatement("INSERT INTO product_inventory_history(product_id,change_qty,resulting_stock,reference_type,reason,created_at) VALUES(?,?,?,?,?,?)")){
                for(Product.InventoryHistoryEntry h:p.getInventoryHistory()){ins.setString(1,p.getId());ins.setInt(2,h.getQuantityChange());ins.setInt(3,h.getResultingStock());ins.setString(4,h.getType());ins.setString(5,h.getReason());ins.setLong(6,h.getDate().getTime());ins.addBatch();}
                ins.executeBatch();
            }
    }

    public static synchronized void deleteProduct(String id) { exec("DELETE FROM products WHERE id=?",id); }

    public static synchronized void loadWarehouses() {
        try {
            InventoryPanel.WarehouseManager.clearForPersistence();
            try(PreparedStatement ps=c().prepareStatement("SELECT id,name,location,is_active FROM warehouses ORDER BY rowid");
                ResultSet rs=ps.executeQuery()){
                while(rs.next()) InventoryPanel.WarehouseManager.addPersistedWarehouse(new InventoryPanel.Warehouse(rs.getString(1),rs.getString(2),rs.getString(3),rs.getBoolean(4)));
            }
            if(InventoryPanel.WarehouseManager.findById(InventoryPanel.WarehouseManager.DEFAULT_WAREHOUSE_ID)==null)
                InventoryPanel.WarehouseManager.addPersistedWarehouse(new InventoryPanel.Warehouse(InventoryPanel.WarehouseManager.DEFAULT_WAREHOUSE_ID,"Main Warehouse","",true));
        } catch(SQLException e){throw db(e);}
    }

    public static synchronized void saveWarehouses() {
        try {
            c().setAutoCommit(false);
            saveWarehousesBody();
            c().commit();c().setAutoCommit(true);
        }catch(SQLException e){rollback();throw db(e);}
    }

    private static void saveWarehousesBody() throws SQLException {
            // Upsert each currently-known warehouse instead of blindly
            // deleting the whole table first. A blanket DELETE FROM
            // warehouses cascades (via product_warehouse_stock's ON
            // DELETE CASCADE foreign key) and wipes every product's
            // persisted warehouse-stock breakdown even when no warehouse
            // actually changed. That used to fire on every single app
            // startup (loadWarehouses() is followed by saveWarehouses()
            // before products are loaded, to make sure a freshly
            // defaulted "Main Warehouse" gets persisted on a brand new
            // database) and silently collapsed every product's stock
            // back into just the Main Warehouse after every restart.
            // Only warehouses that are genuinely gone from memory are
            // deleted below, so the cascade now only ever fires for a
            // warehouse that was actually removed — which callers
            // already handle correctly by re-saving every product's new
            // (merged-into-Main-Warehouse) breakdown right after.
            java.util.List<InventoryPanel.Warehouse> current = InventoryPanel.WarehouseManager.getWarehouses();
            try(PreparedStatement ps=c().prepareStatement("""
                INSERT INTO warehouses(id,name,location,is_active) VALUES(?,?,?,?)
                ON CONFLICT(id) DO UPDATE SET name=excluded.name,location=excluded.location,is_active=excluded.is_active""")){
                for(InventoryPanel.Warehouse w:current){
                    ps.setString(1,w.getId());ps.setString(2,w.getName());ps.setString(3,w.getLocation());ps.setInt(4,w.isActive()?1:0);ps.addBatch();
                } ps.executeBatch();
            }
            if(current.isEmpty()){
                try(Statement d=c().createStatement()){d.executeUpdate("DELETE FROM warehouses");}
            } else {
                StringBuilder placeholders=new StringBuilder();
                for(int i=0;i<current.size();i++){ if(i>0) placeholders.append(','); placeholders.append('?'); }
                try(PreparedStatement del=c().prepareStatement("DELETE FROM warehouses WHERE id NOT IN ("+placeholders+")")){
                    int i=1; for(InventoryPanel.Warehouse w:current) del.setString(i++,w.getId());
                    del.executeUpdate();
                }
            }
    }

    public static synchronized void loadCustomers(DefaultTableModel model) {
        if(model==null) return;
        model.setRowCount(0);
        try(PreparedStatement ps=c().prepareStatement("SELECT id,name,phone,total_orders,total_spent,last_order,status FROM customers ORDER BY rowid");
            ResultSet rs=ps.executeQuery()){
            while(rs.next()) model.addRow(new Object[]{rs.getString(1),rs.getString(2),rs.getString(3),rs.getInt(4),
                    formatMoney(rs.getDouble(5)),rs.getString(6)==null?"-":rs.getString(6),rs.getString(7)});
        }catch(SQLException e){throw db(e);}
    }

    public static synchronized void saveCustomers(DefaultTableModel model) {
        if(model==null) return;
        try{
            c().setAutoCommit(false);
            saveCustomersBody(model);
            c().commit();c().setAutoCommit(true);
        }catch(SQLException e){rollback();throw db(e);}
    }

    private static void saveCustomersBody(DefaultTableModel model) throws SQLException {
            try(Statement d=c().createStatement()){d.executeUpdate("DELETE FROM customers");}
            try(PreparedStatement ps=c().prepareStatement("INSERT INTO customers(id,name,phone,total_orders,total_spent,last_order,status) VALUES(?,?,?,?,?,?,?)")){
                for(int r=0;r<model.getRowCount();r++){
                    ps.setString(1,String.valueOf(model.getValueAt(r,0)));ps.setString(2,String.valueOf(model.getValueAt(r,1)));
                    ps.setString(3,String.valueOf(model.getValueAt(r,2)));ps.setInt(4,intv(model.getValueAt(r,3)));
                    ps.setDouble(5,money(model.getValueAt(r,4)));ps.setString(6,String.valueOf(model.getValueAt(r,5)));
                    ps.setString(7,String.valueOf(model.getValueAt(r,6)));ps.addBatch();
                } ps.executeBatch();
            }
    }

    public static synchronized List<OrdersPanel.Order> readOrders() {
        List<OrdersPanel.Order> out=new ArrayList<>();
        try(PreparedStatement ps=c().prepareStatement("SELECT id,customer,phone,additional_phone,address,order_date,payment,order_status,delivery_status,shipping,discount,notes,total,delivered_at FROM orders ORDER BY rowid");
            ResultSet rs=ps.executeQuery()){
            while(rs.next()){
                // Both status fields are read verbatim from SQLite. Do not
                // derive Delivered/Completed (or any other state) while
                // reconstructing an order. The UI may derive a display
                // state for return accounting, but persistence itself always
                // restores the exact stored values.
                OrdersPanel.Order o=OrdersPanel.Order.persistenceCreate(
                        rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7), rs.getString(8), rs.getString(9),
                        rs.getDouble(10), rs.getDouble(11), rs.getString(12));
                o.persistenceSetTotal(rs.getString(13)); long d=rs.getLong(14); o.persistenceSetDeliveredAt(rs.wasNull()?null:new Date(d));
                try(PreparedStatement pi=c().prepareStatement("SELECT product_name,product_id,sku,category,size,color,unit_price,cost_price,quantity FROM order_items WHERE order_id=? ORDER BY rowid")){
                    pi.setString(1,o.getId());try(ResultSet ri=pi.executeQuery()){while(ri.next())o.persistenceAddItem(OrdersPanel.OrderItem.persistenceCreate(ri.getString(1),ri.getString(2),ri.getString(3),ri.getString(4),ri.getString(5),ri.getString(6),ri.getDouble(7),ri.getDouble(8),ri.getInt(9)));}
                } out.add(o);
            }
        }catch(SQLException e){throw db(e);}
        return out;
    }

    public static synchronized void saveOrder(OrdersPanel.Order o) {
        try{
            c().setAutoCommit(false);
            saveOrderBody(o);
            c().commit();c().setAutoCommit(true);
        }catch(SQLException e){rollback();throw db(e);}
    }

    // NOTE: orders.customer_id is deliberately left NULL here (not set via
    // ps.setString) — see the comment on that column in DatabaseSchema.
    // The customer/phone columns below are the authoritative link.
    private static void saveOrderBody(OrdersPanel.Order o) throws SQLException {
            try(PreparedStatement ps=c().prepareStatement("""
                INSERT INTO orders(id,customer,phone,additional_phone,address,order_date,payment,order_status,delivery_status,shipping,discount,notes,total,delivered_at)
                VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON CONFLICT(id) DO UPDATE SET customer=excluded.customer,phone=excluded.phone,
                additional_phone=excluded.additional_phone,address=excluded.address,order_date=excluded.order_date,payment=excluded.payment,
                order_status=excluded.order_status,delivery_status=excluded.delivery_status,shipping=excluded.shipping,discount=excluded.discount,
                notes=excluded.notes,total=excluded.total,delivered_at=excluded.delivered_at""")){
                ps.setString(1,o.getId());ps.setString(2,o.getCustomer());ps.setString(3,o.getPhone());ps.setString(4,o.getAdditionalPhone());ps.setString(5,o.getAddress());
                ps.setString(6,o.getDate());ps.setString(7,o.getPayment());ps.setString(8,o.getOrderStatus());ps.setString(9,o.getDeliveryStatus());ps.setDouble(10,o.getShipping());
                ps.setDouble(11,o.getDiscount());ps.setString(12,o.getNotes());ps.setString(13,o.getTotal());if(o.getDeliveredAt()==null)ps.setNull(14,Types.INTEGER);else ps.setLong(14,o.getDeliveredAt().getTime());ps.executeUpdate();
            }
            try(PreparedStatement d=c().prepareStatement("DELETE FROM order_items WHERE order_id=?")){d.setString(1,o.getId());d.executeUpdate();}
            try(PreparedStatement pi=c().prepareStatement("INSERT INTO order_items(order_id,product_id,product_name,sku,category,size,color,unit_price,cost_price,quantity,line_total) VALUES(?,?,?,?,?,?,?,?,?,?,?)")){
                for(OrdersPanel.OrderItem i:o.getItems()){pi.setString(1,o.getId());pi.setString(2,i.getCode());pi.setString(3,i.getName());pi.setString(4,i.getSku());pi.setString(5,i.getCategory());pi.setString(6,i.getSize());pi.setString(7,i.getColor());pi.setDouble(8,i.getPrice());pi.setDouble(9,i.getCostPrice());pi.setInt(10,i.getQuantity());pi.setDouble(11,i.getPrice()*i.getQuantity());pi.addBatch();}pi.executeBatch();
            }
    }
    public static synchronized void deleteOrder(String id){exec("DELETE FROM orders WHERE id=?",id);}

    public static synchronized void loadReturns() {
        // Always rebuild the in-memory return state from SQLite. The return
        // map is derived from these records, so loading the records is the
        // single source of truth for both the Returns table and
        // processedReturnQuantities.
        ReturnsPanel.clearPersistenceData();
        try(PreparedStatement ps=c().prepareStatement(
                "SELECT id,order_id,customer,product,product_id,size,color,quantity,return_date,refund_amount,reason,disposition,loss " +
                "FROM returns ORDER BY rowid");
            ResultSet rs=ps.executeQuery()){
            while(rs.next()) {
                ReturnsPanel.addPersistedReturn(
                        rs.getString(1), rs.getString(2), rs.getString(3),
                        rs.getString(4), rs.getString(5), rs.getString(6),
                        rs.getString(7), rs.getInt(8), rs.getString(9),
                        rs.getDouble(10), rs.getString(11), rs.getString(12),
                        rs.getDouble(13));
            }
            // Rebuild return-derived order state after all return records
            // are present. This also repairs legacy databases where a return
            // was saved but the order's delivery_status was not updated.
            ReturnsPanel.reconcileLoadedOrderStatuses();
        }catch(SQLException e){throw db(e);}
    }
    public static synchronized void saveReturns() {
        try{c().setAutoCommit(false);
            saveReturnsBody();
            c().commit();c().setAutoCommit(true);
        }catch(SQLException e){rollback();throw db(e);}
    }

    private static void saveReturnsBody() throws SQLException {
            try(Statement d=c().createStatement()){d.executeUpdate("DELETE FROM returns");}
            try(PreparedStatement ps=c().prepareStatement("INSERT INTO returns(id,order_id,customer,product,product_id,size,color,quantity,return_date,refund_amount,reason,disposition,loss) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")){
                for(ReturnsPanel.ReturnSnapshot r:ReturnsPanel.getReturnSnapshots()){int i=1;ps.setString(i++,r.id());ps.setString(i++,r.orderId());ps.setString(i++,r.customer());ps.setString(i++,r.product());ps.setString(i++,r.productId());ps.setString(i++,r.size());ps.setString(i++,r.color());ps.setInt(i++,r.quantity());ps.setString(i++,r.date());ps.setDouble(i++,r.amount());ps.setString(i++,r.reason());ps.setString(i++,r.disposition());ps.setDouble(i++,r.loss());ps.addBatch();}ps.executeBatch();}
    }

    public static synchronized void loadExpenses() {
        ExpenseManager.clearPersistenceData();
        try(PreparedStatement ps=c().prepareStatement("SELECT id,expense_date,category,description,payment,amount,status FROM expenses ORDER BY rowid");
            ResultSet rs=ps.executeQuery()){
            while(rs.next()) ExpenseManager.addPersisted(rs.getString(1),new Date(rs.getLong(2)),rs.getString(3),rs.getString(4),rs.getString(5),rs.getDouble(6),rs.getString(7));
        }catch(SQLException e){throw db(e);}
    }
    public static synchronized void saveExpenses() {
        try{c().setAutoCommit(false);
            saveExpensesBody();
            c().commit();c().setAutoCommit(true);
        }catch(SQLException e){rollback();throw db(e);}
    }

    private static void saveExpensesBody() throws SQLException {
            try(Statement d=c().createStatement()){d.executeUpdate("DELETE FROM expenses");}
            try(PreparedStatement ps=c().prepareStatement("INSERT INTO expenses(id,expense_date,category,description,payment,amount,status) VALUES(?,?,?,?,?,?,?)")){
                for(ExpenseManager.Expense e:ExpenseManager.getExpenses()){ps.setString(1,e.getId());ps.setLong(2,e.getDate().getTime());ps.setString(3,e.getCategory());ps.setString(4,e.getDescription());ps.setString(5,e.getPayment());ps.setDouble(6,e.getAmount());ps.setString(7,e.getStatus());ps.addBatch();}ps.executeBatch();}
    }

    public static synchronized void loadSettings() {
        try(Statement st=c().createStatement();ResultSet rs=st.executeQuery("SELECT key,value FROM settings")){
            while(rs.next()){String k=rs.getString(1),v=rs.getString(2);applySetting(k,v);}
        }catch(SQLException e){throw db(e);}
    }
    private static void applySetting(String k,String v){
        try{
            switch(k){
                case "storeName"->SettingsManager.setStoreName(v);case "storeType"->SettingsManager.setStoreType(v);
                case "storePhone"->SettingsManager.setStorePhone(v);case "storeAddress"->SettingsManager.setStoreAddress(v);
                case "logoPath"->SettingsManager.setLogoPath(v);case "invoiceFooter"->SettingsManager.setInvoiceFooter(v);
                case "returnPolicy"->SettingsManager.setReturnPolicy(v);case "printer"->SettingsManager.setPrinter(v);
                case "paperSize"->SettingsManager.setPaperSize(v);case "autoPrint"->SettingsManager.setAutoPrint(Boolean.parseBoolean(v));
                case "primaryColor"->SettingsManager.setPrimaryColor(color(v));case "sidebarColor"->SettingsManager.setSidebarColor(color(v));
            }
        }catch(Exception ignored){}
    }
    public static synchronized void saveSettings() {
        Map<String,String> m=settingsSnapshot();
        try{c().setAutoCommit(false);
            saveSettingsBody(m);
            c().commit();c().setAutoCommit(true);
        }catch(SQLException e){rollback();throw db(e);}
    }

    private static void saveSettingsBody(Map<String,String> m) throws SQLException {
            try(Statement d=c().createStatement()){d.executeUpdate("DELETE FROM settings");}
            try(PreparedStatement ps=c().prepareStatement("INSERT INTO settings(key,value) VALUES(?,?)")){for(var e:m.entrySet()){ps.setString(1,e.getKey());ps.setString(2,e.getValue());ps.addBatch();}ps.executeBatch();}
    }
    private static Color color(String s){return new Color(Integer.parseInt(s),true);}


    public static synchronized boolean isInitialSeedPending() {
        try (PreparedStatement ps=c().prepareStatement("SELECT value FROM settings WHERE key='initial_seed_completed'");
             ResultSet rs=ps.executeQuery()) {
            return !rs.next() || !"true".equalsIgnoreCase(rs.getString(1));
        } catch(SQLException e){throw db(e);}
    }

    public static synchronized void markInitialSeedComplete() {
        try(PreparedStatement ps=c().prepareStatement("INSERT INTO settings(key,value,updated_at) VALUES('initial_seed_completed','true',?) ON CONFLICT(key) DO UPDATE SET value='true',updated_at=excluded.updated_at")){
            ps.setLong(1,System.currentTimeMillis());ps.executeUpdate();
        }catch(SQLException e){throw db(e);}
    }

    public static synchronized void markSeedCompleteIfDatabaseAlreadyHasData() {
        try(Statement st=c().createStatement();
            ResultSet rs=st.executeQuery("SELECT (SELECT COUNT(*) FROM products)+(SELECT COUNT(*) FROM orders)+(SELECT COUNT(*) FROM customers)+(SELECT COUNT(*) FROM expenses)+(SELECT COUNT(*) FROM returns)")) {
            if(rs.next() && rs.getInt(1)>0) markInitialSeedComplete();
        }catch(SQLException e){throw db(e);}
    }

    // ATOMIC SAVE: everything below runs on the one shared connection
    // inside a single transaction. Either every entity type commits
    // together, or none of them do — no more "warehouses+products
    // committed, then orders fail and leave the DB half-updated."
    // The individual saveXxx() methods above are untouched and still
    // work standalone (each still manages its own transaction), since
    // saveAll() calls their no-commit *Body() counterparts directly
    // instead of the public methods.
    public static synchronized void saveAll() {
        boolean priorAutoCommit = true;
        boolean autoCommitCaptured = false;
        try{
            priorAutoCommit = c().getAutoCommit();
            autoCommitCaptured = true;
            c().setAutoCommit(false);

            saveWarehousesBody();
            for(Product p:ProductManager.getInstance().getAllProducts()) saveProductBody(p);
            saveCustomersBody(CustomersPanel.getCustomerModel());
            for(OrdersPanel.Order o:OrdersPanel.getOrders()) saveOrderBody(o);
            saveReturnsBody();
            saveExpensesBody();
            saveSettingsBody(settingsSnapshot());

            c().commit();
        }catch(Exception e){
            System.err.println("[Persistence] saveAll failed, rolling back: "+e.getMessage());
            e.printStackTrace();
            rollback();
            if (e instanceof RuntimeException re) throw re;
            throw new IllegalStateException("SQLite persistence transaction failed", e);
        }finally{
            if(autoCommitCaptured){
                try{ c().setAutoCommit(priorAutoCommit); }catch(Exception ignored){}
            }
        }
    }

    // Builds the same settings snapshot saveSettings() persists, factored
    // out so saveAll() can pass it to saveSettingsBody() without touching
    // saveSettings()'s own transaction handling.
    private static Map<String,String> settingsSnapshot() {
        Map<String,String> m=new LinkedHashMap<>();m.put("storeName",SettingsManager.getStoreName());m.put("storeType",SettingsManager.getStoreType());
        m.put("storePhone",SettingsManager.getStorePhone());m.put("storeAddress",SettingsManager.getStoreAddress());m.put("logoPath",SettingsManager.getLogoPath());
        m.put("invoiceFooter",SettingsManager.getInvoiceFooter());m.put("returnPolicy",SettingsManager.getReturnPolicy());m.put("printer",SettingsManager.getPrinter());
        m.put("paperSize",SettingsManager.getPaperSize());m.put("autoPrint",String.valueOf(SettingsManager.isAutoPrint()));
        m.put("primaryColor",SettingsManager.getPrimaryColor().getRGB()+"");m.put("sidebarColor",SettingsManager.getSidebarColor().getRGB()+"");
        return m;
    }

    private static boolean isTableEmpty(String t)throws SQLException{try(Statement s=c().createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM "+t)){return !r.next()||r.getInt(1)==0;}}
    private static void exec(String sql,String arg){try(PreparedStatement ps=c().prepareStatement(sql)){ps.setString(1,arg);ps.executeUpdate();}catch(SQLException e){throw db(e);}}
    private static int intv(Object x){try{return Integer.parseInt(String.valueOf(x).replace(",",""));}catch(Exception e){return 0;}}
    private static double money(Object x){try{return Double.parseDouble(String.valueOf(x).replace("EGP","").replace(",","").trim());}catch(Exception e){return 0;}}

    // Mirrors CustomersPanel's money(double): reconstructing the customer
    // table's "total spent" cell from the REAL column with a fixed
    // "EGP %,.0f" (0 decimals) was silently rounding the stored figure
    // every time the app restarted, drifting it away from the precise
    // value actually persisted (e.g. after a fully-returned order left
    // 299.97 instead of an even 300 in the database).
    private static String formatMoney(double x){
        if(Math.abs(x-Math.round(x))<0.005) return String.format(Locale.US,"EGP %,.0f",(double) Math.round(x));
        return String.format(Locale.US,"EGP %,.2f",x);
    }
    // Robust rollback helper: always attempts the rollback AND always
    // attempts to restore autoCommit, even if the rollback itself throws
    // (e.g. connection already broken) — one failing step must never skip
    // the other. Rollback failures are logged, never silently discarded;
    // the SQLException that triggered the rollback is still rethrown by
    // every caller of this method right after it returns, so the
    // original error is never lost either way.
    private static void rollback(){
        try{
            c().rollback();
        }catch(Exception rollbackFailure){
            System.err.println("[Persistence] rollback() failed: "+rollbackFailure.getMessage());
            rollbackFailure.printStackTrace();
        }
        try{
            c().setAutoCommit(true);
        }catch(Exception autoCommitFailure){
            System.err.println("[Persistence] restoring autoCommit after rollback failed: "+autoCommitFailure.getMessage());
            autoCommitFailure.printStackTrace();
        }
    }
    private static RuntimeException db(SQLException e){return new IllegalStateException("SQLite persistence error: "+e.getMessage(),e);}

    // =========================================================
    // DATA SAFETY: exposed to the UI layer so panels never need
    // to import Clothes_system.db.DatabaseManager or java.sql.* directly.
    // =========================================================

    /** Location of the live SQLite file, for display in the UI. */
    public static java.io.File databaseFile() {
        return DatabaseManager.getDatabaseFile();
    }

    /** PRAGMA integrity_check on the live database; true only if it reports "ok". */
    public static synchronized boolean integrityCheckOk() {
        return DatabaseManager.integrityCheckOk();
    }

    /** True when the live SQLite connection has foreign-key enforcement enabled. */
    public static synchronized boolean foreignKeysEnabled() {
        return DatabaseManager.foreignKeysEnabled();
    }

    /** PRAGMA foreign_key_check on the live database; true only if there are zero violations. */
    public static synchronized boolean foreignKeyCheckOk() {
        return DatabaseManager.foreignKeyCheckOk();
    }

    /**
     * Flushes all in-memory state to SQLite, then writes a validated,
     * standalone backup snapshot to destFile. Throws on any failure
     * (nothing partial is left as a "successful" backup).
     */
    public static synchronized java.io.File backupDatabase(java.io.File destFile) throws Exception {
        saveAll();
        return DatabaseManager.backupTo(destFile);
    }

    /**
     * Restores the live database file from a previously-created, validated
     * backup file, after copying the current live file aside as a safety
     * net. Does not reload in-memory state or reopen the connection — the
     * caller must tell the user to restart the app and must call
     * Clothes_system.suppressSaveOnNextShutdown() first, so the normal
     * shutdown-time saveAll() does not overwrite the just-restored file
     * with the old, stale in-memory data.
     */
    public static synchronized java.io.File restoreDatabase(java.io.File backupFile) throws Exception {
        return DatabaseManager.restoreFrom(backupFile);
    }
}
