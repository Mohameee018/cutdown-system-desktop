package Clothes_system;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Product {

    // =========================================================
    // BASIC DATA
    // =========================================================

    private final String id;
    private String sku;

    private String name;
    private String category;
    private String description;

    // =========================================================
    // IMAGE
    // =========================================================

    private String imagePath;

    // =========================================================
    // PRICING
    // =========================================================

    private double sellingPrice;
    private double costPrice;

    // =========================================================
    // STOCK
    // =========================================================

    private int stockQuantity;
    private int minimumStock;

    // =========================================================
    // STATUS
    // =========================================================

    private boolean active;

    // =========================================================
    // VARIANTS
    // =========================================================

    private final List<ProductVariant> variants;

    // =========================================================
    // HISTORY
    // =========================================================

    private final List<PriceHistoryEntry> priceHistory;
    private final List<InventoryHistoryEntry> inventoryHistory;

    // Multi-warehouse stock breakdown. Kept automatically in sync
    // with stockQuantity (the total) so every existing screen that
    // reads getStockQuantity() keeps working exactly as before,
    // while the Inventory screen can additionally show/manage
    // where that stock physically sits.
    private final java.util.Map<String, Integer> warehouseStock;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public Product(
            String id,
            String sku,
            String name,
            String category,
            String description,
            String imagePath,
            double sellingPrice,
            double costPrice,
            int stockQuantity,
            int minimumStock
    ) {

        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID is required.");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Product name is required.");
        }

        if (sellingPrice < 0) {
            throw new IllegalArgumentException(
                    "Selling price cannot be negative."
            );
        }

        if (costPrice < 0) {
            throw new IllegalArgumentException(
                    "Cost price cannot be negative."
            );
        }

        if (stockQuantity < 0) {
            throw new IllegalArgumentException(
                    "Stock quantity cannot be negative."
            );
        }

        if (minimumStock < 0) {
            throw new IllegalArgumentException(
                    "Minimum stock cannot be negative."
            );
        }

        this.id = id;
        this.sku = sku == null ? "" : sku.trim();

        this.name = name.trim();
        this.category =
                category == null ? "" : category.trim();

        this.description =
                description == null ? "" : description.trim();

        this.imagePath =
                imagePath == null ? "" : imagePath.trim();

        this.sellingPrice = sellingPrice;
        this.costPrice = costPrice;

        this.stockQuantity = stockQuantity;
        this.minimumStock = minimumStock;

        this.active = true;

        this.variants = new ArrayList<>();

        this.priceHistory = new ArrayList<>();
        this.inventoryHistory = new ArrayList<>();

        this.warehouseStock = new java.util.LinkedHashMap<>();
        // New products start fully allocated to the default warehouse;
        // stock can later be moved/split across warehouses from the
        // Inventory screen.
        this.warehouseStock.put(InventoryPanel.WarehouseManager.DEFAULT_WAREHOUSE_ID, stockQuantity);

        // Initial price history
        this.priceHistory.add(
                new PriceHistoryEntry(
                        sellingPrice,
                        costPrice,
                        new Date(),
                        "Initial product price"
                )
        );

        // Initial inventory history
        if (stockQuantity > 0) {

            this.inventoryHistory.add(
                    new InventoryHistoryEntry(
                            stockQuantity,
                            stockQuantity,
                            "INITIAL_STOCK",
                            "Initial product stock",
                            new Date()
                    )
            );
        }
    }

    // =========================================================
    // GETTERS
    // =========================================================

    public String getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public double getSellingPrice() {
        return sellingPrice;
    }

    public double getCostPrice() {
        return costPrice;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public int getMinimumStock() {
        return minimumStock;
    }

    public boolean isActive() {
        return active;
    }

    public List<ProductVariant> getVariants() {
        return variants;
    }

    public List<PriceHistoryEntry> getPriceHistory() {
        return priceHistory;
    }

    public List<InventoryHistoryEntry> getInventoryHistory() {
        return inventoryHistory;
    }

    // =========================================================
    // SETTERS / EDIT
    // =========================================================

    public void setSku(String sku) {

        this.sku =
                sku == null ? "" : sku.trim();
    }

    public void setName(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Product name is required."
            );
        }

        this.name = name.trim();
    }

    public void setCategory(String category) {

        this.category =
                category == null ? "" : category.trim();
    }

    public void setDescription(String description) {

        this.description =
                description == null
                        ? ""
                        : description.trim();
    }

    public void setImagePath(String imagePath) {

        this.imagePath =
                imagePath == null
                        ? ""
                        : imagePath.trim();
    }

    public void setMinimumStock(int minimumStock) {

        if (minimumStock < 0) {
            throw new IllegalArgumentException(
                    "Minimum stock cannot be negative."
            );
        }

        this.minimumStock = minimumStock;
    }

    // =========================================================
    // PRICE UPDATE
    // =========================================================

    public void updatePrice(
            double newSellingPrice,
            double newCostPrice,
            String reason
    ) {

        if (newSellingPrice < 0) {
            throw new IllegalArgumentException(
                    "Selling price cannot be negative."
            );
        }

        if (newCostPrice < 0) {
            throw new IllegalArgumentException(
                    "Cost price cannot be negative."
            );
        }

        boolean changed =
                Double.compare(
                        sellingPrice,
                        newSellingPrice
                ) != 0
                ||
                Double.compare(
                        costPrice,
                        newCostPrice
                ) != 0;

        if (!changed) {
            return;
        }

        this.sellingPrice = newSellingPrice;
        this.costPrice = newCostPrice;

        priceHistory.add(
                new PriceHistoryEntry(
                        newSellingPrice,
                        newCostPrice,
                        new Date(),
                        reason == null
                                ? "Price updated"
                                : reason
                )
        );
    }

    // =========================================================
    // MULTI-WAREHOUSE STOCK
    // =========================================================

    public java.util.Map<String, Integer> getWarehouseBreakdown() {
        return java.util.Collections.unmodifiableMap(warehouseStock);
    }

    public int getWarehouseQuantity(String warehouseId) {
        return warehouseStock.getOrDefault(warehouseId, 0);
    }

    /** Restores persisted warehouse allocation without creating a new
     * inventory transaction or changing the already persisted total. */
    public void loadWarehouseStock(java.util.Map<String, Integer> persisted) {
        warehouseStock.clear();
        if (persisted != null) {
            for (java.util.Map.Entry<String,Integer> e : persisted.entrySet()) {
                if (e.getKey() != null && e.getValue() != null && e.getValue() >= 0)
                    warehouseStock.put(e.getKey(), e.getValue());
            }
        }
        if (warehouseStock.isEmpty()) {
            warehouseStock.put(InventoryPanel.WarehouseManager.DEFAULT_WAREHOUSE_ID, stockQuantity);
        }
    }

    /**
     * Sets the absolute quantity of this product held in one warehouse
     * and keeps the overall stockQuantity (used everywhere else in the
     * system) in sync automatically.
     */
    public void setWarehouseQuantity(
            String warehouseId,
            int quantity,
            String reason
    ) {

        if (warehouseId == null || warehouseId.trim().isEmpty()) {
            throw new IllegalArgumentException("Warehouse is required.");
        }

        if (quantity < 0) {
            throw new IllegalArgumentException("Stock cannot be negative.");
        }

        int old = warehouseStock.getOrDefault(warehouseId, 0);
        int diff = quantity - old;

        if (diff == 0) {
            return;
        }

        warehouseStock.put(warehouseId, quantity);
        stockQuantity += diff;

        inventoryHistory.add(
                new InventoryHistoryEntry(
                        diff,
                        stockQuantity,
                        "WAREHOUSE_ADJUST",
                        reason == null
                                ? "Warehouse stock adjusted"
                                : reason,
                        new Date()
                )
        );
    }

    /**
     * Removes a warehouse from this product's breakdown (e.g. the
     * warehouse itself was deleted), folding whatever stock it held
     * back into the default warehouse so the total never changes.
     */
    void mergeWarehouseIntoDefault(String warehouseId) {

        if (warehouseId == null
                || warehouseId.equals(InventoryPanel.WarehouseManager.DEFAULT_WAREHOUSE_ID)) {
            return;
        }

        Integer removed = warehouseStock.remove(warehouseId);
        int qty = removed == null ? 0 : removed;

        if (qty > 0) {
            String key = InventoryPanel.WarehouseManager.DEFAULT_WAREHOUSE_ID;
            warehouseStock.put(key, warehouseStock.getOrDefault(key, 0) + qty);
        }
    }

    private void addToDefaultWarehouse(int quantity) {

        String key = InventoryPanel.WarehouseManager.DEFAULT_WAREHOUSE_ID;
        int current = warehouseStock.getOrDefault(key, 0);

        warehouseStock.put(key, current + quantity);
    }

    // NOTE: despite what a "proportional" name might suggest, this drains
    // the warehouse holding the most stock first (largest-first / greedy),
    // not proportionally across all warehouses. This is the actual,
    // intentional behavior (kept as-is here) — the method is named to
    // match it rather than mislead callers into expecting a proportional
    // split.
    private void removeFromWarehousesLargestFirst(int quantityToRemove) {

        int remaining = quantityToRemove;

        java.util.List<String> keys =
                new java.util.ArrayList<>(warehouseStock.keySet());

        keys.sort((a, b) ->
                warehouseStock.getOrDefault(b, 0) - warehouseStock.getOrDefault(a, 0)
        );

        for (String key : keys) {

            if (remaining <= 0) {
                break;
            }

            int available = warehouseStock.getOrDefault(key, 0);
            int take = Math.min(available, remaining);

            warehouseStock.put(key, available - take);
            remaining -= take;
        }

        if (remaining > 0) {
            // Legacy/inconsistent data: force the default warehouse
            // to absorb whatever is left so totals stay correct.
            String key = InventoryPanel.WarehouseManager.DEFAULT_WAREHOUSE_ID;
            int mainQty = warehouseStock.getOrDefault(key, 0);
            warehouseStock.put(key, Math.max(0, mainQty - remaining));
        }
    }

    // =========================================================
    // STOCK
    // =========================================================

    public void addStock(
            int quantity,
            String reason
    ) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }

        int oldStock = stockQuantity;

        stockQuantity += quantity;
        addToDefaultWarehouse(quantity);

        inventoryHistory.add(
                new InventoryHistoryEntry(
                        quantity,
                        stockQuantity,
                        "ADD",
                        reason == null
                                ? "Stock added"
                                : reason,
                        new Date()
                )
        );
    }

    public boolean removeStock(
            int quantity,
            String reason
    ) {

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero."
            );
        }

        if (quantity > stockQuantity) {
            return false;
        }

        stockQuantity -= quantity;
        removeFromWarehousesLargestFirst(quantity);

        inventoryHistory.add(
                new InventoryHistoryEntry(
                        -quantity,
                        stockQuantity,
                        "REMOVE",
                        reason == null
                                ? "Stock removed"
                                : reason,
                        new Date()
                )
        );

        return true;
    }

    public void setStock(
            int newStock,
            String reason
    ) {

        if (newStock < 0) {
            throw new IllegalArgumentException(
                    "Stock cannot be negative."
            );
        }

        int difference =
                newStock - stockQuantity;

        if (difference == 0) {
            return;
        }

        stockQuantity = newStock;

        if (difference > 0) {
            addToDefaultWarehouse(difference);
        } else {
            removeFromWarehousesLargestFirst(-difference);
        }

        inventoryHistory.add(
                new InventoryHistoryEntry(
                        difference,
                        stockQuantity,
                        "ADJUSTMENT",
                        reason == null
                                ? "Stock adjusted"
                                : reason,
                        new Date()
                )
        );
    }

    // =========================================================
    // STATUS
    // =========================================================

    public String getStockStatus() {

        if (stockQuantity <= 0) {
            return "Out of Stock";
        }

        if (stockQuantity <= minimumStock) {
            return "Low Stock";
        }

        return "In Stock";
    }

    // =========================================================
    // ACTIVATE / DEACTIVATE
    // =========================================================

    public void deactivate() {
        active = false;
    }

    public void activate() {
        active = true;
    }

    // =========================================================
    // VARIANTS
    // =========================================================

    public void addVariant(ProductVariant variant) {

        if (variant == null) {
            return;
        }

        variants.add(variant);
    }

    public void removeVariant(ProductVariant variant) {

        variants.remove(variant);
    }

    // =========================================================
    // PRICE HISTORY CLASS
    // =========================================================

    public static class PriceHistoryEntry {

        private final double sellingPrice;
        private final double costPrice;
        private final Date date;
        private final String reason;

        public PriceHistoryEntry(
                double sellingPrice,
                double costPrice,
                Date date,
                String reason
        ) {

            this.sellingPrice = sellingPrice;
            this.costPrice = costPrice;
            this.date = date;
            this.reason = reason;
        }

        public double getSellingPrice() {
            return sellingPrice;
        }

        public double getCostPrice() {
            return costPrice;
        }

        public Date getDate() {
            return date;
        }

        public String getReason() {
            return reason;
        }
    }

    // =========================================================
    // INVENTORY HISTORY CLASS
    // =========================================================

    public static class InventoryHistoryEntry {

        private final int quantityChange;
        private final int resultingStock;
        private final String type;
        private final String reason;
        private final Date date;

        public InventoryHistoryEntry(
                int quantityChange,
                int resultingStock,
                String type,
                String reason,
                Date date
        ) {

            this.quantityChange = quantityChange;
            this.resultingStock = resultingStock;
            this.type = type;
            this.reason = reason;
            this.date = date;
        }

        public int getQuantityChange() {
            return quantityChange;
        }

        public int getResultingStock() {
            return resultingStock;
        }

        public String getType() {
            return type;
        }

        public String getReason() {
            return reason;
        }

        public Date getDate() {
            return date;
        }
    }

    // =========================================================
    // PRODUCT VARIANT
    // =========================================================

    public static class ProductVariant {

        private final String id;

        private String color;
        private String size;

        private int stockQuantity;

        public ProductVariant(
                String id,
                String color,
                String size,
                int stockQuantity
        ) {

            if (id == null || id.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Variant ID is required."
                );
            }

            if (stockQuantity < 0) {
                throw new IllegalArgumentException(
                        "Variant stock cannot be negative."
                );
            }

            this.id = id;
            this.color =
                    color == null ? "" : color.trim();

            this.size =
                    size == null ? "" : size.trim();

            this.stockQuantity = stockQuantity;
        }

        public String getId() {
            return id;
        }

        public String getColor() {
            return color;
        }

        public String getSize() {
            return size;
        }

        public int getStockQuantity() {
            return stockQuantity;
        }

        public void setColor(String color) {

            this.color =
                    color == null
                            ? ""
                            : color.trim();
        }

        public void setSize(String size) {

            this.size =
                    size == null
                            ? ""
                            : size.trim();
        }

        public void setStockQuantity(int stockQuantity) {

            if (stockQuantity < 0) {
                throw new IllegalArgumentException(
                        "Variant stock cannot be negative."
                );
            }

            this.stockQuantity = stockQuantity;
        }
    }
}