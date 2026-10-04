# FreshMart Supermarket Billing Demo

A small Java Swing checkout demo built from the supermarket billing project abstract. It uses sample in-memory data so it can be demonstrated without configuring a database.

## Run on Windows

Install Java JDK 11 or later, open Command Prompt in this folder, and run:

```bat
run.bat
```

Or compile and launch manually from PowerShell:

```powershell
New-Item -ItemType Directory -Force out
javac -encoding UTF-8 -d out (Get-ChildItem src -Filter *.java | ForEach-Object FullName)
java -cp out SupermarketBillingApp
```

## Demo flow

1. Search by product name or type/scan a sample product code, such as `200201`.
2. Select a product, set its quantity, and add it to the cart.
3. Watch the subtotal and eligible near-expiry or multi-buy discount update.
4. Complete the sale to see the receipt. The receipt can be saved as a text file.
5. Stock decreases after checkout, and the low-stock alert updates.

The scanner field accepts keyboard input, which is how many USB barcode scanners behave. No actual barcode hardware is required for the demo.

## OOP concepts for the presentation

- **Classes:** `Product`, `PerishableProduct`, `RegularProduct`, `CartItem`, `Bill`, and `BillingService`.
- **Objects:** Each sample product, cart line, discount policy, and completed bill is an object created while the program runs.
- **Inheritance:** `PerishableProduct` and `RegularProduct` extend the abstract `Product` class.
- **Polymorphism:** `BillingService` evaluates a list of `DiscountPolicy` objects; expiry and quantity policies provide their own calculation.
- **Encapsulation:** Product stock and model fields are private; stock changes through `reduceStock`, and cart operations go through `BillingService`.
- **Abstraction:** `Product` and `DiscountPolicy` describe common behavior while leaving details to their subclasses and implementations.

## Architecture

```text
Swing screens (SupermarketBillingApp)
                 |
                 v
     BillingService (cart, checkout, stock)
          |                    |
          v                    v
 Product / CartItem       DiscountPolicy
          |
          v
    Bill / BillLine / text receipt
```

The demo stores sample products in memory, so changes reset when the app closes. Database persistence, stored customer purchase history, sales analysis, and voice input are suitable future extensions for the full project.
