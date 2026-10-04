import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class Bill {
    private final String billNumber;
    private final String customerName;
    private final LocalDateTime createdAt;
    private final List<BillLine> lines;

    public Bill(String billNumber, String customerName, LocalDateTime createdAt, List<BillLine> lines) {
        this.billNumber = billNumber;
        this.customerName = customerName;
        this.createdAt = createdAt;
        this.lines = List.copyOf(lines);
    }

    public String getBillNumber() { return billNumber; }
    public List<BillLine> getLines() { return lines; }
    public double getSubtotal() { return lines.stream().mapToDouble(BillLine::getGrossAmount).sum(); }
    public double getDiscountTotal() { return lines.stream().mapToDouble(BillLine::getDiscount).sum(); }
    public double getTotal() { return getSubtotal() - getDiscountTotal(); }

    public String toReceiptText() {
        StringBuilder receipt = new StringBuilder();
        receipt.append("              FRESHMART\n")
                .append("          SUPERMARKET RECEIPT\n")
                .append("---------------------------------------------\n")
                .append("Bill: ").append(billNumber).append('\n')
                .append("Date: ").append(createdAt.format(DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a"))).append('\n')
                .append("Customer: ").append(customerName).append('\n')
                .append("---------------------------------------------\n")
                .append(String.format(Locale.US, "%-19s %3s %8s %8s%n", "Item", "Qty", "Price", "Total"));
        for (BillLine line : lines) {
            receipt.append(String.format(Locale.US, "%-19s %3d %8.2f %8.2f%n",
                    shorten(line.getProductName(), 19), line.getQuantity(), line.getUnitPrice(), line.getNetAmount()));
            if (line.getDiscount() > 0) {
                receipt.append(String.format(Locale.US, "   Discount saved:                   -%.2f%n", line.getDiscount()));
            }
        }
        receipt.append("---------------------------------------------\n")
                .append(String.format(Locale.US, "Subtotal:                         ₹%8.2f%n", getSubtotal()))
                .append(String.format(Locale.US, "Discount:                         -₹%7.2f%n", getDiscountTotal()))
                .append(String.format(Locale.US, "AMOUNT PAID:                       ₹%8.2f%n", getTotal()))
                .append("---------------------------------------------\n")
                .append("       Thank you for shopping with us!\n");
        return receipt.toString();
    }

    private String shorten(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
