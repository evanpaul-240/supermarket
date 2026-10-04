import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/** JavaFX point-of-sale interface for the supermarket billing demo. */
public class SupermarketBillingApp extends Application {
    private static final PseudoClass LOW_STOCK = PseudoClass.getPseudoClass("low-stock");
    private static final PseudoClass COMPACT_LAYOUT = PseudoClass.getPseudoClass("compact");
    private static final DateTimeFormatter EXPIRY_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final ObservableList<Product> checkoutProducts = FXCollections.observableArrayList();
    private final ObservableList<Product> inventoryProducts = FXCollections.observableArrayList();
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();

    private BillingService billingService;
    private Stage stage;
    private TabPane tabs;
    private Tab checkoutTab;
    private Tab inventoryTab;
    private TableView<Product> checkoutTable;
    private TableView<Product> inventoryTable;
    private TableView<CartItem> cartTable;
    private TextField checkoutSearch;
    private TextField inventorySearch;
    private TextField customerField;
    private Spinner<Integer> quantitySpinner;
    private Label subtotalLabel;
    private Label discountLabel;
    private Label totalLabel;
    private Button stockNotice;
    private Button checkoutNav;
    private Button inventoryNav;

    @Override
    public void start(Stage primaryStage) {
        stage = primaryStage;
        try {
            ProductRepository repository = new MySqlProductRepository(DatabaseConnection.load());
            billingService = new BillingService(repository);
        } catch (IOException | SQLException exception) {
            showAlert(Alert.AlertType.ERROR, "MySQL connection failed",
                    "Could not load the product catalog. Check that MySQL is running, the schema has been applied, "
                            + "and config/db.properties contains the right credentials.\n\nDetails: "
                            + exception.getMessage());
            Platform.exit();
            return;
        }

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");

        tabs = new TabPane();
        tabs.getStyleClass().add("workspace-tabs");
        checkoutTab = new Tab("Checkout", buildCheckoutTab());
        checkoutTab.setClosable(false);
        inventoryTab = new Tab("Inventory", buildInventoryTab());
        inventoryTab.setClosable(false);
        tabs.getTabs().addAll(checkoutTab, inventoryTab);
        tabs.getSelectionModel().selectedItemProperty().addListener(
                (observable, previous, selected) -> updateNavigation(selected));

        BorderPane content = new BorderPane();
        content.getStyleClass().add("main-content");
        content.setTop(buildHeader());
        content.setCenter(tabs);
        BorderPane.setMargin(tabs, new Insets(0, 28, 26, 28));
        root.setLeft(buildSidebar());
        root.setCenter(content);
        updateNavigation(tabs.getSelectionModel().getSelectedItem());

        Scene scene = new Scene(root, 1360, 800);
        if (getClass().getResource("/styles.css") != null) {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        }
        stage.setTitle("FreshMart | Supermarket Billing");
        stage.setMinWidth(960);
        stage.setMinHeight(620);
        stage.setScene(scene);
        root.widthProperty().addListener((observable, oldWidth, newWidth) ->
                updateCompactLayout(root, newWidth.doubleValue(), root.getHeight()));
        root.heightProperty().addListener((observable, oldHeight, newHeight) ->
                updateCompactLayout(root, root.getWidth(), newHeight.doubleValue()));
        updateCompactLayout(root, scene.getWidth(), scene.getHeight());
        stage.setMaximized(true);
        stage.show();
        refreshViews();
    }

