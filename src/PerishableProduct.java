import java.time.LocalDate;

public class PerishableProduct extends Product {
    private final LocalDate expiryDate;

    public PerishableProduct(String code, String name, double unitPrice, int stock, LocalDate expiryDate) {
        super(code, name, unitPrice, stock);
        this.expiryDate = expiryDate;
    }

    @Override
    public String getCategory() { return "Perishable"; }

    @Override
    public LocalDate getExpiryDate() { return expiryDate; }
}
