import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/** Coordinates stock, cart operations, discount rules, and checkout. */
public class BillingService {
    private final List<Product> products;
    private final Map<String, CartItem> cart = new LinkedHashMap<>();
    private final List<DiscountPolicy> discountPolicies = List.of(
            new ExpiryDiscountPolicy(), new QuantityDiscountPolicy());
    private final AtomicInteger billSequence = new AtomicInteger(1);

    public BillingService(List<Product> products) {
        this.products = new ArrayList<>(products);
    }

    public List<Product> getProducts() { return List.copyOf(products); }
    public List<CartItem> getCartItems() { return new ArrayList<>(cart.values()); }
    public List<DiscountPolicy> getDiscountPolicies() { return discountPolicies; }

    public void addToCart(Product product, int quantity) {
        if (product == null || !products.contains(product)) throw new IllegalArgumentException("Choose a product from the catalog.");
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least one.");
        if (product.isExpired(LocalDate.now())) throw new IllegalArgumentException("Expired products cannot be sold.");
        CartItem existing = cart.get(product.getCode());
        int alreadyInCart = existing == null ? 0 : existing.getQuantity();
        if (alreadyInCart + quantity > product.getStock()) {
            throw new IllegalArgumentException("Only " + product.getStock() + " unit(s) are currently in stock.");
        }
        if (existing == null) cart.put(product.getCode(), new CartItem(product, quantity));
        else existing.addQuantity(quantity);
    }

    public void removeFromCart(String productCode) { cart.remove(productCode); }
    public void clearCart() { cart.clear(); }

    public double getDiscount(CartItem item) {
        double amount = 0;
        for (DiscountPolicy policy : discountPolicies) {
            amount += policy.calculateDiscount(item, LocalDate.now());
        }
        return Math.min(item.getGrossAmount(), amount);
    }

    public double getSubtotal() { return cart.values().stream().mapToDouble(CartItem::getGrossAmount).sum(); }
    public double getDiscountTotal() { return cart.values().stream().mapToDouble(this::getDiscount).sum(); }
    public double getTotal() { return getSubtotal() - getDiscountTotal(); }

    public int getLowStockCount() {
        return (int) products.stream().filter(product -> product.getStock() <= 5).count();
    }

    public Bill checkout(String customerName) {
        if (cart.isEmpty()) throw new IllegalStateException("Add at least one product before checkout.");
        for (CartItem item : cart.values()) {
            if (item.getQuantity() > item.getProduct().getStock()) {
                throw new IllegalStateException("Stock changed for " + item.getProduct().getName() + ". Please review the cart.");
            }
        }

        List<BillLine> lines = new ArrayList<>();
        for (CartItem item : cart.values()) {
            lines.add(new BillLine(item.getProduct().getName(), item.getQuantity(),
                    item.getProduct().getUnitPrice(), getDiscount(item)));
        }
        for (CartItem item : cart.values()) item.getProduct().reduceStock(item.getQuantity());

        String billNumber = "FM-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + String.format(Locale.US, "%03d", billSequence.getAndIncrement());
        Bill bill = new Bill(billNumber,
                customerName == null || customerName.isBlank() ? "Walk-in Customer" : customerName.trim(),
                LocalDateTime.now(), lines);
        cart.clear();
        return bill;
    }
}