    private Node buildSidebar() {
        VBox sidebar = new VBox(30);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(238);
        sidebar.setMinWidth(190);
        sidebar.setMaxWidth(260);

        HBox brand = new HBox(11);
        brand.setAlignment(Pos.CENTER_LEFT);
        Label mark = new Label("F");
        mark.getStyleClass().add("brand-mark");
        VBox brandCopy = new VBox(2);
        Label name = new Label("FreshMart");
        name.getStyleClass().add("brand-name");
        Label caption = new Label("BILLING DESK");
        caption.getStyleClass().add("brand-caption");
        brandCopy.getChildren().addAll(name, caption);
        brand.getChildren().addAll(mark, brandCopy);

        VBox navigation = new VBox(7);
        Label navHeading = new Label("WORKSPACE");
        navHeading.getStyleClass().add("nav-heading");
        checkoutNav = navigationButton("▦", "Checkout");
        inventoryNav = navigationButton("▤", "Inventory");
        checkoutNav.setOnAction(event -> tabs.getSelectionModel().select(checkoutTab));
        inventoryNav.setOnAction(event -> tabs.getSelectionModel().select(inventoryTab));
        navigation.getChildren().addAll(navHeading, checkoutNav, inventoryNav);

        Region push = spacer();
        VBox connection = new VBox(7);
        connection.getStyleClass().add("connection-card");
        Label connectionHeading = new Label("DATABASE");
        connectionHeading.getStyleClass().add("connection-heading");
        Label connectionStatus = new Label("●  MySQL connected");
        connectionStatus.getStyleClass().add("connection-status");
        connection.getChildren().addAll(connectionHeading, connectionStatus);

        sidebar.getChildren().addAll(brand, navigation, push, connection);
        VBox.setVgrow(push, Priority.ALWAYS);
        return sidebar;
    }

    private Button navigationButton(String iconText, String title) {
        Button button = new Button(iconText + "    " + title);
        button.getStyleClass().add("sidebar-link");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setAlignment(Pos.CENTER_LEFT);
        return button;
    }

    private void updateNavigation(Tab selected) {
        if (checkoutNav == null || inventoryNav == null) return;
        checkoutNav.getStyleClass().remove("active");
        inventoryNav.getStyleClass().remove("active");
        (selected == inventoryTab ? inventoryNav : checkoutNav).getStyleClass().add("active");
    }

    private void updateCompactLayout(BorderPane root, double width, double height) {
        boolean compact = width < 1220 || height < 760;
        root.pseudoClassStateChanged(COMPACT_LAYOUT, compact);
    }

    private Node buildHeader() {
        BorderPane header = new BorderPane();
        header.getStyleClass().add("top-bar");

        VBox brand = new VBox(3);
        Label title = new Label("Supermarket operations");
        title.getStyleClass().add("header-title");
        Label subtitle = new Label("Point of sale and inventory management");
        subtitle.getStyleClass().add("brand-subtitle");
        brand.getChildren().addAll(title, subtitle);
        header.setLeft(brand);

        stockNotice = new Button();
        stockNotice.getStyleClass().add("stock-notice");
        stockNotice.setOnAction(event -> showLowStockProducts());
        header.setRight(stockNotice);
        return header;
    }

