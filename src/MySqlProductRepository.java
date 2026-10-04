import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Loads products and persists completed sales through MySQL JDBC. */
public class MySqlProductRepository implements ProductRepository {
    private final DatabaseConnection database;

    public MySqlProductRepository(DatabaseConnection database) {
        this.database = database;
    }

    @Override
    public List<Product> findAll() throws SQLException {
        String sql = "SELECT product_code, product_name, category, unit_price, stock, expiry_date "
                + "FROM products ORDER BY product_name";
        List<Product> products = new ArrayList<>();
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            while (results.next()) {
                String code = results.getString("product_code");
                String name = results.getString("product_name");
                double price = results.getBigDecimal("unit_price").doubleValue();
                int stock = results.getInt("stock");
                String category = results.getString("category");
                Date expiryDate = results.getDate("expiry_date");

                if ("PERISHABLE".equals(category)) {
                    products.add(new PerishableProduct(code, name, price, stock,
                            expiryDate == null ? null : expiryDate.toLocalDate()));
                } else {
                    products.add(new RegularProduct(code, name, price, stock));
                }
            }
        }
        return products;
    }

    @Override
    public void createProduct(Product product) throws SQLException {
        String sql = "INSERT INTO products "
                + "(product_code, product_name, category, unit_price, stock, expiry_date) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getCode().trim());
            statement.setString(2, product.getName().trim());
            statement.setString(3, product.getCategory().toUpperCase(Locale.ROOT));
            statement.setBigDecimal(4, currency(product.getUnitPrice()));
            statement.setInt(5, product.getStock());
            setExpiry(statement, 6, product);
            statement.executeUpdate();
        } catch (SQLException exception) {
            if ("23000".equals(exception.getSQLState())) {
                throw new SQLException("That product code already exists. Choose a unique code.", exception);
            }
            throw exception;
        }
    }

    @Override
    public void updateProduct(Product product) throws SQLException {
        String sql = "UPDATE products SET product_name = ?, category = ?, unit_price = ?, expiry_date = ? "
                + "WHERE product_code = ?";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, product.getName().trim());
            statement.setString(2, product.getCategory().toUpperCase(Locale.ROOT));
            statement.setBigDecimal(3, currency(product.getUnitPrice()));
            setExpiry(statement, 4, product);
            statement.setString(5, product.getCode());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The product was not found. Refresh the inventory and try again.");
            }
        }
    }

    @Override
    public void deleteProduct(String productCode) throws SQLException {
        String sql = "DELETE FROM products WHERE product_code = ?";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, productCode);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("The product was not found. Refresh the inventory and try again.");
            }
        }
    }

    @Override
    public void restockProduct(String productCode, int quantity) throws SQLException {
        String sql = "UPDATE products SET stock = stock + ? "
                + "WHERE product_code = ? AND stock <= 2147483647 - ?";
        try (Connection connection = database.open();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, quantity);
            statement.setString(2, productCode);
            statement.setInt(3, quantity);
            if (statement.executeUpdate() != 1) {
                throw new SQLException("Restock failed. The product may be missing or its stock limit was reached.");
            }
        }
    }

    private void setExpiry(PreparedStatement statement, int parameter, Product product) throws SQLException {
        if (product.getExpiryDate() == null) statement.setNull(parameter, java.sql.Types.DATE);
        else statement.setDate(parameter, Date.valueOf(product.getExpiryDate()));
    }

    @Override
    public void saveSale(Bill bill) throws SQLException {
        String updateStockSql = "UPDATE products SET stock = stock - ? "
                + "WHERE product_code = ? AND stock >= ?";
        String insertBillSql = "INSERT INTO bills "
                + "(bill_number, customer_name, created_at, subtotal, discount_total, total) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        String insertLineSql = "INSERT INTO bill_items "
                + "(bill_id, product_code, product_name, quantity, unit_price, discount_amount, line_total) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = database.open()) {
            connection.setAutoCommit(false);
            try {
                for (BillLine line : bill.getLines()) {
                    try (PreparedStatement update = connection.prepareStatement(updateStockSql)) {
                        update.setInt(1, line.getQuantity());
                        update.setString(2, line.getProductCode());
                        update.setInt(3, line.getQuantity());
                        if (update.executeUpdate() != 1) {
                            throw new SQLException("Not enough stock remains for " + line.getProductName()
                                    + ". The sale was rolled back. Close and reopen the app to reload the catalog.");
                        }
                    }
                }

                long billId;
                try (PreparedStatement insertBill = connection.prepareStatement(
                        insertBillSql, Statement.RETURN_GENERATED_KEYS)) {
                    insertBill.setString(1, bill.getBillNumber());
                    insertBill.setString(2, bill.getCustomerName());
                    insertBill.setTimestamp(3, Timestamp.valueOf(bill.getCreatedAt()));
                    insertBill.setBigDecimal(4, currency(bill.getSubtotal()));
                    insertBill.setBigDecimal(5, currency(bill.getDiscountTotal()));
                    insertBill.setBigDecimal(6, currency(bill.getTotal()));
                    insertBill.executeUpdate();
                    try (ResultSet keys = insertBill.getGeneratedKeys()) {
                        if (!keys.next()) throw new SQLException("MySQL did not return the new bill ID.");
                        billId = keys.getLong(1);
                    }
                }

                try (PreparedStatement insertLine = connection.prepareStatement(insertLineSql)) {
                    for (BillLine line : bill.getLines()) {
                        insertLine.setLong(1, billId);
                        insertLine.setString(2, line.getProductCode());
                        insertLine.setString(3, line.getProductName());
                        insertLine.setInt(4, line.getQuantity());
                        insertLine.setBigDecimal(5, currency(line.getUnitPrice()));
                        insertLine.setBigDecimal(6, currency(line.getDiscount()));
                        insertLine.setBigDecimal(7, currency(line.getNetAmount()));
                        insertLine.addBatch();
                    }
                    insertLine.executeBatch();
                }

                connection.commit();
            } catch (SQLException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        }
    }

    private BigDecimal currency(double amount) {
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP);
    }
}
