import java.time.LocalDate;

public class QuantityDiscountPolicy implements DiscountPolicy {
    private static final int MINIMUM_QUANTITY = 3;
    private static final double DISCOUNT_RATE = 0.05;

    @Override
    public double calculateDiscount(CartItem item, LocalDate today) {
        return item.getQuantity() >= MINIMUM_QUANTITY ? item.getGrossAmount() * DISCOUNT_RATE : 0;
    }

    @Override
    public String getDescription() { return "5% multi-buy discount (3 or more)"; }
}