    private Node buildCheckoutTab() {
        HBox workspace = new HBox(18);
        workspace.setPadding(new Insets(20, 0, 0, 0));
        workspace.getStyleClass().add("workspace");

        VBox catalog = card("Product catalog", "Search or scan a product code to add items to this bill");
        checkoutSearch = new TextField();
        checkoutSearch.setPromptText("Search product name or scan code…");
        checkoutTable = buildCheckoutTable();
        quantitySpinner = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 9999, 1));
        quantitySpinner.setEditable(true);
        quantitySpinner.setPrefWidth(100);
        Label qtyLabel = new Label("Quantity");
        qtyLabel.getStyleClass().add("field-label");
        Button addToCart = primaryButton("Add to bill");
        addToCart.setOnAction(event -> addSelectedProduct());
        HBox addControls = new HBox(12, qtyLabel, quantitySpinner, addToCart);
        addControls.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(checkoutTable, Priority.ALWAYS);
        catalog.setMinWidth(0);
        catalog.setPrefWidth(700);
        HBox.setHgrow(catalog, Priority.ALWAYS);
        catalog.getChildren().addAll(checkoutSearch, checkoutTable, addControls);

        VBox bill = card("Current bill", "Review the cart, discounts, and amount due");
        bill.setMinWidth(0);
        bill.setPrefWidth(470);
        customerField = new TextField("Walk-in Customer");
        customerField.setPromptText("Customer name");
        cartTable = buildCartTable();
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        subtotalLabel = new Label(money(0));
        discountLabel = new Label("−" + money(0));
        totalLabel = new Label(money(0));
        totalLabel.getStyleClass().add("total-amount");
        VBox totals = new VBox(8,
                totalRow("Subtotal", subtotalLabel, false),
                totalRow("Discounts", discountLabel, false),
                totalRow("Amount due", totalLabel, true));
        totals.getStyleClass().add("totals-panel");

        Button remove = secondaryButton("Remove item");
        remove.setOnAction(event -> removeSelectedItem());
        Button clear = secondaryButton("Clear bill");
        clear.setOnAction(event -> {
            billingService.clearCart();
            refreshViews();
        });
        HBox cartActions = new HBox(8, remove, clear);
        cartActions.setAlignment(Pos.CENTER_LEFT);
        Button checkout = primaryButton("Complete sale  →");
        checkout.getStyleClass().add("checkout-button");
        checkout.setOnAction(event -> completeSale());
        VBox billFooter = new VBox(12, totals, cartActions, checkout);
        bill.getChildren().addAll(customerField, cartTable, billFooter);
        HBox.setHgrow(bill, Priority.ALWAYS);

        checkoutSearch.textProperty().addListener((observable, oldValue, newValue) -> filterCheckoutProducts());
        checkoutSearch.setOnAction(event -> {
            if (!checkoutTable.getItems().isEmpty()) {
                checkoutTable.getSelectionModel().selectFirst();
                addSelectedProduct();
            }
        });
        checkoutTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) addSelectedProduct();
        });
        workspace.getChildren().addAll(catalog, bill);
        return workspace;
    }

    private Node buildInventoryTab() {
        VBox inventory = new VBox(14);
        inventory.setPadding(new Insets(20, 0, 0, 0));
        inventory.getStyleClass().add("workspace");

        BorderPane titleRow = new BorderPane();
        VBox heading = new VBox(3);
        Label title = new Label("Inventory management");
        title.getStyleClass().add("section-title");
        Label description = new Label("Add products, edit details, remove listings, or restock low inventory");
        description.getStyleClass().add("section-subtitle");
        heading.getChildren().addAll(title, description);
        titleRow.setLeft(heading);
        Button add = primaryButton("＋ Add product");
        add.setOnAction(event -> addProduct());
        titleRow.setRight(add);

        VBox tableCard = new VBox(12);
        tableCard.getStyleClass().add("panel");
        inventorySearch = new TextField();
        inventorySearch.setPromptText("Search product name or code…");
        inventoryTable = buildInventoryTable();
        VBox.setVgrow(inventoryTable, Priority.ALWAYS);

        Label stockHint = new Label("On-hand stock changes through completed sales and restocking.");
        stockHint.getStyleClass().add("muted-label");
        Button edit = secondaryButton("Edit product");
        edit.setOnAction(event -> editSelectedProduct());
        Button delete = secondaryButton("Delete");
        delete.setOnAction(event -> deleteSelectedProduct());
        Button restock = primaryButton("Restock");
        restock.setOnAction(event -> restockSelectedProduct());
        HBox actions = new HBox(10, stockHint, spacer(), edit, delete, restock);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(stockHint, Priority.ALWAYS);
        tableCard.getChildren().addAll(inventorySearch, inventoryTable, actions);
        VBox.setVgrow(tableCard, Priority.ALWAYS);

        inventorySearch.textProperty().addListener((observable, oldValue, newValue) -> filterInventoryProducts());
        inventoryTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) editSelectedProduct();
        });
        inventory.getChildren().addAll(titleRow, tableCard);
        return inventory;
    }

    private TableView<Product> buildCheckoutTable() {
        TableView<Product> table = new TableView<>(checkoutProducts);
        table.getStyleClass().add("data-table");
        table.setPlaceholder(new Label("No matching products"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Product, String> code = column("Code", Product::getCode);
        TableColumn<Product, String> name = column("Product", Product::getName);
        TableColumn<Product, String> price = column("Price", product -> money(product.getUnitPrice()));
        TableColumn<Product, Integer> available = column("Available", billingService::getAvailableStock);
        TableColumn<Product, String> expiry = column("Expiry", this::expiryText);
        code.setPrefWidth(94);
        name.setPrefWidth(190);
        price.setPrefWidth(92);
        available.setPrefWidth(88);
        expiry.setPrefWidth(110);
        price.getStyleClass().add("numeric-column");
        available.getStyleClass().add("numeric-column");
        table.getColumns().add(code);
        table.getColumns().add(name);
        table.getColumns().add(price);
        table.getColumns().add(available);
        table.getColumns().add(expiry);
        addLowStockStyling(table, billingService::getAvailableStock);
        return table;
    }

    private TableView<Product> buildInventoryTable() {
        TableView<Product> table = new TableView<>(inventoryProducts);
        table.getStyleClass().add("data-table");
        table.setPlaceholder(new Label("No products in inventory"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Product, String> code = column("Code", Product::getCode);
        TableColumn<Product, String> name = column("Product", Product::getName);
        TableColumn<Product, String> category = column("Type", Product::getCategory);
        TableColumn<Product, String> price = column("Unit price", product -> money(product.getUnitPrice()));
        TableColumn<Product, Integer> stock = column("On hand", Product::getStock);
        TableColumn<Product, String> expiry = column("Expiry date", this::expiryText);
        code.setPrefWidth(120);
        name.setPrefWidth(340);
        category.setPrefWidth(140);
        price.setPrefWidth(130);
        stock.setPrefWidth(120);
        expiry.setPrefWidth(170);
        price.getStyleClass().add("numeric-column");
        stock.getStyleClass().add("numeric-column");
        table.getColumns().add(code);
        table.getColumns().add(name);
        table.getColumns().add(category);
        table.getColumns().add(price);
        table.getColumns().add(stock);
        table.getColumns().add(expiry);
        addLowStockStyling(table, Product::getStock);
        return table;
    }

    private TableView<CartItem> buildCartTable() {
        TableView<CartItem> table = new TableView<>(cartItems);
        table.getStyleClass().add("data-table");
        table.setPlaceholder(new Label("The bill is empty. Add products to begin."));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<CartItem, String> product = column("Product", item -> item.getProduct().getName());
        TableColumn<CartItem, Integer> quantity = column("Qty", CartItem::getQuantity);
        TableColumn<CartItem, String> price = column("Price", item -> money(item.getProduct().getUnitPrice()));
        TableColumn<CartItem, String> discount = column("Saved", item -> {
            double saved = billingService.getDiscount(item);
            return saved == 0 ? "—" : "−" + money(saved);
        });
        TableColumn<CartItem, String> total = column("Total", item -> money(
                item.getGrossAmount() - billingService.getDiscount(item)));
        product.setPrefWidth(190);
        quantity.setPrefWidth(55);
        price.setPrefWidth(90);
        discount.setPrefWidth(95);
        total.setPrefWidth(100);
        table.getColumns().add(product);
        table.getColumns().add(quantity);
        table.getColumns().add(price);
        table.getColumns().add(discount);
        table.getColumns().add(total);
        return table;
    }

    private <S, T> TableColumn<S, T> column(String title, Function<S, T> value) {
        TableColumn<S, T> column = new TableColumn<>(title);
        column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(value.apply(cell.getValue())));
        return column;
    }

    private <T> void addLowStockStyling(TableView<T> table, ToIntFunction<T> stockValue) {
        table.setRowFactory(view -> new TableRow<T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                pseudoClassStateChanged(LOW_STOCK,
                        !empty && item != null && stockValue.applyAsInt(item) <= 5);
            }
        });
    }

    private VBox card(String title, String subtitle) {
        VBox panel = new VBox(14);
        panel.getStyleClass().add("panel");
        VBox heading = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("section-subtitle");
        heading.getChildren().addAll(titleLabel, subtitleLabel);
        panel.getChildren().add(heading);
        return panel;
    }

    private HBox totalRow(String labelText, Label amount, boolean isTotal) {
        Label label = new Label(labelText);
        label.getStyleClass().add(isTotal ? "total-caption" : "muted-label");
        Region push = spacer();
        HBox row = new HBox(8, label, push, amount);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(push, Priority.ALWAYS);
        if (isTotal) row.getStyleClass().add("total-row");
        return row;
    }

    private Button primaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("button-primary");
        return button;
    }

    private Button secondaryButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("button-secondary");
        return button;
    }

    private Region spacer() { return new Region(); }

    private void filterCheckoutProducts() {
        String query = checkoutSearch.getText().trim().toLowerCase(Locale.ROOT);
        List<Product> matches = billingService.getProducts().stream()
                .filter(product -> matches(product, query))
                .collect(Collectors.toList());
        checkoutProducts.setAll(matches);
        if (!matches.isEmpty() && checkoutTable.getSelectionModel().getSelectedItem() == null) {
            checkoutTable.getSelectionModel().selectFirst();
        }
    }

    private void filterInventoryProducts() {
        String query = inventorySearch.getText().trim().toLowerCase(Locale.ROOT);
        inventoryProducts.setAll(billingService.getProducts().stream()
                .filter(product -> matches(product, query))
                .collect(Collectors.toList()));
    }

    private boolean matches(Product product, String query) {
        return product.getName().toLowerCase(Locale.ROOT).contains(query)
                || product.getCode().toLowerCase(Locale.ROOT).contains(query);
    }

    private void refreshViews() {
        filterCheckoutProducts();
        filterInventoryProducts();
        cartItems.setAll(billingService.getCartItems());
        checkoutTable.refresh();
        inventoryTable.refresh();
        subtotalLabel.setText(money(billingService.getSubtotal()));
        discountLabel.setText("−" + money(billingService.getDiscountTotal()));
        totalLabel.setText(money(billingService.getTotal()));

        int lowStockCount = billingService.getLowStockCount();
        stockNotice.setText(lowStockCount == 0
                ? "✓  Stock levels healthy"
                : "⚠  Low stock · " + lowStockCount + " product(s)  ·  Review");
        stockNotice.getStyleClass().removeAll("notice-healthy", "notice-warning");
        stockNotice.getStyleClass().add(lowStockCount == 0 ? "notice-healthy" : "notice-warning");
    }

    private void addSelectedProduct() {
        Product product = checkoutTable.getSelectionModel().getSelectedItem();
        if (product == null) {
            showAlert(Alert.AlertType.INFORMATION, "Choose a product", "Select a product in the catalog first.");
            return;
        }
        try {
            billingService.addToCart(product, quantitySpinner.getValue());
            refreshViews();
            checkoutSearch.requestFocus();
            checkoutSearch.selectAll();
        } catch (IllegalArgumentException exception) {
            showAlert(Alert.AlertType.WARNING, "Could not add product", exception.getMessage());
        }
    }

    private void removeSelectedItem() {
        CartItem item = cartTable.getSelectionModel().getSelectedItem();
        if (item == null) {
            showAlert(Alert.AlertType.INFORMATION, "Choose an item", "Select a bill item to remove.");
            return;
        }
        billingService.removeFromCart(item.getProduct().getCode());
        refreshViews();
    }

    private void completeSale() {
        try {
            Bill completed = billingService.checkout(customerField.getText());
            refreshViews();
            showReceipt(completed);
        } catch (IllegalStateException | SQLException exception) {
            showAlert(Alert.AlertType.WARNING, "Sale could not be completed", exception.getMessage());
        }
    }

    private void addProduct() {
        Product product = showProductEditor(null);
        if (product == null) return;
        try {
            billingService.createProduct(product);
            refreshViews();
            showAlert(Alert.AlertType.INFORMATION, "Inventory updated", "Product added to the catalog.");
        } catch (IllegalArgumentException | SQLException exception) {
            showAlert(Alert.AlertType.WARNING, "Could not add product", exception.getMessage());
        }
    }

    private void editSelectedProduct() {
        Product selected = inventoryTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.INFORMATION, "Choose a product", "Select a product in the inventory first.");
            return;
        }
        Product updated = showProductEditor(selected);
        if (updated == null) return;
        try {
            billingService.updateProduct(updated);
            refreshViews();
            showAlert(Alert.AlertType.INFORMATION, "Inventory updated", "Product details saved.");
        } catch (IllegalArgumentException | SQLException exception) {
            showAlert(Alert.AlertType.WARNING, "Could not update product", exception.getMessage());
        }
    }

    private Product showProductEditor(Product existing) {
        boolean isNew = existing == null;
        Dialog<Product> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle(isNew ? "Add product" : "Edit product");
        dialog.setHeaderText(isNew ? "Add a product to the inventory" : "Update product details");
        dialog.getDialogPane().getStyleClass().add("app-dialog");

        ButtonType saveType = new ButtonType(isNew ? "Add product" : "Save changes", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        TextField code = new TextField(isNew ? "" : existing.getCode());
        code.setPromptText("Unique product code");
        code.setEditable(isNew);
        TextField name = new TextField(isNew ? "" : existing.getName());
        name.setPromptText("Product name");
        ComboBox<String> category = new ComboBox<>(FXCollections.observableArrayList("Regular", "Perishable"));
        category.setValue(isNew || "REGULAR".equals(existing.getCategory().toUpperCase(Locale.ROOT))
                ? "Regular" : "Perishable");
        TextField price = new TextField(isNew ? "" : String.format(Locale.ROOT, "%.2f", existing.getUnitPrice()));
        price.setPromptText("0.00");
        DatePicker expiry = new DatePicker(isNew ? null : existing.getExpiryDate());
        expiry.setPromptText("Optional expiry date");
        Spinner<Integer> stock = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                0, Integer.MAX_VALUE, isNew ? 0 : existing.getStock()));
        stock.setEditable(true);
        if (!isNew) stock.setDisable(true);

        category.valueProperty().addListener((observable, previous, selected) -> {
            boolean perishable = "Perishable".equals(selected);
            expiry.setDisable(!perishable);
            if (!perishable) expiry.setValue(null);
        });
        expiry.setDisable(!"Perishable".equals(category.getValue()));

        GridPane form = new GridPane();
        form.setHgap(14);
        form.setVgap(12);
        form.setPadding(new Insets(8, 4, 8, 4));
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(115);
        ColumnConstraints fieldColumn = new ColumnConstraints();
        fieldColumn.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labelColumn, fieldColumn);
        addFormField(form, 0, "Product code", code);
        addFormField(form, 1, "Product name", name);
        addFormField(form, 2, "Product type", category);
        addFormField(form, 3, "Unit price (₹)", price);
        addFormField(form, 4, "Expiry date", expiry);
        addFormField(form, 5, isNew ? "Opening stock" : "Current stock", stock);
        Label hint = new Label(isNew
                ? "Perishable expiry is optional. Leave it blank if unknown."
                : "Product code is fixed; use Restock to add stock.");
        hint.getStyleClass().add("muted-label");
        form.add(hint, 0, 6, 2, 1);
        dialog.getDialogPane().setContent(form);
        dialog.getDialogPane().setMinWidth(510);

        Product[] valueToSave = new Product[1];
        Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            try {
                String codeText = code.getText().trim();
                String nameText = name.getText().trim();
                if (codeText.isEmpty() || codeText.length() > 24) {
                    throw new IllegalArgumentException("Product code must contain 1 to 24 characters.");
                }
                if (nameText.isEmpty() || nameText.length() > 120) {
                    throw new IllegalArgumentException("Product name must contain 1 to 120 characters.");
                }
                BigDecimal amount = new BigDecimal(price.getText().trim()).setScale(2, RoundingMode.UNNECESSARY);
                if (amount.signum() < 0 || amount.compareTo(new BigDecimal("99999999.99")) > 0) {
                    throw new IllegalArgumentException("Unit price must be between ₹0.00 and ₹99,999,999.99.");
                }
                boolean perishable = "Perishable".equals(category.getValue());
                LocalDate expiryDate = perishable ? expiry.getValue() : null;
                int stockValue = isNew ? stock.getValue() : existing.getStock();
                valueToSave[0] = perishable
                        ? new PerishableProduct(codeText, nameText, amount.doubleValue(), stockValue, expiryDate)
                        : new RegularProduct(codeText, nameText, amount.doubleValue(), stockValue);
            } catch (NumberFormatException | ArithmeticException exception) {
                event.consume();
                showAlert(Alert.AlertType.WARNING, "Check the unit price",
                        "Enter a valid amount with up to two decimal places.");
            } catch (IllegalArgumentException exception) {
                event.consume();
                showAlert(Alert.AlertType.WARNING, "Check product details", exception.getMessage());
            }
        });
        dialog.setResultConverter(button -> button == saveType ? valueToSave[0] : null);
        return dialog.showAndWait().orElse(null);
    }

    private void addFormField(GridPane grid, int row, String labelText, Node field) {
        Label label = new Label(labelText);
        label.getStyleClass().add("field-label");
        grid.add(label, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
    }

    private void deleteSelectedProduct() {
        Product product = inventoryTable.getSelectionModel().getSelectedItem();
        if (product == null) {
            showAlert(Alert.AlertType.INFORMATION, "Choose a product", "Select a product in the inventory first.");
            return;
        }
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(stage);
        confirmation.setTitle("Delete product");
        confirmation.setHeaderText("Delete “" + product.getName() + "”?");
        confirmation.setContentText("Existing receipts will remain saved.");
        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        try {
            billingService.deleteProduct(product.getCode());
            refreshViews();
        } catch (IllegalArgumentException | SQLException exception) {
            showAlert(Alert.AlertType.WARNING, "Could not delete product", exception.getMessage());
        }
    }

    private void restockSelectedProduct() {
        Product product = inventoryTable.getSelectionModel().getSelectedItem();
        if (product == null) {
            showAlert(Alert.AlertType.INFORMATION, "Choose a product", "Select a product in the inventory first.");
            return;
        }
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog("10");
        dialog.initOwner(stage);
        dialog.setTitle("Restock product");
        dialog.setHeaderText("Restock “" + product.getName() + "”");
        dialog.setContentText("Units to add:");
        Optional<String> value = dialog.showAndWait();
        if (value.isEmpty()) return;
        try {
            int amount = Integer.parseInt(value.get().trim());
            billingService.restockProduct(product.getCode(), amount);
            refreshViews();
            showAlert(Alert.AlertType.INFORMATION, "Restock complete",
                    product.getName() + " now has " + product.getStock() + " units on hand.");
        } catch (NumberFormatException exception) {
            showAlert(Alert.AlertType.WARNING, "Invalid quantity", "Enter a whole number greater than zero.");
        } catch (IllegalArgumentException | SQLException exception) {
            showAlert(Alert.AlertType.WARNING, "Could not restock product", exception.getMessage());
        }
    }

    private void showLowStockProducts() {
        List<Product> lowStock = billingService.getLowStockProducts();
        tabs.getSelectionModel().select(inventoryTab);
        if (lowStock.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "Stock levels", "All products are above the 5-unit threshold.");
            return;
        }
        StringBuilder message = new StringBuilder("Products at or below 5 available units:\n\n");
        for (Product product : lowStock) {
            message.append(product.getCode()).append("  ·  ").append(product.getName())
                    .append("  —  ").append(billingService.getAvailableStock(product)).append(" available\n");
        }
        message.append("\nSelect a product and choose Restock in Inventory.");
        showAlert(Alert.AlertType.WARNING, "Low-stock notification", message.toString());
    }

    private void showReceipt(Bill bill) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(stage);
        dialog.setTitle("Sale complete · " + bill.getBillNumber());
        dialog.setHeaderText("Sale completed successfully");
        ButtonType saveType = new ButtonType("Save receipt…", ButtonBar.ButtonData.LEFT);
        dialog.getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CLOSE);
        TextArea receipt = new TextArea(bill.toReceiptText());
        receipt.setEditable(false);
        receipt.setWrapText(false);
        receipt.getStyleClass().add("receipt-area");
        dialog.getDialogPane().setContent(receipt);
        dialog.getDialogPane().setMinWidth(520);
        dialog.getDialogPane().setMinHeight(500);
        dialog.setResultConverter(button -> button);
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == saveType) saveReceipt(bill);
    }

    private void saveReceipt(Bill bill) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save digital receipt");
        chooser.setInitialFileName("receipt-" + bill.getBillNumber() + ".txt");
        java.io.File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        try {
            Files.writeString(file.toPath(), bill.toReceiptText(), StandardCharsets.UTF_8);
            showAlert(Alert.AlertType.INFORMATION, "Receipt saved", "The digital receipt was saved successfully.");
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Could not save receipt", exception.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        if (stage != null) alert.initOwner(stage);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String expiryText(Product product) {
        return product.getExpiryDate() == null ? "—" : product.getExpiryDate().format(EXPIRY_FORMAT);
    }

    private static String money(double amount) {
        return String.format(Locale.forLanguageTag("en-IN"), "₹%,.2f", amount);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
