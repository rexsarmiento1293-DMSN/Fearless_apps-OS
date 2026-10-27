/**
 * ====================================================================================
 * LROS VR26.10 — I.A.M DIMENSION V.7 CLOUD FUNCTION (`verifyKaggleToken`)
 * Project ID : gen-lang-client-0597903520
 * Package ID : com.iamdimension.v7
 * Database ID: ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2
 * ====================================================================================
 */
const { onRequest } = require("firebase-functions/v2/https");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");

const app = initializeApp();

// Explicitly connect to the exact AI Studio Firestore Database ID (never the default instance)
const EXACT_DATABASE_ID = "ai-studio-android-iamsover-129d727b-49ce-41e6-b574-5705e44eced2";
const db = getFirestore(app, EXACT_DATABASE_ID);

exports.verifyKaggleToken = onRequest({ cors: true }, async (req, res) => {
  try {
    const payload = req.body?.data || req.body || {};
    const submittedToken = String(payload.token || "").trim();
    const expectedToken = process.env.KAGGLE_WAKE_UP_KEY || "";

    const isMatch = submittedToken.length > 0 && submittedToken === expectedToken;

    if (isMatch && payload.uid) {
      await db.collection("vault_dockets").doc(`black_case_${payload.uid}`).set(
        {
          userId: String(payload.uid),
          title: "BLACK CASE 100% CATCH BASIN OVERFLOW",
          caseSummary: "Owner authenticated via verifyKaggleToken Cloud Function.",
          moduleTag: "MODULE_04",
          phase: "IMMORTALIZED",
          planeStatus: "IMMORTAL",
          rulingText: "OWNER AUTHENTICATED • 100% Catch Basin Overflow Unlocked.",
          sealHash: "#VAULT1293-SECURE-BLACKCASE",
          createdAt: FieldValue.serverTimestamp(),
          updatedAt: FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    }

    return res.status(200).json({
      success: isMatch,
      result: {
        success: isMatch,
        databaseId: EXACT_DATABASE_ID,
      },
    });
  } catch (err) {
    return res.status(500).json({
      success: false,
      error: err.message || "Internal verification error",
    });
  }
});
