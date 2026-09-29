package Clothes_system;

import Clothes_system.db.PersistenceRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProductManager {

    // =========================================================
    // SINGLE INSTANCE
    // =========================================================

    private static final ProductManager INSTANCE =
            new ProductManager();

    public static ProductManager getInstance() {
        return INSTANCE;
    }

    // =========================================================
    // PRODUCTS
    // =========================================================

    private final List<Product> products;

    private ProductManager() {

        products = new ArrayList<>();

        List<Product> persisted = PersistenceRepository.readProducts();
        if (!persisted.isEmpty()) {
            products.addAll(persisted);
        } else if (PersistenceRepository.isInitialSeedPending()) {
            loadInitialProducts();
            for (Product p : products) PersistenceRepository.saveProduct(p);
        }
    }

    // =========================================================
    // ADD
    // =========================================================

    public synchronized boolean addProduct(
            Product product
    ) {

        if (product == null) {
            return false;
        }

        if (findById(product.getId()) != null) {
            return false;
        }

        if (
                !product.getSku().isEmpty()
                &&
                findBySku(product.getSku()) != null
        ) {
            return false;
        }

        products.add(product);
        PersistenceRepository.saveProduct(product);

        return true;
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public synchronized boolean updateProduct(
            Product product
    ) {

        if (product == null) {
            return false;
        }

        Product existing =
                findById(product.getId());

        if (existing == null) {
            return false;
        }

        // An edit must not be allowed to take an SKU that already
        // belongs to a different product. addProduct() enforces this
        // on creation; updateProduct() previously did not.
        if (!isSkuAvailable(product.getSku(), existing.getId())) {
            return false;
        }

        existing.setName(product.getName());
        existing.setSku(product.getSku());
        existing.setCategory(product.getCategory());
        existing.setDescription(product.getDescription());
        existing.setImagePath(product.getImagePath());
        existing.setMinimumStock(
                product.getMinimumStock()
        );

        existing.updatePrice(
                product.getSellingPrice(),
                product.getCostPrice(),
                "Product edited"
        );
        PersistenceRepository.saveProduct(existing);

        return true;
    }

    // =========================================================
    // FIND BY ID
    // =========================================================

    public synchronized Product findById(
            String id
    ) {

        if (id == null) {
            return null;
        }

        for (Product product : products) {

            if (
                    product.getId()
                            .equalsIgnoreCase(
                                    id.trim()
                            )
            ) {
                return product;
            }
        }

        return null;
    }

    // =========================================================
    // FIND BY SKU
    // =========================================================

    public synchronized Product findBySku(
            String sku
    ) {

        if (sku == null || sku.trim().isEmpty()) {
            return null;
        }

        for (Product product : products) {

            if (
                    product.getSku()
                            .equalsIgnoreCase(
                                    sku.trim()
                            )
            ) {
                return product;
            }
        }

        return null;
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public synchronized List<Product> search(
            String text
    ) {

        if (text == null) {
            return getAllProducts();
        }

        String query =
                text.trim().toLowerCase();

        if (query.isEmpty()) {
            return getAllProducts();
        }

        List<Product> result =
                new ArrayList<>();

        for (Product product : products) {

            if (
                    product.getName()
                            .toLowerCase()
                            .contains(query)
                    ||
                    product.getSku()
                            .toLowerCase()
                            .contains(query)
                    ||
                    product.getCategory()
                            .toLowerCase()
                            .contains(query)
            ) {

                result.add(product);
            }
        }

        return result;
    }

    // =========================================================
    // ALL PRODUCTS
    // =========================================================

    public synchronized List<Product> getAllProducts() {

        return Collections.unmodifiableList(
                new ArrayList<>(products)
        );
    }

    // =========================================================
    // ACTIVE PRODUCTS
    // =========================================================

    public synchronized List<Product> getActiveProducts() {

        List<Product> result =
                new ArrayList<>();

        for (Product product : products) {

            if (product.isActive()) {
                result.add(product);
            }
        }

        return result;
    }

    // =========================================================
    // DEACTIVATE
    // =========================================================

    public synchronized boolean deactivateProduct(
            String id
    ) {

        Product product =
                findById(id);

        if (product == null) {
            return false;
        }

        product.deactivate();
        PersistenceRepository.saveProduct(product);

        return true;
    }

    // =========================================================
    // ACTIVATE
    // =========================================================

    public synchronized boolean activateProduct(
            String id
    ) {

        Product product =
                findById(id);

        if (product == null) {
            return false;
        }

        product.activate();
        PersistenceRepository.saveProduct(product);

        return true;
    }

    // =========================================================
    // REMOVE
    // =========================================================
    // Physical deletion is intentionally separated.
    // We will normally use deactivateProduct().
    // =========================================================

    public synchronized boolean removeProduct(
            String id
    ) {

        Product product =
                findById(id);

        if (product == null) {
            return false;
        }

        boolean removed = products.remove(product);
        if (removed) PersistenceRepository.deleteProduct(product.getId());
        return removed;
    }

    // =========================================================
    // STOCK
    // =========================================================

    public synchronized boolean addStock(
            String productId,
            int quantity,
            String reason
    ) {

        Product product =
                findById(productId);

        if (product == null) {
            return false;
        }

        product.addStock(
                quantity,
                reason
        );
        PersistenceRepository.saveProduct(product);

        return true;
    }

    public synchronized boolean removeStock(
            String productId,
            int quantity,
            String reason
    ) {

        Product product =
                findById(productId);

        if (product == null) {
            return false;
        }

        boolean changed = product.removeStock(
                quantity,
                reason
        );
        if (changed) PersistenceRepository.saveProduct(product);
        return changed;
    }

    // =========================================================
    // PRICE
    // =========================================================

    public synchronized boolean updatePrice(
            String productId,
            double sellingPrice,
            double costPrice,
            String reason
    ) {

        Product product =
                findById(productId);

        if (product == null) {
            return false;
        }

        product.updatePrice(
                sellingPrice,
                costPrice,
                reason
        );
        PersistenceRepository.saveProduct(product);

        return true;
    }

    // =========================================================
    // ID GENERATION
    // =========================================================

    public synchronized String generateProductId() {
        int max = 0;
        for (Product product : products) {
            if (product == null || product.getId() == null) continue;
            String id = product.getId().trim().toUpperCase();
            if (!id.startsWith("PRD-")) continue;
            try { max = Math.max(max, Integer.parseInt(id.substring(4))); }
            catch (NumberFormatException ignored) { }
        }
        return String.format("PRD-%03d", max + 1);
    }

    // =========================================================
    // SKU CHECK
    // =========================================================

    public synchronized boolean isSkuAvailable(
            String sku,
            String currentProductId
    ) {

        if (
                sku == null
                ||
                sku.trim().isEmpty()
        ) {
            return true;
        }

        Product product =
                findBySku(sku);

        if (product == null) {
            return true;
        }

        return product.getId().equals(
                currentProductId
        );
    }

    // =========================================================
    // LOW STOCK
    // =========================================================

    public synchronized List<Product> getLowStockProducts() {

        List<Product> result =
                new ArrayList<>();

        for (Product product : products) {

            if (
                    product.isActive()
                    &&
                    product.getStockQuantity()
                            <= product.getMinimumStock()
            ) {

                result.add(product);
            }
        }

        return result;
    }

    // =========================================================
    // OUT OF STOCK
    // =========================================================

    public synchronized List<Product> getOutOfStockProducts() {

        List<Product> result =
                new ArrayList<>();

        for (Product product : products) {

            if (
                    product.isActive()
                    &&
                    product.getStockQuantity() == 0
            ) {

                result.add(product);
            }
        }

        return result;
    }

    // =========================================================
    // INITIAL PRODUCTS
    // =========================================================
    // Temporary data only.
    // Later this will come from persistent storage/database.
    // =========================================================

    private void loadInitialProducts() {

        addProduct(
                new Product(
                        "PRD-001",
                        "HD-001",
                        "Classic Hoodie",
                        "Hoodies",
                        "",
                        "",
                        1250,
                        750,
                        3,
                        5
                )
        );

        addProduct(
                new Product(
                        "PRD-002",
                        "JN-001",
                        "Slim Jeans",
                        "Jeans",
                        "",
                        "",
                        950,
                        550,
                        2,
                        5
                )
        );

        addProduct(
                new Product(
                        "PRD-003",
                        "TS-001",
                        "Basic T-Shirt",
                        "T-Shirts",
                        "",
                        "",
                        450,
                        220,
                        18,
                        5
                )
        );

        addProduct(
                new Product(
                        "PRD-004",
                        "SH-001",
                        "Oversized Shirt",
                        "Shirts",
                        "",
                        "",
                        780,
                        400,
                        5,
                        5
                )
        );

        addProduct(
                new Product(
                        "PRD-005",
                        "CP-001",
                        "Cargo Pants",
                        "Pants",
                        "",
                        "",
                        1100,
                        650,
                        12,
                        5
                )
        );

        addProduct(
                new Product(
                        "PRD-006",
                        "JK-001",
                        "Winter Jacket",
                        "Jackets",
                        "",
                        "",
                        1850,
                        1100,
                        0,
                        5
                )
        );
    }
}