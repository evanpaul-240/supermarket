import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ExpiryDiscountPolicy implements DiscountPolicy {
    private static final double DISCOUNT_RATE = 0.10;
    private static final long DAYS_BEFORE_EXPIRY = 7;

    @Override
    public double calculateDiscount(CartItem item, LocalDate today) {
        LocalDate expiry = item.getProduct().getExpiryDate();
        if (expiry == null || expiry.isBefore(today)) return 0;
        long daysRemaining = ChronoUnit.DAYS.between(today, expiry);
        return daysRemaining <= DAYS_BEFORE_EXPIRY ? item.getGrossAmount() * DISCOUNT_RATE : 0;
    }

    @Override
    public String getDescription() { return "10% near-expiry discount"; }
}
