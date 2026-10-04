public class CartItem {
    private final Product product;
    private int quantity;

    public CartItem(Product product, int quantity) {
        if (product == null || quantity < 1) throw new IllegalArgumentException("A product and positive quantity are required.");
        this.product = product;
        this.quantity = quantity;
    }

    public Product getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public double getGrossAmount() { return product.getUnitPrice() * quantity; }
    public void addQuantity(int additionalQuantity) {
        if (additionalQuantity < 1) throw new IllegalArgumentException("Quantity must be positive.");
        quantity += additionalQuantity;
    }
}
