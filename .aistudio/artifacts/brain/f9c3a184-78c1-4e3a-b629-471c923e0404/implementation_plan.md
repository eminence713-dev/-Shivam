# Shri Shivam Readymade & Riya Fancy Shoes Mart — Online Store & Owner Suite

A complete, mobile-first native Android store and shop-management application for **Shri Shivam Readymade & Riya Fancy Shoes Mart** (Bada Bazar, Kalyanpur, Motihari, Bihar), enabling local families to browse clothing and footwear in English or Hindi, place verified orders or WhatsApp enquiries, and allowing owner Raju Kumar Gupta to manage inventory, online/offline billing, expenses, and reports directly from a phone.

## User Review & Critical Decisions

> [!IMPORTANT]
> Please review the confirmed choices below—including how this AI Studio Android workspace adapts the PWA brief into a native installable Android app with zero-trust Firebase security—and click **Proceed** when ready to provision Firebase and build.

- **Confirmed — Platform & Runtime Architecture**: Because this workspace targets **native Android (Kotlin & Jetpack Compose)** rather than a browser runtime, the store will be built as a responsive, installable Android application (`.apk` / `.aab`) rather than a web-only PWA. All functional requirements—customer storefront, 10 categories, cart, checkout, WhatsApp ordering, UPI QR & COD handling, printable PDF invoices, sales/expense reports, and owner setup documentation—are preserved in full without claiming a web service worker where an APK is produced.
- **Confirmed — Owner Dashboard Authorization**: Owner privileges are granted strictly via verified Google Sign-In (`email_verified == true`) to **`rajuguptas123@gmail.com`** and your active testing account (**`eminence713@gmail.com`**), enforced directly inside Cloud Firestore security rules rather than client-side UI hiding.
- **Confirmed — Initial Policies & Payment Posture**: Delivery charges and return/exchange policies start clearly marked **"Awaiting owner confirmation — contact shop for details"**, with **Cash on Delivery (COD)** and **WhatsApp Ordering** active immediately. **UPI QR** stays in a safe "Awaiting verified merchant UPI setup" state until the owner saves a real UPI ID in Store Settings (never inventing a UPI ID or auto-marking payments as paid).
- **Confirmed — Bilingual Experience**: Opens in **English** by default with a persistent one-tap **हिंदी / EN** switch in the top bar that translates navigation, categories, buttons, checkout validation messages, order statuses, and owner tools instantaneously.
- **Platform Requirement — Frictionless One-Tap Google Sign-In**: In this environment's hardened Firebase configuration, all Cloud Firestore reads and writes require Google Sign-In via Android Credential Manager (anonymous/unauthenticated database access is disabled for zero-trust isolation). Customers sign in with a single Google tap—no account forms or passwords—and proceed straight to streamlined checkout where delivery details are entered per order, while their order tracking remains strictly isolated from other shoppers.

---

## 1. Overview & Core Concept

- **What It Does**: Combines a welcoming neighbourhood clothing & footwear storefront across 10 curated categories with a full mobile Point-of-Sale (POS) and back-office dashboard for the shop owner—unifying online customer orders, walk-in counter billing, stock tracking, expense logging, and profit reporting in one place.
- **Target Audience / Persona**:
  - **Local Shoppers**: Families and residents in Kalyanpur, Motihari, and nearby villages browsing sarees, kurtis, shirts, and family footwear on budget-to-midrange Android phones over mobile data.
  - **Shop Owner (Raju Kumar Gupta)**: Non-technical retail owner operating product listings, photo updates, stock adjustments, customer bills, and daily accounts entirely on a smartphone.
