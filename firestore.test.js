const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const OWNER_UID = "owner_raju_1";
const CUSTOMER_A_UID = "cust_anita_1";
const CUSTOMER_B_UID = "cust_rahul_2";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function ownerDb() {
  return testEnv
    .authenticatedContext(OWNER_UID, {
      email: "rajuguptas123@gmail.com",
      email_verified: true,
    })
    .firestore();
}

function spoofedUnverifiedOwnerDb() {
  return testEnv
    .authenticatedContext("spoof_uid", {
      email: "rajuguptas123@gmail.com",
      email_verified: false,
    })
    .firestore();
}

function customerDb(uid) {
  return testEnv
    .authenticatedContext(uid, {
      email: `${uid}@gmail.com`,
      email_verified: true,
    })
    .firestore();
}

test("Unauthenticated customer: can read published catalog & public_config, denied orders & drafts & writes", async () => {
  const admin = ownerDb();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    admin.collection("products").doc("pub_shoe").set({
      nameEn: "Riya Men Casual Shoes",
      category: "SHOES",
      sellingPriceInr: 799,
      originalPriceInr: 999,
      stockCount: 10,
      lowStockThreshold: 2,
      sizes: ["7", "8", "9"],
      colors: ["Black"],
      isPublished: true,
      isFeatured: true,
      isDemo: false,
      soldCount: 0,
      createdBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    admin.collection("products").doc("draft_shoe").set({
      nameEn: "Hidden Draft Shoe",
      category: "SHOES",
      sellingPriceInr: 799,
      originalPriceInr: 999,
      stockCount: 5,
      lowStockThreshold: 2,
      sizes: ["8"],
      colors: ["Brown"],
      isPublished: false,
      isFeatured: false,
      isDemo: false,
      soldCount: 0,
      createdBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertSucceeds(unauth.collection("products").where("isPublished", "==", true).get());
  await assertSucceeds(unauth.collection("products").doc("pub_shoe").get());
  await assertSucceeds(unauth.collection("store_settings").doc("public_config").get());

  // Must reject unfiltered list, draft product read, catalog tamper, and order PII reads
  await assertFails(unauth.collection("products").get());
  await assertFails(unauth.collection("products").doc("draft_shoe").get());
  await assertFails(unauth.collection("products").doc("pub_shoe").update({ sellingPriceInr: 1 }));
  await assertFails(unauth.collection("orders").get());
  await assertFails(unauth.collection("product_costs").get());
});

test("Zero-login guest customer order: validated create succeeds, PII read denied to guests & other customers, allowed for owner", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  const orderTime = new Date(Date.now() - 500);

  await assertSucceeds(
    unauth.collection("orders").doc("ord_guest_1001").set({
      orderReference: "SSR-20261027-G101",
      customerId: "guest",
      customerName: "Sunil Kumar",
      customerPhone: "9123456789",
      deliveryAddress: "Bada Bazar, Near Block Road, Kalyanpur",
      landmark: "Main Chowk",
      items: [
        {
          productId: "pub_shoe",
          productName: "Riya Men Casual Shoes",
          category: "SHOES",
          quantity: 1,
          unitPriceInr: 799,
          lineTotalInr: 799,
          selectedSize: "8",
          selectedColor: "Black",
        },
      ],
      itemSummary: "Riya Men Casual Shoes (8) x1",
      totalQuantity: 1,
      subtotalInr: 799,
      deliveryChargeInr: 0,
      discountInr: 0,
      totalPayableInr: 799,
      deliveryFeeConfirmed: false,
      paymentMethod: "COD",
      paymentStatus: "PENDING",
      orderStatus: "PENDING",
      stockDeducted: false,
      cancellationReason: "",
      invoiceNumber: "",
      createdAt: orderTime,
      updatedAt: orderTime,
    })
  );

  // Guest cannot overwrite existing order, cannot set stockDeducted=true, and cannot read PII back
  await assertFails(
    unauth.collection("orders").doc("ord_guest_1001").update({ orderStatus: "CONFIRMED" })
  );
  await assertFails(unauth.collection("orders").doc("ord_guest_1001").get());

  // Even a signed-in non-owner customer CANNOT read or query 'guest' orders!
  const custB = customerDb(CUSTOMER_B_UID);
  await assertFails(custB.collection("orders").doc("ord_guest_1001").get());
  await assertFails(custB.collection("orders").where("customerId", "==", "guest").get());

  // Verified shop owner CAN read & list all orders including guest orders!
  const admin = ownerDb();
  await assertSucceeds(admin.collection("orders").doc("ord_guest_1001").get());
  await assertSucceeds(admin.collection("orders").get());
});

test("Email spoofing guard: unverified owner email cannot create product or read expenses", async () => {
  const db = spoofedUnverifiedOwnerDb();
  const now = new Date();
  await assertFails(
    db.collection("products").doc("prod_1").set({
      nameEn: "Silk Saree",
      category: "SAREES",
      sellingPriceInr: 1299,
      originalPriceInr: 1599,
      stockCount: 10,
      lowStockThreshold: 2,
      sizes: ["Free Size"],
      colors: ["Maroon"],
      isPublished: true,
      isFeatured: true,
      isDemo: false,
      soldCount: 0,
      createdBy: "spoof_uid",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(db.collection("expenses").get());
});

test("Shadow field rejection: owner write with unexpected ghost field fails", async () => {
  const admin = ownerDb();
  const now = new Date(Date.now() - 1000);
  await assertFails(
    admin.collection("expenses").doc("exp_1").set({
      title: "Shop Electricity",
      category: "ELECTRICITY_UTILITIES",
      amountInr: 950,
      recordedBy: OWNER_UID,
      expenseDateEpochDay: 20752,
      createdAt: now,
      ghostAdminFlag: true,
    })
  );
});

test("Signed-in customer atomic order creation + stock deduction succeeds; cross-customer PII read fails", async () => {
  const admin = ownerDb();
  const now = new Date(Date.now() - 2000);
  await assertSucceeds(
    admin.collection("products").doc("sandal_1").set({
      nameEn: "Riya Festive Wedge Sandals",
      category: "SANDALS",
      sellingPriceInr: 499,
      originalPriceInr: 650,
      stockCount: 6,
      lowStockThreshold: 2,
      sizes: ["6", "7"],
      colors: ["Gold"],
      isPublished: true,
      isFeatured: true,
      isDemo: true,
      soldCount: 0,
      createdBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  const custA = customerDb(CUSTOMER_A_UID);
  const batch = custA.batch();
  const orderRef = custA.collection("orders").doc("ord_1001");
  const prodRef = custA.collection("products").doc("sandal_1");
  const movRef = custA.collection("inventory_movements").doc("mov_1001");
  const orderTime = new Date(Date.now() - 500);

  batch.set(orderRef, {
    orderReference: "SSR-20261026-1001",
    customerId: CUSTOMER_A_UID,
    customerName: "Anita Devi",
    customerPhone: "9876543210",
    deliveryAddress: "Ward 4, Near Main Chowk, Kalyanpur",
    landmark: "Near Hanuman Mandir",
    items: [
      {
        productId: "sandal_1",
        productName: "Riya Festive Wedge Sandals",
        category: "SANDALS",
        quantity: 2,
        unitPriceInr: 499,
        lineTotalInr: 998,
        selectedSize: "6",
        selectedColor: "Gold",
      },
    ],
    itemSummary: "Riya Festive Wedge Sandals x2",
    totalQuantity: 2,
    subtotalInr: 998,
    deliveryChargeInr: 0,
    discountInr: 0,
    totalPayableInr: 998,
    deliveryFeeConfirmed: false,
    paymentMethod: "COD",
    paymentStatus: "PENDING",
    orderStatus: "PENDING",
    stockDeducted: true,
    createdAt: orderTime,
    updatedAt: orderTime,
  });

  batch.update(prodRef, {
    stockCount: 4,
    soldCount: 2,
    lastOrderId: "ord_1001",
    updatedAt: orderTime,
  });

  batch.set(movRef, {
    productId: "sandal_1",
    productName: "Riya Festive Wedge Sandals",
    previousStock: 6,
    newStock: 4,
    delta: -2,
    reason: "ONLINE_ORDER",
    referenceId: "ord_1001",
    actorId: CUSTOMER_A_UID,
    createdAt: orderTime,
  });

  await assertSucceeds(batch.commit());

  await assertSucceeds(
    custA.collection("orders").where("customerId", "==", CUSTOMER_A_UID).get()
  );

  const custB = customerDb(CUSTOMER_B_UID);
  await assertFails(custB.collection("orders").doc("ord_1001").get());
  await assertFails(custB.collection("orders").get());
});
