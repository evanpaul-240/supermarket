import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class SupermarketBillingApp extends JFrame {
    private static final Color INK = new Color(28, 45, 43);
    private static final Color MUTED = new Color(105, 122, 117);
    private static final Color GREEN = new Color(30, 126, 93);
    private static final Color PALE = new Color(243, 248, 245);
    private static final Color BORDER = new Color(222, 232, 226);
    private static final Color ORANGE = new Color(177, 105, 23);

    private final BillingService billingService = new BillingService(createSampleProducts());
    private final ProductTableModel productModel = new ProductTableModel();
    private final CartTableModel cartModel = new CartTableModel();
    private final JTable productTable = new JTable(productModel);
    private final JTable cartTable = new JTable(cartModel);
    private final JTextField searchField = new JTextField();
    private final JTextField customerField = new JTextField("Walk-in Customer");
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JLabel subtotalValue = new JLabel("₹0.00");
    private final JLabel discountValue = new JLabel("-₹0.00");
    private final JLabel totalValue = new JLabel("₹0.00");
    private final JLabel stockAlert = new JLabel();

    public SupermarketBillingApp() {
        super("FreshMart | Supermarket Billing");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 680));
        setSize(1240, 790);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        refreshTables();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 18));
        root.setBackground(PALE);
        root.setBorder(BorderFactory.createEmptyBorder(22, 26, 24, 26));
        root.add(buildHeader(), BorderLayout.NORTH);

        JPanel columns = new JPanel(new GridBagLayout());
        columns.setOpaque(false);
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = 0; left.weightx = 0.58; left.weighty = 1;
        left.fill = GridBagConstraints.BOTH; left.insets = new Insets(0, 0, 0, 12);
        columns.add(buildCatalogPanel(), left);
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = 0; right.weightx = 0.42; right.weighty = 1;
        right.fill = GridBagConstraints.BOTH; right.insets = new Insets(0, 12, 0, 0);
        columns.add(buildCheckoutPanel(), right);
        root.add(columns, BorderLayout.CENTER);
        return root;
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titleBlock = new JPanel(new BorderLayout(0, 3));
        titleBlock.setOpaque(false);
        JLabel brand = new JLabel("FRESHMART");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 25));
        brand.setForeground(GREEN);
        JLabel subtitle = new JLabel("Supermarket billing desk  ·  Demo model");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(MUTED);
        titleBlock.add(brand, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);
        header.add(titleBlock, BorderLayout.WEST);

        JPanel alert = new JPanel(new BorderLayout(8, 0));
        alert.setBackground(new Color(255, 248, 233));
        alert.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(245, 225, 188)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel dot = new JLabel("●"); dot.setForeground(ORANGE);
        stockAlert.setFont(new Font("Segoe UI", Font.BOLD, 12));
        stockAlert.setForeground(new Color(119, 78, 25));
        alert.add(dot, BorderLayout.WEST); alert.add(stockAlert, BorderLayout.CENTER);
        header.add(alert, BorderLayout.EAST);
        return header;
    }

    private JPanel buildCatalogPanel() {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(0, 14));
        panel.add(sectionHeading("Product catalog", "Search a name or scan a product code"), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchField.setToolTipText("Search by product name or code; scanner input is supported");
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        center.add(searchField, BorderLayout.NORTH);
        styleTable(productTable);
        productTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(productTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        center.add(scroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new BorderLayout(10, 0));
        actions.setOpaque(false);
        JPanel qty = new JPanel(new BorderLayout(8, 0)); qty.setOpaque(false);
        JLabel qtyLabel = new JLabel("Qty"); qtyLabel.setForeground(MUTED);
        qty.add(qtyLabel, BorderLayout.WEST); qty.add(quantitySpinner, BorderLayout.CENTER);
        quantitySpinner.setPreferredSize(new Dimension(72, 38));
        JButton add = primaryButton("Add to cart");
        add.addActionListener(event -> addSelectedProduct());
        actions.add(qty, BorderLayout.WEST); actions.add(add, BorderLayout.CENTER);
        center.add(actions, BorderLayout.SOUTH);
        panel.add(center, BorderLayout.CENTER);

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterProducts(); }
            public void removeUpdate(DocumentEvent e) { filterProducts(); }
            public void changedUpdate(DocumentEvent e) { filterProducts(); }
        });
        searchField.addActionListener(event -> {
            if (productModel.getRowCount() > 0) {
                productTable.setRowSelectionInterval(0, 0);
                quantitySpinner.setValue(1);
                addSelectedProduct();
            }
        });
        productTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && productTable.getSelectedRow() >= 0) {
                Product p = productModel.getProductAt(productTable.getSelectedRow());
                quantitySpinner.setValue(Math.min(1, Math.max(1, p.getStock())));
            }
        });
        return panel;
    }

    private JPanel buildCheckoutPanel() {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(0, 12));
        panel.add(sectionHeading("Current bill", "Add items, review discounts, then complete sale"), BorderLayout.NORTH);

        JPanel middle = new JPanel(new BorderLayout(0, 10));
        middle.setOpaque(false);
        JPanel customer = new JPanel(new BorderLayout(8, 0)); customer.setOpaque(false);
        JLabel customerLabel = new JLabel("Customer"); customerLabel.setForeground(MUTED);
        customerField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        customerField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        customer.add(customerLabel, BorderLayout.WEST); customer.add(customerField, BorderLayout.CENTER);
        middle.add(customer, BorderLayout.NORTH);
        styleTable(cartTable);
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createLineBorder(BORDER));
        middle.add(cartScroll, BorderLayout.CENTER);

        JPanel lower = new JPanel(new BorderLayout(0, 12)); lower.setOpaque(false);
        JPanel totals = new JPanel(new GridBagLayout()); totals.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints(); c.insets = new Insets(4, 0, 4, 0);
        c.gridx = 0; c.gridy = 0; c.anchor = GridBagConstraints.WEST; c.weightx = 1;
        JLabel sub = new JLabel("Subtotal"); sub.setForeground(MUTED); totals.add(sub, c);
        c.gridx = 1; c.anchor = GridBagConstraints.EAST; totals.add(subtotalValue, c);
        c.gridx = 0; c.gridy = 1; c.anchor = GridBagConstraints.WEST;
        JLabel disc = new JLabel("Discounts"); disc.setForeground(GREEN); totals.add(disc, c);
        c.gridx = 1; c.anchor = GridBagConstraints.EAST; discountValue.setForeground(GREEN); totals.add(discountValue, c);
        c.gridx = 0; c.gridy = 2; c.anchor = GridBagConstraints.WEST;
        JLabel total = new JLabel("Amount due"); total.setFont(new Font("Segoe UI", Font.BOLD, 17)); total.setForeground(INK); totals.add(total, c);
        c.gridx = 1; c.anchor = GridBagConstraints.EAST; totalValue.setFont(new Font("Segoe UI", Font.BOLD, 20)); totalValue.setForeground(GREEN); totals.add(totalValue, c);
        lower.add(totals, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new BorderLayout(9, 0)); buttons.setOpaque(false);
        JButton remove = secondaryButton("Remove item");
        remove.addActionListener(event -> removeSelectedItem());
        JButton clear = secondaryButton("Clear");
        clear.addActionListener(event -> { billingService.clearCart(); refreshTables(); });
        JPanel leftButtons = new JPanel(new BorderLayout(8, 0)); leftButtons.setOpaque(false);
        leftButtons.add(remove, BorderLayout.CENTER); leftButtons.add(clear, BorderLayout.EAST);
        JButton checkout = primaryButton("Complete sale  →");
        checkout.addActionListener(event -> completeSale());
        JPanel actions = new JPanel(new BorderLayout(0, 9)); actions.setOpaque(false);
        actions.add(leftButtons, BorderLayout.NORTH); actions.add(checkout, BorderLayout.CENTER);
        lower.add(actions, BorderLayout.SOUTH);
        middle.add(lower, BorderLayout.SOUTH);
        panel.add(middle, BorderLayout.CENTER);
        return panel;
    }

    private void addSelectedProduct() {
        int row = productTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a product from the catalog first.", "Choose a product", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        Product product = productModel.getProductAt(row);
        try {
            billingService.addToCart(product, (Integer) quantitySpinner.getValue());
            refreshTables();
            searchField.requestFocusInWindow();
            searchField.selectAll();
        } catch (IllegalArgumentException exception) {
            showError(exception.getMessage());
        }
    }

    private void removeSelectedItem() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a cart item to remove.", "Choose an item", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        billingService.removeFromCart(cartModel.getProductCodeAt(row));
        refreshTables();
    }

    private void completeSale() {
        try {
            Bill bill = billingService.checkout(customerField.getText());
            refreshTables();
            showReceipt(bill);
        } catch (IllegalStateException exception) {
            showError(exception.getMessage());
        }
    }

    private void showReceipt(Bill bill) {
        JDialog dialog = new JDialog(this, "Sale complete · " + bill.getBillNumber(), true);
        dialog.setLayout(new BorderLayout(12, 12));
        dialog.getRootPane().setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        JTextArea receipt = new JTextArea(bill.toReceiptText());
        receipt.setEditable(false);
        receipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        receipt.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        receipt.setBackground(new Color(250, 251, 249));
        dialog.add(new JScrollPane(receipt), BorderLayout.CENTER);
        JPanel actions = new JPanel(new BorderLayout(8, 0));
        JButton save = secondaryButton("Save digital receipt");
        save.addActionListener(event -> saveReceipt(dialog, bill));
        JButton done = primaryButton("Done");
        done.addActionListener(event -> dialog.dispose());
        actions.add(save, BorderLayout.WEST); actions.add(done, BorderLayout.EAST);
        dialog.add(actions, BorderLayout.SOUTH);
        dialog.setSize(520, 540);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void saveReceipt(Component parent, Bill bill) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save digital receipt");
        chooser.setSelectedFile(new java.io.File("receipt-" + bill.getBillNumber() + ".txt"));
        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.writeString(chooser.getSelectedFile().toPath(), bill.toReceiptText(), StandardCharsets.UTF_8);
                JOptionPane.showMessageDialog(parent, "Receipt saved successfully.", "Receipt saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException exception) {
                showError("Could not save receipt: " + exception.getMessage());
            }
        }
    }

    private void filterProducts() {
        String query = searchField.getText().trim().toLowerCase(Locale.ROOT);
        List<Product> filtered = billingService.getProducts().stream()
                .filter(product -> product.getName().toLowerCase(Locale.ROOT).contains(query)
                        || product.getCode().toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
        productModel.setProducts(filtered);
        if (!filtered.isEmpty()) productTable.setRowSelectionInterval(0, 0);
    }

    private void refreshTables() {
        filterProducts();
        cartModel.setItems(billingService.getCartItems());
        subtotalValue.setText(money(billingService.getSubtotal()));
        discountValue.setText("-" + money(billingService.getDiscountTotal()));
        totalValue.setText(money(billingService.getTotal()));
        int lowStock = billingService.getLowStockCount();
        stockAlert.setText(lowStock == 0 ? "All products are sufficiently stocked" : lowStock + " product(s) need restocking soon");
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Unable to continue", JOptionPane.WARNING_MESSAGE);
    }

    private JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(18, 18, 18, 18)));
        return panel;
    }

    private JPanel sectionHeading(String title, String subtitle) {
        JPanel heading = new JPanel(new BorderLayout(0, 3)); heading.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18)); titleLabel.setForeground(INK);
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12)); subtitleLabel.setForeground(MUTED);
        heading.add(titleLabel, BorderLayout.NORTH); heading.add(subtitleLabel, BorderLayout.SOUTH);
        return heading;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(Color.WHITE); button.setBackground(GREEN);
        button.setFocusPainted(false); button.setBorder(BorderFactory.createEmptyBorder(11, 14, 11, 14));
        button.setOpaque(true);
        return button;
    }

    private JButton secondaryButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(INK); button.setBackground(Color.WHITE);
        button.setFocusPainted(false); button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(9, 11, 9, 11)));
        button.setOpaque(true);
        return button;
    }

    private void styleTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        table.setForeground(INK); table.setRowHeight(36);
        table.setGridColor(new Color(237, 242, 238));
        table.setSelectionBackground(new Color(225, 243, 234));
        table.setSelectionForeground(INK);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setForeground(MUTED); header.setBackground(new Color(248, 250, 248));
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        if (table == productTable) {
            table.getColumnModel().getColumn(2).setCellRenderer(right);
            table.getColumnModel().getColumn(3).setCellRenderer(right);
            table.getColumnModel().getColumn(4).setCellRenderer(right);
            table.getColumnModel().getColumn(0).setPreferredWidth(76);
            table.getColumnModel().getColumn(1).setPreferredWidth(150);
            table.getColumnModel().getColumn(2).setPreferredWidth(82);
            table.getColumnModel().getColumn(3).setPreferredWidth(82);
        } else {
            for (int i = 1; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(right);
            table.getColumnModel().getColumn(0).setPreferredWidth(120);
            table.getColumnModel().getColumn(1).setPreferredWidth(42);
        }
    }

    private static String money(double amount) { return String.format(Locale.forLanguageTag("en-IN"), "₹%,.2f", amount); }

    private static List<Product> createSampleProducts() {
        LocalDate today = LocalDate.now();
        List<Product> products = new ArrayList<>();
        products.add(new RegularProduct("100101", "Basmati Rice 1 kg", 118.00, 42));
        products.add(new RegularProduct("100102", "Wheat Flour 1 kg", 62.00, 28));
        products.add(new RegularProduct("100103", "Tea 250 g", 145.00, 31));
        products.add(new RegularProduct("100104", "Dishwash Liquid", 99.00, 2));
        products.add(new RegularProduct("100105", "Pasta 500 g", 78.00, 18));
        products.add(new PerishableProduct("200201", "Fresh Milk 1 L", 68.00, 12, today.plusDays(5)));
        products.add(new PerishableProduct("200202", "Whole Wheat Bread", 45.00, 5, today.plusDays(1)));
        products.add(new PerishableProduct("200203", "Plain Yogurt 400 g", 72.00, 14, today.plusDays(3)));
        products.add(new PerishableProduct("200204", "Apples 1 kg", 165.00, 9, today.plusDays(10)));
        products.add(new PerishableProduct("200205", "Cheddar Cheese 200 g", 132.00, 4, today.plusDays(6)));
        return products;
    }

    private class ProductTableModel extends AbstractTableModel {
        private final String[] columns = {"CODE", "PRODUCT", "PRICE", "STOCK", "BEST BEFORE"};
        private List<Product> rows = new ArrayList<>();
        public void setProducts(List<Product> products) { rows = new ArrayList<>(products); fireTableDataChanged(); }
        public Product getProductAt(int row) { return rows.get(row); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }
        @Override public Object getValueAt(int row, int col) {
            Product product = rows.get(row);
            switch (col) {
                case 0: return product.getCode();
                case 1: return product.getName();
                case 2: return money(product.getUnitPrice());
                case 3: return product.getStock();
                case 4: return product.getExpiryDate() == null ? "—" : product.getExpiryDate().format(DateTimeFormatter.ofPattern("dd MMM"));
                default: return "";
            }
        }
    }

    private class CartTableModel extends AbstractTableModel {
        private final String[] columns = {"PRODUCT", "QTY", "PRICE", "SAVE", "TOTAL"};
        private List<CartItem> rows = new ArrayList<>();
        public void setItems(List<CartItem> items) { rows = new ArrayList<>(items); fireTableDataChanged(); }
        public String getProductCodeAt(int row) { return rows.get(row).getProduct().getCode(); }
        @Override public int getRowCount() { return rows.size(); }
        @Override public int getColumnCount() { return columns.length; }
        @Override public String getColumnName(int col) { return columns[col]; }
        @Override public Object getValueAt(int row, int col) {
            CartItem item = rows.get(row);
            double discount = billingService.getDiscount(item);
            switch (col) {
                case 0: return item.getProduct().getName();
                case 1: return item.getQuantity();
                case 2: return money(item.getProduct().getUnitPrice());
                case 3: return discount == 0 ? "—" : "-" + money(discount);
                case 4: return money(item.getGrossAmount() - discount);
                default: return "";
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new SupermarketBillingApp().setVisible(true);
        });
    }
}
