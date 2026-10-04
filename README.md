# FreshMart Supermarket Billing System

A JavaFX supermarket point-of-sale demo with checkout and inventory screens. Product details, stock, optional expiry dates, completed bills, and bill lines are stored in MySQL.

## Requirements

- JDK 21 or later
- MySQL Server 8.0 or later, running locally
- MySQL Workbench (recommended for loading the schema)
- Apache Maven 3.9 or later
- Internet access the first time Maven downloads JavaFX, MySQL Connector/J, and Maven plugins

## Set up MySQL

1. Start MySQL Server and connect to it in MySQL Workbench.
2. In Workbench, open `database/schema.sql` and run the full script. It creates the `supermarket_billing` database, its tables, and the initial product catalog.
   If you already created the database with an earlier version of this project, run `database/migrate_optional_expiry.sql` once so perishable products can have a blank expiry date.
3. Copy `config/db.properties.example` to `config/db.properties`.
4. Edit `config/db.properties` and set `db.user` and `db.password` to the account you use in Workbench. `db.url` normally stays as provided for a local MySQL server.

`config/db.properties` is ignored by Git so local credentials are not committed. You can also provide `DB_URL`, `DB_USER`, or `DB_PASSWORD` environment variables; they override values in the config file.

## Run on Windows

Open Command Prompt in this folder and run:

```bat
run.bat
```

Or run directly with Maven:

```bat
mvn javafx:run
```

The first run downloads JavaFX modules and platform libraries, MySQL Connector/J, and Maven plugins. Maven manages JavaFX, so a separate JavaFX SDK installation is not required. The UI uses JavaFX 21.0.12 to match the JDK 21 baseline.

## Demo flow

1. In **Inventory**, add products, search/view the catalog, edit product details, delete products, and restock stock quantities.
2. Use the optional expiry date picker for perishable products; leave it blank when the date is unknown. Regular products do not use expiry dates.
3. In **Checkout**, search a product name or type/scan its code, choose a quantity, and add it to the bill. Available stock reflects the cart reservation immediately.
4. The application calculates near-expiry and multi-buy discounts. Products with 5 or fewer available units appear in the low-stock alert; use **Restock** in Inventory.
5. Complete the sale to show a receipt. The bill, bill lines, and reduced stock are committed to MySQL in one transaction. Removing an item or clearing the cart releases its reserved quantity.

The scanner field accepts keyboard input, which is how many USB barcode scanners behave. No physical scanner is required for the demo.

## OOP concepts for the presentation

- **Classes and objects:** `Product`, `PerishableProduct`, `RegularProduct`, `CartItem`, `Bill`, and `BillingService` model the checkout.
- **Inheritance:** `PerishableProduct` and `RegularProduct` extend `Product`.
- **Polymorphism:** `BillingService` evaluates `DiscountPolicy` objects without depending on their concrete discount class.
- **Encapsulation:** Product fields are private, stock is reduced through `reduceStock`, and checkout operations are coordinated by `BillingService`.
- **Abstraction:** `Product`, `DiscountPolicy`, and `ProductRepository` define common behavior; concrete product types, discount rules, and `MySqlProductRepository` provide the implementations.

## Architecture

```text
Cashier
   |
   v
JavaFX UI (SupermarketBillingApp)
   |
   v
BillingService (cart, discounts, checkout)
   |
   v
ProductRepository interface
   |
   v
MySqlProductRepository (JDBC)
   |
   +--> products: load catalog and update stock
   +--> bills and bill_items: save completed sales
```

The SQL operations for stock reduction and bill creation run in one transaction. If any stock update or insert fails, the transaction is rolled back and the cart remains available for correction.