- **Key Value**: Builds genuine local trust through transparent `₹` pricing, clear "Demo Sample" labelling on prototype items, honest delivery/payment disclosures, atomic stock protection, and instant WhatsApp support (`+91 9162685162`).

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Welcome & Storefront Browsing**: After a one-tap Google Sign-In welcome card featuring the store crest and address, shoppers land on the **Home** screen with a persistent `हिंदी / EN` toggle, search bar, owner-configured offer banner, horizontal **10-Category** strip, Featured items, New Arrivals, Popular products (computed from real order/sale counts once data exists), transparent Delivery & Payment info cards, and a floating/prominent **Order or Ask on WhatsApp** action.
2. **Category Filtering, Product Detail & Cart**: Tapping any category (Sarees, Kurtis, Ladies Frocks, Shirts, T-shirts, Jeans, Trousers, Shoes, Sandals, Slippers) or searching opens an adaptive product grid with Price and Newest sorting and clear availability badges. Product Detail displays real/sample imagery (explicitly badged **"Demo Product"** until owner-published), `₹` selling price, optional original MRP, size/colour chips, stock-capped quantity selector, **Add to Cart**, **Buy Now**, and **Order on WhatsApp**.
3. **Checkout, Payment & Safe Order Tracking**: Cart validates live stock, displays item subtotals, shows configured delivery fee (or "Awaiting owner confirmation"), and collects only **Customer Name**, **10-digit Mobile Number**, **Delivery Address**, optional **Landmark/Instructions**, and **Payment Method** (COD or verified UPI QR). Order confirmation appears **only after** an atomic Firestore transaction commits stock and generates a unique `SSR-...` reference. UPI orders display a scannable QR code + `upi://pay` intent button with recipient verification reminders and stay `Pending Verification` until the owner confirms receipt. Customers track status safely in **My Orders** without ever seeing another customer's data.
4. **Owner Admin Dashboard (Mobile-First)**: Authorized owner accounts see an **Owner Mode** switch unlocking:
   - **Overview & Alerts**: Today/range revenue, pending order count, low-stock alerts, and large touch-friendly action tiles.
   - **Manage Products & Stock**: Add/edit/publish/unpublish/delete items, pick phone camera/gallery photos via Android's zero-permission Photo Picker (auto-compressed safely), set selling/original/purchase cost prices, hide demo products in one tap, and adjust stock with automatic movement logs.
   - **Order Fulfilment**: Advance orders through `Pending → Confirmed → Packed → Out for Delivery → Delivered` or `Cancelled` (with atomic restock when previously deducted) and mark UPI/COD payments verified.
   - **Offline Counter Sale & Print Bill**: Record walk-in shop sales (deducting stock atomically without double-counting), generate unique `INV-...` customer invoices, and print or share clean PDF bills via Android's native print service.
   - **Daily Sales, Profit & Expenses**: Log business expenses, view date-filtered breakdowns of Gross Revenue, Discounts, Cancelled/Refunded amounts, Expenses, and true **Gross Profit** (computed strictly where purchase cost is recorded, never mislabelling revenue as profit), plus CSV export and an interactive **Setup & Deployment Guide**.

### Visual Identity & Theme

- **Aesthetic Direction**: **Heritage Indian Retail Editorial**—warm, dignified, festive yet uncluttered. Evokes a trusted family clothing & footwear showroom in Bihar with subtle geometric jharokha arch headers and fine gold hairline dividers, free of fake countdown timers, popups, or marketplace noise.
- **Color Palette & Mood** (Material 3 semantic tokens):
  - `primary`: **Royal Sindoor Maroon** (`#6E1423`) with crisp cream `onPrimary` (`#FFFDF9`) for primary CTAs (`Shop Now`, `Place Order`, `Print Bill`) and brand headers.
  - `background` & `surface`: **Warm Handloom Ivory** (`#FAF6F0`) and **Soft Parchment Cream** (`#F3ECE1`) for effortless readability in bright daylight.
  - `secondary` / `tertiary`: **Muted Antique Gold** (`#B6862C`) and **Warm Terracotta** (`#9C412D`) for category medallions, subtle ornamental borders, and active navigation pills, paired with **WhatsApp Emerald** (`#1F7A4C`) exclusively for verified WhatsApp actions.
  - `onBackground` / `onSurface`: **Deep Espresso Ink** (`#23191B`) and **Muted Rosewood Grey** (`#5C4B4E`) ensuring high WCAG contrast.
- **Typography & Hierarchy**: Locally bundled Google Fonts—pairing **Rozha One** (or **Playfair Display**) for stately display headings across both Devanagari (`श्री शिवम...`) and Latin scripts with **Noto Sans Devanagari** / **Plus Jakarta Sans** for crisp tabular `₹` numerals, form labels, and dense admin tables. All text uses `sp` units for system font-scale accessibility.
- **Component Styling & Adaptive Layout**:
  - Edge-to-edge layout (`enableEdgeToEdge()`) respecting status-bar and gesture navigation insets.
  - Minimum `48.dp` interactive targets across all chips, quantity steppers, and owner controls.
  - Responsive canonical switching: bottom `NavigationBar` + 2-column grid on handheld phones; side `NavigationRail` + multi-column / list-detail split panes on tablets and foldables.
- **Interactive Feedback & Motion**: Gentle `<250ms` spring transitions between storefront tabs, tactile Material ripples, in-flight button spinners preventing duplicate order taps, clear empty-state illustrations with reset buttons, and confirmation dialogs before destructive actions (deleting products or cancelling orders).

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native Android App + Built-In PDF Printing over Browser-Only PWA**
  - *Chosen Approach*: Deliver a full Jetpack Compose Android application backed by Firebase Auth and Cloud Firestore, using Android's native `PdfDocument` / `PrintManager` for invoices and `Intent.ACTION_VIEW` for WhatsApp (`https://wa.me/919162685162`) and UPI deep links.
  - *Why*: Matches the active Android build environment, runs natively on the owner's and customers' phones as an installable APK, and avoids fabricating web service-worker claims. An honest **Setup & Hosting Guide** inside the dashboard documents APK sharing, Play Store distribution, and how web hosting/domains work if a companion web storefront is deployed later.
