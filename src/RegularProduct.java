public class RegularProduct extends Product {
    public RegularProduct(String code, String name, double unitPrice, int stock) {
        super(code, name, unitPrice, stock);
    }

    @Override
    public String getCategory() { return "Regular"; }
}
