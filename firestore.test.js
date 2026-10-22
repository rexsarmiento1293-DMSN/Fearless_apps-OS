const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
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

test("Unauthenticated user: cannot read or create vault_dockets", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("vault_dockets").get());
  await assertFails(
    unauthDb.collection("vault_dockets").doc("doc_1").set({
      userId: ALICE_UID,
      title: "Test",
    })
  );
});

test("Authenticated user: can create and query own vault_dockets, cannot read Bob's", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("vault_dockets").doc("docket_alice_1").set({
      userId: ALICE_UID,
      title: "Barangay Blotter Escalation",
      caseSummary: "Unacted blotter in Bacoor barangay hall over boundary dispute.",
      moduleTag: "MODULE_01",
      phase: "GENESIS",
      planeStatus: "MORTAL",
      rulingText: "Request CFA after 15 days under RA 7160.",
      sealHash: "#VAULT1293-SECURE-A1B2C3",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Query with matching userId filter succeeds
  await assertSucceeds(
    aliceDb.collection("vault_dockets").where("userId", "==", ALICE_UID).get()
  );

  // Query without userId filter fails
  await assertFails(aliceDb.collection("vault_dockets").get());

  // Cross-user read fails
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("vault_dockets").doc("docket_alice_1").get());
});

test("Shadow Update & Invalid Enum: rejected by vault_dockets rules", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("vault_dockets").doc("docket_ghost").set({
      userId: ALICE_UID,
      title: "Ghost Test",
      caseSummary: "Summary",
      moduleTag: "MODULE_01",
      phase: "GENESIS",
      planeStatus: "MORTAL",
      rulingText: "Ruling",
      sealHash: "#VAULT1293-SECURE-999",
      isVerifiedAdmin: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Invalid phase enum rejected
  await assertFails(
    aliceDb.collection("vault_dockets").doc("docket_bad_enum").set({
      userId: ALICE_UID,
      title: "Bad Enum Test",
      caseSummary: "Summary",
      moduleTag: "MODULE_01",
      phase: "INVALID_PHASE",
      planeStatus: "MORTAL",
      rulingText: "Ruling",
      sealHash: "#VAULT1293-SECURE-999",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("ChatMessage & BioscienceAudit: owner can create and query, updates forbidden", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("chat_messages").doc("msg_1").set({
      userId: ALICE_UID,
      sender: "VAULT",
      moduleTag: "MODULE_07",
      phase: "EXECUTION",
      planeStatus: "IMMORTAL",
      content: "Ang ruling: I-reklamo sa NPC at SEC ang OLA harassment.",
      modelUsed: "gemini-4-argon",
      sealHash: "#VAULT1293-SECURE-777",
      groundingSources: "RA 10173 Data Privacy Act",
      createdAt: now,
    })
  );

  // Update is forbidden on append-only chat_messages
  await assertFails(
    aliceDb.collection("chat_messages").doc("msg_1").update({
      content: "Tampered content",
    })
  );

  await assertSucceeds(
    aliceDb.collection("bioscience_audits").doc("bio_1").set({
      userId: ALICE_UID,
      sampleName: "Bacoor Zapote River Runoff",
      bioscienceCategory: "Water Quality & Toxicology",
      findings: "Elevated coliform and heavy metal particulates detected.",
      regulatoryCitation: "RA 9275 Philippine Clean Water Act of 2004",
      riskLevel: "HIGH",
      sealHash: "#VAULT1293-SECURE-BIO1",
      createdAt: now,
    })
  );
});
