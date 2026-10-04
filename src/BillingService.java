import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.stream.Collectors;

/** Coordinates stock, cart operations, discount rules, and checkout. */
public class BillingService {
    private final ProductRepository productRepository;
    private final List<Product> products;
    private final Map<String, CartItem> cart = new LinkedHashMap<>();
    private final List<DiscountPolicy> discountPolicies = List.of(
            new ExpiryDiscountPolicy(), new QuantityDiscountPolicy());

    public BillingService(ProductRepository productRepository) throws SQLException {
        this.productRepository = productRepository;
        this.products = new ArrayList<>(productRepository.findAll());
    }

    public List<Product> getProducts() { return List.copyOf(products); }
    public List<CartItem> getCartItems() { return new ArrayList<>(cart.values()); }
    public List<DiscountPolicy> getDiscountPolicies() { return discountPolicies; }

    /** Stock available to another cart after reserving this cashier's current cart. */
    public int getAvailableStock(Product product) {
        CartItem item = cart.get(product.getCode());
        return product.getStock() - (item == null ? 0 : item.getQuantity());
    }

    public void createProduct(Product product) throws SQLException {
        if (findProduct(product.getCode()) != null) {
            throw new IllegalArgumentException("That product code already exists. Choose a unique code.");
        }
        productRepository.createProduct(product);
        products.add(product);
        products.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
    }

    public void updateProduct(Product updatedProduct) throws SQLException {
        Product current = findProduct(updatedProduct.getCode());
        if (current == null) throw new IllegalArgumentException("That product no longer exists. Refresh the inventory.");
        if (cart.containsKey(current.getCode())) {
            throw new IllegalArgumentException("Remove this product from the current bill before editing it.");
        }
        productRepository.updateProduct(updatedProduct);
        products.set(products.indexOf(current), updatedProduct);
        products.sort(Comparator.comparing(Product::getName, String.CASE_INSENSITIVE_ORDER));
    }

    public void deleteProduct(String productCode) throws SQLException {
        Product product = findProduct(productCode);
        if (product == null) throw new IllegalArgumentException("That product no longer exists. Refresh the inventory.");
        if (cart.containsKey(productCode)) {
            throw new IllegalArgumentException("Remove this product from the current bill before deleting it.");
        }
        productRepository.deleteProduct(productCode);
        products.remove(product);
    }

    public void restockProduct(String productCode, int quantity) throws SQLException {
        Product product = findProduct(productCode);
        if (product == null) throw new IllegalArgumentException("That product no longer exists. Refresh the inventory.");
        if (quantity < 1) throw new IllegalArgumentException("Restock quantity must be at least one.");
        if ((long) product.getStock() + quantity > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("That restock amount exceeds the supported stock limit.");
        }
        productRepository.restockProduct(productCode, quantity);
        product.increaseStock(quantity);
    }

    private Product findProduct(String productCode) {
        return products.stream().filter(product -> product.getCode().equalsIgnoreCase(productCode))
                .findFirst().orElse(null);
    }

    public void addToCart(Product product, int quantity) {
        if (product == null || !products.contains(product)) throw new IllegalArgumentException("Choose a product from the catalog.");
        if (quantity < 1) throw new IllegalArgumentException("Quantity must be at least one.");
        if (product.isExpired(LocalDate.now())) throw new IllegalArgumentException("Expired products cannot be sold.");
        CartItem existing = cart.get(product.getCode());
        int alreadyInCart = existing == null ? 0 : existing.getQuantity();
        if (alreadyInCart + quantity > product.getStock()) {
            throw new IllegalArgumentException("Only " + (product.getStock() - alreadyInCart)
                    + " unit(s) are currently available.");
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
        return (int) products.stream().filter(product -> getAvailableStock(product) <= 5).count();
    }

    public List<Product> getLowStockProducts() {
        return products.stream().filter(product -> getAvailableStock(product) <= 5).collect(Collectors.toList());
    }

    public Bill checkout(String customerName) throws SQLException {
        if (cart.isEmpty()) throw new IllegalStateException("Add at least one product before checkout.");
        for (CartItem item : cart.values()) {
            if (item.getQuantity() > item.getProduct().getStock()) {
                throw new IllegalStateException("Stock changed for " + item.getProduct().getName() + ". Please review the cart.");
            }
        }

        List<BillLine> lines = new ArrayList<>();
        for (CartItem item : cart.values()) {
            lines.add(new BillLine(item.getProduct().getCode(), item.getProduct().getName(), item.getQuantity(),
                    item.getProduct().getUnitPrice(), getDiscount(item)));
        }
        String billNumber = "FM-" + LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"))
                + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Bill bill = new Bill(billNumber,
                customerName == null || customerName.isBlank() ? "Walk-in Customer" : customerName.trim(),
                LocalDateTime.now(), lines);

        // Persist the bill and decrement database stock in one transaction before
        // changing the in-memory view. A failed transaction leaves the cart intact.
        productRepository.saveSale(bill);
        for (CartItem item : cart.values()) item.getProduct().reduceStock(item.getQuantity());
        cart.clear();
        return bill;
    }
}
