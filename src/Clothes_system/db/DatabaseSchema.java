package Clothes_system.db;

/** SQLite schema for the persistent application data. */
final class DatabaseSchema {
    private DatabaseSchema() {}

    static final String[] CREATE_TABLES = {
        """
        CREATE TABLE IF NOT EXISTS products (
            id TEXT PRIMARY KEY,
            sku TEXT,
            name TEXT NOT NULL,
            category TEXT,
            description TEXT,
            image_path TEXT,
            cost_price REAL NOT NULL DEFAULT 0,
            sale_price REAL NOT NULL DEFAULT 0,
            stock_quantity INTEGER NOT NULL DEFAULT 0,
            minimum_stock INTEGER NOT NULL DEFAULT 0,
            is_active INTEGER NOT NULL DEFAULT 1,
            created_at INTEGER NOT NULL DEFAULT 0,
            updated_at INTEGER NOT NULL DEFAULT 0
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS product_variants (
            variant_id TEXT PRIMARY KEY,
            product_id TEXT NOT NULL,
            color TEXT,
            size TEXT,
            stock_quantity INTEGER NOT NULL DEFAULT 0,
            FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS warehouses (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            location TEXT DEFAULT '',
            is_active INTEGER NOT NULL DEFAULT 1,
            created_at INTEGER NOT NULL DEFAULT 0
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS product_warehouse_stock (
            product_id TEXT NOT NULL,
            warehouse_id TEXT NOT NULL,
            quantity INTEGER NOT NULL DEFAULT 0,
            updated_at INTEGER NOT NULL DEFAULT 0,
            PRIMARY KEY(product_id,warehouse_id),
            FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE,
            FOREIGN KEY(warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS product_price_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id TEXT NOT NULL,
            old_cost_price REAL,
            new_cost_price REAL,
            old_sale_price REAL,
            new_sale_price REAL,
            changed_at INTEGER NOT NULL,
            changed_by TEXT,
            FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS product_inventory_history (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            product_id TEXT NOT NULL,
            variant_id TEXT,
            warehouse_id TEXT,
            change_qty INTEGER NOT NULL,
            resulting_stock INTEGER NOT NULL,
            reason TEXT,
            reference_type TEXT,
            reference_id TEXT,
            created_at INTEGER NOT NULL,
            FOREIGN KEY(product_id) REFERENCES products(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS customers (
            id TEXT PRIMARY KEY,
            name TEXT NOT NULL,
            phone TEXT NOT NULL UNIQUE,
            total_orders INTEGER NOT NULL DEFAULT 0,
            total_spent REAL NOT NULL DEFAULT 0,
            last_order TEXT,
            status TEXT NOT NULL DEFAULT 'Active',
            created_at INTEGER NOT NULL DEFAULT 0,
            updated_at INTEGER NOT NULL DEFAULT 0
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS orders (
            id TEXT PRIMARY KEY,
            -- customer_id is intentionally unpopulated (see
            -- PersistenceRepository.saveOrderBody()). The app links an
            -- order to a customer by the customer/phone snapshot columns
            -- below instead, so an order keeps the exact name/phone it was
            -- placed under even if that customer record is later renamed
            -- or merged. Activating this FK would require a real
            -- migration to back-fill existing rows, which is out of scope
            -- here; the column and its index are kept for schema/FK
            -- compatibility, not removed.
            customer_id TEXT,
            customer TEXT,
            phone TEXT,
            additional_phone TEXT,
            address TEXT,
            order_date TEXT NOT NULL,
            payment TEXT,
            order_status TEXT,
            delivery_status TEXT,
            shipping REAL NOT NULL DEFAULT 0,
            discount REAL NOT NULL DEFAULT 0,
            notes TEXT,
            total TEXT,
            delivered_at INTEGER,
            FOREIGN KEY(customer_id) REFERENCES customers(id) ON DELETE SET NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS order_items (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            order_id TEXT NOT NULL,
            product_id TEXT,
            product_name TEXT NOT NULL,
            sku TEXT,
            category TEXT,
            size TEXT,
            color TEXT,
            unit_price REAL NOT NULL DEFAULT 0,
            cost_price REAL NOT NULL DEFAULT 0,
            quantity INTEGER NOT NULL,
            line_total REAL NOT NULL DEFAULT 0,
            FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS returns (
            id TEXT PRIMARY KEY,
            order_id TEXT,
            customer TEXT,
            product TEXT,
            product_id TEXT,
            size TEXT,
            color TEXT,
            quantity INTEGER NOT NULL,
            return_date TEXT,
            refund_amount REAL NOT NULL DEFAULT 0,
            reason TEXT,
            disposition TEXT,
            loss REAL NOT NULL DEFAULT 0,
            FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE SET NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS expenses (
            id TEXT PRIMARY KEY,
            expense_date INTEGER NOT NULL,
            category TEXT,
            description TEXT,
            payment TEXT,
            amount REAL NOT NULL,
            status TEXT
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS settings (
            key TEXT PRIMARY KEY,
            value TEXT,
            updated_at INTEGER NOT NULL DEFAULT 0
        )
        """
    };

    static final String[] CREATE_INDEXES = {
        "CREATE INDEX IF NOT EXISTS idx_variants_product ON product_variants(product_id)",
        "CREATE INDEX IF NOT EXISTS idx_stock_product ON product_warehouse_stock(product_id)",
        "CREATE INDEX IF NOT EXISTS idx_stock_warehouse ON product_warehouse_stock(warehouse_id)",
        "CREATE INDEX IF NOT EXISTS idx_price_history_product ON product_price_history(product_id)",
        "CREATE INDEX IF NOT EXISTS idx_inventory_history_product ON product_inventory_history(product_id)",
        "CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id)",
        "CREATE INDEX IF NOT EXISTS idx_orders_date ON orders(order_date)",
        "CREATE INDEX IF NOT EXISTS idx_items_order ON order_items(order_id)",
        "CREATE INDEX IF NOT EXISTS idx_returns_order ON returns(order_id)",
        "CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(expense_date)"
    };
}