- **Decision 2: Atomic Firestore Transactions for Stock, Orders & Walk-In Sales**
  - *Chosen Approach*: Execute checkout order placement, order cancellation restocking, manual stock edits, and offline shop sales inside Cloud Firestore transactions (`runTransaction`), recording an `InventoryMovement` audit entry alongside every stock delta.
  - *Why*: Guarantees prices and available stock are verified against live server state, prevents overselling when two shoppers buy the last item, blocks duplicate stock deductions or double-restocks via an explicit `stockDeducted` state flag, and ensures online orders are never double-counted as offline sales.
- **Decision 3: Honest Product Photo Handling Without Unprovisioned Cloud Storage**
  - *Chosen Approach*: Combine custom-generated category sample illustrations (clearly flagged on demo items) with an owner Photo Picker workflow that downscales and compresses real phone photos into lightweight thumbnails stored safely within Firestore document size limits, plus support for external HTTPS image URLs and explicit Firebase Storage setup instructions.
  - *Why*: Automated provisioning in this environment covers Firestore and Google Auth only (not Firebase Cloud Storage). Compressing photos keeps real phone uploads genuinely persistent across devices right away while transparently guiding the owner on configuring full-resolution Cloud Storage later.
- **Decision 4: Verified-Recipient UPI QR & Strict Accounting Separation**
  - *Chosen Approach*: Generate a standard NPCI `upi://pay` URI and scannable QR code on-device **only** when the owner configures a valid UPI ID (`pa`) and Payee Name (`pn`), keeping payment status `Pending` until owner verification. Calculate Gross Profit strictly on line items that carry a recorded `costPrice`.
  - *Why*: Eliminates fake payment confirmations, protects customers with explicit recipient-verification prompts, and prevents misleading financial reports that confuse gross revenue with profit.

---

## 4. Technical Architecture & Data Strategy

### Architecture & Component Diagram

```
┌────────────────────────────────────────────────────────────────────────────┐
│                   PRESENTATION LAYER (Jetpack Compose + M3)                │
│  ┌──────────────────────────────────┐  ┌────────────────────────────────┐  │
│  │       Customer Storefront        │  │    Owner Admin Suite (RBAC)    │  │
│  │ • Home (Offers, 10 Categories)   │  │ • Summary & Low-Stock Alerts   │  │
│  │ • Search & Category Grid         │  │ • Product & Photo Editor       │  │
│  │ • Product Detail (Sizes/Colours) │  │ • Stock Adjuster & Audit Log   │  │
│  │ • Cart & Validated Checkout      │  │ • Order & Payment Verification │  │
│  │ • UPI QR / COD / WhatsApp Sheet  │  │ • Offline POS & PDF Bill Print │  │
│  │ • My Orders (Customer-Isolated)  │  │ • Sales, Profit & Expense Hub  │  │
│  └─────────────────┬────────────────┘  │ • Store Settings & Setup Guide │  │
│                    │                   └───────────────┬────────────────┘  │
│                    └─────────────────┬─────────────────┘                   │
│                         Bilingual UI State (EN / हिं)                      │
└──────────────────────────────────────┼─────────────────────────────────────┘
                                       ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                     VIEWMODEL & DOMAIN VALIDATION LAYER                    │
│  • AuthViewModel (Credential Manager Google Sign-In & Owner Role Check)    │
│  • StorefrontViewModel (Catalog Flows, Cart Math, Checkout Idempotency)    │
│  • AdminViewModel (Product CRUD, Order Lifecycle, POS, Profit Analytics)   │
│  • InvoicePrintHelper &QrCodeGenerator & WhatsAppLinkBuilder               │
└──────────────────────────────────────┬─────────────────────────────────────┘
                                       ▼
┌────────────────────────────────────────────────────────────────────────────┐
│              DATA & ZERO-TRUST SECURITY LAYER (Firebase SDK)               │
│  • StoreRepository (Named DB Instance + Structured Error Handling)         │
│  • Atomic Firestore Transactions (Stock Guard, Order Ref, Ledger Sync)     │
│  ┌──────────────────────────────────────────────────────────────────────┐  │
│  │ Cloud Firestore Collections (Protected by Hardened firestore.rules)  │  │
│  │  /store_settings/public_config  │  /products/{productId}             │  │
│  │  /orders/{orderId}              │  /offline_sales/{saleId}           │  │
│  │  /expenses/{expenseId}          │  /inventory_movements/{movementId} │  │
│  │  /admins/{uid}                                                       │  │
│  └──────────────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────────────────┘
```

### Data Model & Zero-Trust Security Strategy

