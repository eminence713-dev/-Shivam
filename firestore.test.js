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

test("Unauthenticated user: denied reading products, settings, and orders", async () => {
  const unauth = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauth.collection("products").where("isPublished", "==", true).get());
  await assertFails(unauth.collection("store_settings").doc("public_config").get());
  await assertFails(unauth.collection("orders").get());
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

test("Verified owner can create published & hidden products; customers only read published", async () => {
  const admin = ownerDb();
  const now = new Date(Date.now() - 1000);
  await assertSucceeds(
    admin.collection("products").doc("pub_saree").set({
      nameEn: "Banarasi Silk Saree",
      category: "SAREES",
      sellingPriceInr: 1450,
      originalPriceInr: 1800,
      stockCount: 8,
      lowStockThreshold: 2,
      sizes: ["Free Size"],
      colors: ["Maroon", "Gold"],
      isPublished: true,
      isFeatured: true,
      isDemo: true,
      soldCount: 0,
      createdBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertSucceeds(
    admin.collection("products").doc("draft_shirt").set({
      nameEn: "Draft Cotton Shirt",
      category: "SHIRTS",
      sellingPriceInr: 599,
      originalPriceInr: 0,
      stockCount: 5,
      lowStockThreshold: 2,
      sizes: ["M", "L"],
      colors: ["Cream"],
      isPublished: false,
      isFeatured: false,
      isDemo: false,
      soldCount: 0,
      createdBy: OWNER_UID,
      createdAt: now,
      updatedAt: now,
    })
  );

  const custA = customerDb(CUSTOMER_A_UID);
  await assertSucceeds(custA.collection("products").where("isPublished", "==", true).get());
  await assertFails(custA.collection("products").get());
  await assertFails(custA.collection("products").doc("draft_shirt").get());
  await assertFails(
    custA.collection("products").doc("pub_saree").update({ sellingPriceInr: 10 })
  );
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

test("Atomic customer order creation + stock deduction succeeds; cross-customer PII read fails", async () => {
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

  // Customer A can query their own orders
  await assertSucceeds(
    custA.collection("orders").where("customerId", "==", CUSTOMER_A_UID).get()
  );

  // Customer B CANNOT read Customer A's order or list unfiltered orders
  const custB = customerDb(CUSTOMER_B_UID);
  await assertFails(custB.collection("orders").doc("ord_1001").get());
  await assertFails(custB.collection("orders").get());
  await assertFails(custB.collection("product_costs").get());
  await assertFails(custB.collection("offline_sales").get());
});
