const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_noor_123";
const BOB_UID = "bob_noor_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastTimestamp() {
  return new Date(Date.now() - 1000);
}

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

test("Unauthenticated user cannot read or write user profile or subcollections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("namaz_logs")
      .get()
  );
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("ai_messages")
      .add({
        userId: ALICE_UID,
        sender: "USER",
        text: "Salam",
        createdAt: pastTimestamp(),
      })
  );
});

test("Authenticated user can create and update their own UserProfile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const profileRef = aliceDb.collection("users").doc(ALICE_UID);
  const ts = pastTimestamp();

  await assertSucceeds(
    profileRef.set({
      userId: ALICE_UID,
      displayName: "Boss",
      cityName: "Mymensingh",
      latitude: 24.7471,
      longitude: 90.4203,
      lastReadSurah: 1,
      lastReadAyah: 1,
      lastReadSurahName: "Al-Fatiha",
      azanEnabled: true,
      prePrayerAlertEnabled: true,
      nightLockEnabled: true,
      pornBlockEnabled: true,
      uninstallLockEnabled: true,
      dailyPhoneLimitMinutes: 120,
      focusModeUntilEpochMs: 0,
      activeLockMode: "NONE",
      requiredPushUps: 0,
      activeLockUntilEpochMs: 0,
      femaleVoiceEnabled: true,
      namazAutoSilentEnabled: true,
      ffDailyLimitMinutes: 60,
      ffGuardEnabled: true,
      gamblingBlockEnabled: true,
      sosPhoneNumber: "01700000000",
      defensePlanDayBits: 0,
      lockTriggerReason: "",
      familyRole: "BOSS",
      isPeriodMode: false,
      periodDayNumber: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    profileRef.update({
      lastReadSurah: 18,
      lastReadAyah: 10,
      lastReadSurahName: "Al-Kahf",
      familyRole: "BON",
      isPeriodMode: true,
      periodDayNumber: 3,
      defensePlanDayBits: 5,
      ffDailyLimitMinutes: 45,
      lockTriggerReason: "FF Time Ses",
      updatedAt: pastTimestamp(),
    })
  );

  await assertFails(
    profileRef.update({
      familyRole: "INVALID_ROLE",
      updatedAt: pastTimestamp(),
    })
  );

  await assertFails(
    profileRef.update({
      defensePlanDayBits: 200,
      updatedAt: pastTimestamp(),
    })
  );
});

test("Shadow update with ghost field or cross-user access fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = pastTimestamp();

  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      cityName: "Mymensingh",
      latitude: 24.7471,
      longitude: 90.4203,
      isAdmin: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      cityName: "Mymensingh",
      latitude: 24.7471,
      longitude: 90.4203,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("namaz_logs")
      .doc("2026-11-15")
      .set({
        userId: BOB_UID,
        dateKey: "2026-11-15",
        fajr: true,
        dhuhr: false,
        asr: false,
        maghrib: false,
        isha: false,
        createdAt: ts,
        updatedAt: ts,
      })
  );
});

test("Owner can CRUD namaz_logs, ai_messages, reminders, and app_limits", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userDoc = aliceDb.collection("users").doc(ALICE_UID);
  const ts = pastTimestamp();

  await assertSucceeds(
    userDoc.collection("namaz_logs").doc("2026-11-15").set({
      userId: ALICE_UID,
      dateKey: "2026-11-15",
      fajr: true,
      dhuhr: true,
      asr: false,
      maghrib: false,
      isha: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    userDoc.collection("namaz_logs").doc("2026-11-15").update({
      asr: true,
      updatedAt: pastTimestamp(),
    })
  );

  await assertSucceeds(
    userDoc.collection("ai_messages").doc("msg_1").set({
      userId: ALICE_UID,
      sender: "USER",
      text: "Namaz er somoy kokhon?",
      createdAt: ts,
    })
  );

  await assertSucceeds(
    userDoc.collection("reminders").doc("rem_1").set({
      userId: ALICE_UID,
      title: "Boss, Sokal 6 Tay Uthen",
      hour: 6,
      minute: 0,
      enabled: true,
      repeatDaily: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    userDoc.collection("app_limits").doc("lim_fb").set({
      userId: ALICE_UID,
      packageName: "com.facebook.katana",
      appName: "Facebook",
      dailyLimitMinutes: 30,
      enabled: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );
});