- **`/store_settings/public_config`**: Store name, owner contact (`9162685162`, `rajuguptas123@gmail.com`), address, promotional banner, delivery coverage/fee/free-threshold/min-order, `policiesConfirmed` flag, return/exchange policy text, COD toggle, and optional verified merchant UPI ID & payee name.
  - *Access*: Readable by signed-in shoppers; writable **only** by verified owner accounts (`rajuguptas123@gmail.com`, `eminence713@gmail.com`, or `/admins/{uid}`).
- **`/products/{productId}`**: Title (EN + optional HI), category (strict enum of the 10 categories), description, `sellingPriceInr`, optional `originalPriceInr`, optional owner-entered `costPriceInr`, `stockCount`, `lowStockThreshold`, `sizes`, `colors`, future-ready `variants` list, `imageKey` / compressed `imageDataUri` / `imageUrl`, `isPublished`, `isFeatured`, `isDemo`, `soldCount`, and server timestamps.
  - *Access*: Customers can read published products (`isPublished == true`); owners can read/create/update/delete all products. During customer order placement, a narrow action-gated update rule permits atomic stock decrement (`stockCount` decreasing by ordered quantity and `soldCount` incrementing) only when paired with a valid order creation.
- **`/orders/{orderId}`**: Unique `orderReference`, `idempotencyKey`, `customerId` (`request.auth.uid`), customer name, 10-digit phone, address, optional landmark, bounded `items` snapshot list (product ID, name, qty, unit selling price, unit cost snapshot, selected size/colour), `subtotalInr`, `deliveryChargeInr`, `discountInr`, `totalPayableInr`, `paymentMethod` (`COD` or `UPI_QR`), `paymentStatus` (`PENDING`, `VERIFIED_PAID`, `REFUNDED`), `orderStatus` (`PENDING`, `CONFIRMED`, `PACKED`, `OUT_FOR_DELIVERY`, `DELIVERED`, `CANCELLED`), `stockDeducted` boolean, and timestamps.
  - *Access*: Customers can `create` orders only for their own `customerId == request.auth.uid` with strict schema/math bounds, and can `get`/`list` **only** documents where `resource.data.customerId == request.auth.uid` (preventing any cross-customer PII or order leakage). Only verified owners can transition `orderStatus` or `paymentStatus`.
- **`/offline_sales/{saleId}`, `/expenses/{expenseId}`, `/inventory_movements/{movementId}`**: Walk-in counter invoices (`INV-...`), categorized shop expenses, and stock delta audit logs (recording previous stock, new stock, delta, reason, and reference ID).
  - *Access*: Strictly owner-only (`isOwnerAdmin()`) for all reads and writes; completely inaccessible to regular customers.

### Interactive Component & State Mapping

- **Language Switch (`हिंदी / EN`)**: Updates `AppLanguage` state in `StorefrontViewModel`, immediately recomposing localized strings, category names, validation hints, and policy notices without losing cart or form inputs.
- **Catalog Search, Filter & Demo Seeding**: Real-time `snapshots()` flow combines search query, selected category chip, and sort mode. On first owner sign-in when the catalog is empty, the owner can seed clearly badged **Demo Sample Products** across all 10 categories for testing and unpublish/remove them with one tap when ready for live shop inventory.
- **Cart & Atomic Checkout Handler**: Validates quantities against live `stockCount` before enabling checkout; validates Indian 10-digit phone numbers and non-empty delivery fields with bilingual errors; locks the submit button during execution; runs a Firestore transaction that re-reads each product document, verifies `isPublished` and `stockCount >= qty`, computes server-trusted totals, deducts stock, logs inventory movements, and writes the order before navigating to Confirmation.
- **WhatsApp Click-to-Chat Builder**: Sanitizes configured shop phone into `919162685162`, encodes item names, quantities, selected size/colour, subtotal, delivery fee status, and customer's next step into `https://wa.me/...`, and displays a clear notice distinguishing a WhatsApp message from a saved store order.
- **Owner Order, POS, Invoice & Analytics Handlers**:
  - Status selector enforces safe stock lifecycle: cancelling an order where `stockDeducted == true` atomically restores each item's `stockCount`, flips `stockDeducted = false`, and writes a restock movement entry so stock can never be restored twice.
  - Offline POS builder records walk-in sales with unique `INV-...` numbers separately from online orders (preventing duplicate revenue counting) and invokes Android's `PrintManager` to print or save a formatted A4/thermal-friendly store invoice bearing the shop logo, GST/policy note, and line items.
  - Reports screen aggregates online non-cancelled orders + offline sales + expenses for the chosen date range, displaying Gross Revenue, Discounts, Cancelled/Refunded totals, Expenses, Low-Stock items, Category/Product leaders, and Gross Profit (strictly from cost-recorded items).
