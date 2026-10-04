import java.time.LocalDate;

/** Shared product data. Stock changes are controlled through this class. */
public abstract class Product {
    private final String code;
    private final String name;
    private final double unitPrice;
    private int stock;

    protected Product(String code, String name, double unitPrice, int stock) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) {
            throw new IllegalArgumentException("Product code and name are required.");
        }
        if (unitPrice < 0 || stock < 0) {
            throw new IllegalArgumentException("Price and stock cannot be negative.");
        }
        this.code = code;
        this.name = name;
        this.unitPrice = unitPrice;
        this.stock = stock;
    }

    public String getCode() { return code; }
    public String getName() { return name; }
    public double getUnitPrice() { return unitPrice; }
    public int getStock() { return stock; }
    public abstract String getCategory();

    /** Returns null for products that do not have an expiry date. */
    public LocalDate getExpiryDate() { return null; }

    public boolean isExpired(LocalDate today) {
        return getExpiryDate() != null && getExpiryDate().isBefore(today);
    }

    public boolean reduceStock(int quantity) {
        if (quantity < 1 || quantity > stock) return false;
        stock -= quantity;
        return true;
    }
}
