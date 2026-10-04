public class BillLine {
    private final String productName;
    private final int quantity;
    private final double unitPrice;
    private final double discount;

    public BillLine(String productName, int quantity, double unitPrice, double discount) {
        this.productName = productName;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discount = discount;
    }

    public String getProductName() { return productName; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getGrossAmount() { return unitPrice * quantity; }
    public double getDiscount() { return discount; }
    public double getNetAmount() { return getGrossAmount() - discount; }
}
