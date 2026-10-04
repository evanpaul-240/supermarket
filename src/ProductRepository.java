import java.sql.SQLException;
import java.util.List;

/** Database-facing abstraction used by the billing logic. */
public interface ProductRepository {
    List<Product> findAll() throws SQLException;
    void createProduct(Product product) throws SQLException;
    void updateProduct(Product product) throws SQLException;
    void deleteProduct(String productCode) throws SQLException;
    void restockProduct(String productCode, int quantity) throws SQLException;
    void saveSale(Bill bill) throws SQLException;
}
