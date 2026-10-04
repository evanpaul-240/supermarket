import java.time.LocalDate;

/** Abstraction for any rule that can reduce a cart line's price. */
public interface DiscountPolicy {
    double calculateDiscount(CartItem item, LocalDate today);
    String getDescription();
}
