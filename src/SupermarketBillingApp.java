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
import javax.swing.JComboBox;
import javax.swing.JTabbedPane;
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

    private final BillingService billingService;
    private final ProductTableModel productModel = new ProductTableModel();
    private final InventoryTableModel inventoryModel = new InventoryTableModel();
    private final CartTableModel cartModel = new CartTableModel();
    private final JTable productTable = new JTable(productModel);
    private final JTable inventoryTable = new JTable(inventoryModel);
    private final JTable cartTable = new JTable(cartModel);
    private final JTextField searchField = new JTextField();
    private final JTextField inventorySearchField = new JTextField();
    private final JTextField customerField = new JTextField("Walk-in Customer");
    private final JSpinner quantitySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 99, 1));
    private final JLabel subtotalValue = new JLabel("₹0.00");
    private final JLabel discountValue = new JLabel("-₹0.00");
    private final JLabel totalValue = new JLabel("₹0.00");
    private final JButton stockAlert = new JButton();
    private final JTabbedPane workspaceTabs = new JTabbedPane();

    public SupermarketBillingApp(BillingService billingService) {
        super("FreshMart | Supermarket Billing");
        this.billingService = billingService;
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
        workspaceTabs.setFont(new Font("Segoe UI", Font.BOLD, 13));
        workspaceTabs.addTab("Checkout", columns);
        workspaceTabs.addTab("Inventory", buildInventoryPanel());
        root.add(workspaceTabs, BorderLayout.CENTER);
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
        JLabel subtitle = new JLabel("Supermarket billing desk  ·  MySQL inventory");
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
        stockAlert.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        stockAlert.setContentAreaFilled(false);
        stockAlert.setFocusPainted(false);
        stockAlert.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        stockAlert.addActionListener(event -> showLowStockProducts());
        alert.add(dot, BorderLayout.WEST); alert.add(stockAlert, BorderLayout.CENTER);
        header.add(alert, BorderLayout.EAST);
        return header;
    }

    private JPanel buildInventoryPanel() {
        JPanel panel = cardPanel();
        panel.setLayout(new BorderLayout(0, 14));

        JPanel titleBar = new JPanel(new BorderLayout(12, 0));
        titleBar.setOpaque(false);
        titleBar.add(sectionHeading("Inventory management", "Maintain product details and keep shelf stock current"), BorderLayout.CENTER);
        JButton add = primaryButton("＋ Add product");
        add.addActionListener(event -> showProductEditor(null));
        titleBar.add(add, BorderLayout.EAST);
        panel.add(titleBar, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 10));
        content.setOpaque(false);
        inventorySearchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inventorySearchField.setToolTipText("Search by product name or code");
        inventorySearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        content.add(inventorySearchField, BorderLayout.NORTH);

        styleTable(inventoryTable);
        inventoryTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        inventoryTable.setFillsViewportHeight(true);
        inventoryTable.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(inventoryTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));
        content.add(scroll, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        JLabel stockNote = new JLabel("On-hand stock changes through sales and restocking.");
        stockNote.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        stockNote.setForeground(MUTED);
        footer.add(stockNote, BorderLayout.WEST);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton edit = secondaryButton("Edit product");
        edit.addActionListener(event -> editSelectedProduct());
        JButton delete = secondaryButton("Delete");
        delete.addActionListener(event -> deleteSelectedProduct());
        JButton restock = primaryButton("Restock");
        restock.addActionListener(event -> restockSelectedProduct());
        actions.add(edit);
        actions.add(delete);
        actions.add(restock);
        footer.add(actions, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        panel.add(content, BorderLayout.CENTER);

        inventorySearchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { filterInventoryProducts(); }
            public void removeUpdate(DocumentEvent event) { filterInventoryProducts(); }
            public void changedUpdate(DocumentEvent event) { filterInventoryProducts(); }
        });
        inventoryTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent event) {
                if (event.getClickCount() == 2) editSelectedProduct();
            }
        });
        return panel;
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

    private void filterInventoryProducts() {
        String query = inventorySearchField.getText().trim().toLowerCase(Locale.ROOT);
        List<Product> filtered = billingService.getProducts().stream()
                .filter(product -> product.getName().toLowerCase(Locale.ROOT).contains(query)
                        || product.getCode().toLowerCase(Locale.ROOT).contains(query))
                .collect(Collectors.toList());
        inventoryModel.setProducts(filtered);
    }

    private Product selectedInventoryProduct() {
        int viewRow = inventoryTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Select a product from the inventory first.",
                    "Choose a product", JOptionPane.INFORMATION_MESSAGE);
            return null;
        }
        return inventoryModel.getProductAt(inventoryTable.convertRowIndexToModel(viewRow));
    }

    private void editSelectedProduct() {
        Product product = selectedInventoryProduct();
        if (product != null) showProductEditor(product);
    }

    private void showProductEditor(Product existing) {
        boolean isNew = existing == null;
        JDialog dialog = new JDialog(this, isNew ? "Add product" : "Edit product", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(0, 14));
        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 22, 18, 22));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        JTextField codeField = new JTextField(isNew ? "" : existing.getCode(), 22);
        codeField.setEditable(isNew);
        JTextField nameField = new JTextField(isNew ? "" : existing.getName(), 22);
        JComboBox<String> categoryField = new JComboBox<>(new String[] {"REGULAR", "PERISHABLE"});
        if (!isNew) categoryField.setSelectedItem(existing.getCategory().toUpperCase(Locale.ROOT));
        JTextField priceField = new JTextField(isNew ? "" : String.format(Locale.ROOT, "%.2f", existing.getUnitPrice()), 22);
        JTextField expiryField = new JTextField(isNew || existing.getExpiryDate() == null
                ? "" : existing.getExpiryDate().toString(), 22);
        expiryField.setToolTipText("yyyy-MM-dd; leave blank if unknown");
        JSpinner stockField = new JSpinner(new SpinnerNumberModel(
                isNew ? 0 : existing.getStock(), 0, Integer.MAX_VALUE, 1));
        if (!isNew) {
            stockField.setEnabled(false);
            stockField.setToolTipText("Use Restock to increase stock; sales reduce it at checkout.");
        }

        addFormRow(form, "Product code", codeField, 0);
        addFormRow(form, "Product name", nameField, 1);
        addFormRow(form, "Product type", categoryField, 2);
        addFormRow(form, "Unit price (₹)", priceField, 3);
        addFormRow(form, "Expiry date", expiryField, 4);
        addFormRow(form, isNew ? "Opening stock" : "Current stock", stockField, 5);
        JLabel hint = new JLabel(isNew
                ? "Perishable expiry is optional. Leave it blank if unknown."
                : "Product code is fixed; use Restock to add stock.");
        hint.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        hint.setForeground(MUTED);
        GridBagConstraints hintConstraints = new GridBagConstraints();
        hintConstraints.gridx = 0; hintConstraints.gridy = 6; hintConstraints.gridwidth = 2;
        hintConstraints.anchor = GridBagConstraints.WEST;
        hintConstraints.insets = new Insets(10, 0, 0, 0);
        form.add(hint, hintConstraints);

        categoryField.addActionListener(event -> {
            boolean perishable = "PERISHABLE".equals(categoryField.getSelectedItem());
            expiryField.setEnabled(perishable);
            if (!perishable) expiryField.setText("");
        });
        expiryField.setEnabled("PERISHABLE".equals(categoryField.getSelectedItem()));

        content.add(sectionHeading(isNew ? "Create a product" : "Update product details",
                isNew ? "Add the details used by the checkout counter"
                        : "Update catalog details; manage stock with Restock"), BorderLayout.NORTH);
        content.add(form, BorderLayout.CENTER);
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JButton cancel = secondaryButton("Cancel");
        cancel.addActionListener(event -> dialog.dispose());
        JButton save = primaryButton(isNew ? "Add product" : "Save changes");
        save.addActionListener(event -> {
            try {
                String code = codeField.getText().trim();
                String name = nameField.getText().trim();
                if (code.isEmpty() || code.length() > 24) {
                    throw new IllegalArgumentException("Product code must contain 1 to 24 characters.");
                }
                if (name.isEmpty() || name.length() > 120) {
                    throw new IllegalArgumentException("Product name must contain 1 to 120 characters.");
                }
                BigDecimal price = new BigDecimal(priceField.getText().trim()).setScale(2, RoundingMode.UNNECESSARY);
                if (price.signum() < 0 || price.compareTo(new BigDecimal("99999999.99")) > 0) {
                    throw new IllegalArgumentException("Unit price must be between ₹0.00 and ₹99,999,999.99.");
                }
                String category = (String) categoryField.getSelectedItem();
                LocalDate expiry = null;
                if ("PERISHABLE".equals(category) && !expiryField.getText().trim().isEmpty()) {
                    try {
                        expiry = LocalDate.parse(expiryField.getText().trim());
                    } catch (DateTimeParseException exception) {
                        throw new IllegalArgumentException("Enter expiry as yyyy-MM-dd, or leave it blank.");
                    }
                }
                int stock = isNew ? (Integer) stockField.getValue() : existing.getStock();
                Product product = "PERISHABLE".equals(category)
                        ? new PerishableProduct(code, name, price.doubleValue(), stock, expiry)
                        : new RegularProduct(code, name, price.doubleValue(), stock);
                if (isNew) billingService.createProduct(product);
                else billingService.updateProduct(product);
                refreshTables();
                dialog.dispose();
                JOptionPane.showMessageDialog(this, isNew ? "Product added to inventory." : "Product details updated.",
                        "Inventory saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (NumberFormatException | ArithmeticException exception) {
                showError("Enter a valid unit price with up to two decimal places.");
            } catch (IllegalArgumentException | SQLException exception) {
                showError(exception.getMessage());
            }
        });
        actions.add(cancel);
        actions.add(save);
        content.add(actions, BorderLayout.SOUTH);
        dialog.add(content, BorderLayout.CENTER);
        dialog.getRootPane().setDefaultButton(save);
        dialog.setSize(560, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addFormRow(JPanel form, String label, Component field, int row) {
        GridBagConstraints left = new GridBagConstraints();
        left.gridx = 0; left.gridy = row; left.anchor = GridBagConstraints.WEST;
        left.insets = new Insets(6, 0, 6, 14);
        JLabel text = new JLabel(label);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        text.setForeground(INK);
        form.add(text, left);

        if (field instanceof JTextField) {
            ((JTextField) field).setFont(new Font("Segoe UI", Font.PLAIN, 13));
            ((JTextField) field).setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER), BorderFactory.createEmptyBorder(8, 9, 8, 9)));
        }
        GridBagConstraints right = new GridBagConstraints();
        right.gridx = 1; right.gridy = row; right.weightx = 1;
        right.fill = GridBagConstraints.HORIZONTAL; right.insets = new Insets(6, 0, 6, 0);
        form.add(field, right);
    }

    private void deleteSelectedProduct() {
        Product product = selectedInventoryProduct();
        if (product == null) return;
        int answer = JOptionPane.showConfirmDialog(this,
                "Delete “" + product.getName() + "” from the catalog? Existing receipts will remain saved.",
                "Delete product", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (answer != JOptionPane.YES_OPTION) return;
        try {
            billingService.deleteProduct(product.getCode());
            refreshTables();
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    private void restockSelectedProduct() {
        Product product = selectedInventoryProduct();
        if (product == null) return;
        JSpinner quantity = new JSpinner(new SpinnerNumberModel(10, 1, 1_000_000, 1));
        JPanel prompt = new JPanel(new BorderLayout(10, 0));
        prompt.add(new JLabel("Units to add to “" + product.getName() + "”:"), BorderLayout.CENTER);
        prompt.add(quantity, BorderLayout.EAST);
        int answer = JOptionPane.showConfirmDialog(this, prompt, "Restock product",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION) return;
        try {
            int amount = (Integer) quantity.getValue();
            billingService.restockProduct(product.getCode(), amount);
            refreshTables();
            JOptionPane.showMessageDialog(this, product.getName() + " restocked. New on-hand quantity: "
                            + product.getStock() + ".",
                    "Restock complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IllegalArgumentException | SQLException exception) {
            showError(exception.getMessage());
        }
    }

    private void showLowStockProducts() {
        List<Product> lowStock = billingService.getLowStockProducts();
        workspaceTabs.setSelectedIndex(1);
        if (lowStock.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All products are above the low-stock threshold of 5 units.",
                    "Stock levels", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        StringBuilder message = new StringBuilder("Products at or below 5 available units:\n\n");
        for (Product product : lowStock) {
            message.append(product.getCode()).append("  ·  ").append(product.getName())
                    .append("  —  ").append(billingService.getAvailableStock(product)).append(" available\n");
        }
        message.append("\nSelect a product and choose Restock in Inventory.");
        JOptionPane.showMessageDialog(this, message.toString(), "Low-stock notification",
                JOptionPane.WARNING_MESSAGE);
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
        } catch (IllegalStateException | SQLException exception) {
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
        filterInventoryProducts();
        cartModel.setItems(billingService.getCartItems());
        subtotalValue.setText(money(billingService.getSubtotal()));
        discountValue.setText("-" + money(billingService.getDiscountTotal()));
        totalValue.setText(money(billingService.getTotal()));
        int lowStock = billingService.getLowStockCount();
        stockAlert.setText(lowStock == 0
                ? "All stock healthy · click to review"
                : "Low stock: " + lowStock + " product(s) · click to review");
        stockAlert.setForeground(lowStock == 0 ? GREEN : new Color(119, 78, 25));
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
            table.getColumnModel().getColumn(3).setCellRenderer(stockRenderer());
            table.getColumnModel().getColumn(0).setPreferredWidth(76);
            table.getColumnModel().getColumn(1).setPreferredWidth(150);
            table.getColumnModel().getColumn(2).setPreferredWidth(82);
            table.getColumnModel().getColumn(3).setPreferredWidth(82);
        } else if (table == inventoryTable) {
            table.getColumnModel().getColumn(3).setCellRenderer(right);
            table.getColumnModel().getColumn(4).setCellRenderer(stockRenderer());
            table.getColumnModel().getColumn(0).setPreferredWidth(100);
            table.getColumnModel().getColumn(1).setPreferredWidth(260);
            table.getColumnModel().getColumn(2).setPreferredWidth(110);
            table.getColumnModel().getColumn(3).setPreferredWidth(120);
            table.getColumnModel().getColumn(4).setPreferredWidth(110);
            table.getColumnModel().getColumn(5).setPreferredWidth(150);
        } else {
            for (int i = 1; i < table.getColumnCount(); i++) table.getColumnModel().getColumn(i).setCellRenderer(right);
            table.getColumnModel().getColumn(0).setPreferredWidth(120);
            table.getColumnModel().getColumn(1).setPreferredWidth(42);
        }
    }

    private DefaultTableCellRenderer stockRenderer() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                super.getTableCellRendererComponent(table, value, selected, focused, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                if (!selected && value instanceof Number && ((Number) value).intValue() <= 5) {
                    setForeground(ORANGE);
                    setFont(getFont().deriveFont(Font.BOLD));
                } else if (!selected) {
                    setForeground(INK);
                    setFont(getFont().deriveFont(Font.PLAIN));
                }
                return this;
            }
        };
    }

    private static String money(double amount) { return String.format(Locale.forLanguageTag("en-IN"), "₹%,.2f", amount); }

    private class ProductTableModel extends AbstractTableModel {
        private final String[] columns = {"CODE", "PRODUCT", "PRICE", "AVAILABLE", "BEST BEFORE"};
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
                case 3: return billingService.getAvailableStock(product);
                case 4: return product.getExpiryDate() == null ? "—" : product.getExpiryDate().format(DateTimeFormatter.ofPattern("dd MMM"));
                default: return "";
            }
        }
    }

    private class InventoryTableModel extends AbstractTableModel {
        private final String[] columns = {"CODE", "PRODUCT", "TYPE", "UNIT PRICE", "ON HAND", "EXPIRY DATE"};
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
                case 2: return product.getCategory();
                case 3: return money(product.getUnitPrice());
                case 4: return product.getStock();
                case 5: return product.getExpiryDate() == null ? "—"
                        : product.getExpiryDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
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
            try {
                ProductRepository repository = new MySqlProductRepository(DatabaseConnection.load());
                BillingService billingService = new BillingService(repository);
                if (billingService.getProducts().isEmpty()) {
                    JOptionPane.showMessageDialog(null,
                            "MySQL connected, but the product table is empty. Run database/schema.sql first.",
                            "No products found", JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                new SupermarketBillingApp(billingService).setVisible(true);
            } catch (IOException | SQLException exception) {
                JOptionPane.showMessageDialog(null,
                        "Could not load the product catalog from MySQL.\n\n"
                                + "Check that MySQL Server is running, database/schema.sql has been executed, "
                                + "and config/db.properties has the correct username and password.\n\n"
                                + "Details: " + exception.getMessage(),
                        "MySQL connection failed", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
