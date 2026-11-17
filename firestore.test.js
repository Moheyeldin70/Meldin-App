const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ADMIN_UID = "admin_mohey";
const ADMIN_EMAIL = "moheyeldinemad9@gmail.com";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

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

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb
      .collection("posts")
      .where("type", "in", ["LONG_VIDEO", "SHORT_VIDEO", "IMAGE", "ARTICLE"])
      .get()
  );
  await assertFails(
    unauthDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      type: "ARTICLE",
      title: "Hack",
      description: "Nope",
      category: "Tech",
      likesCount: 0,
      commentsCount: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Email spoofing with unverified admin email is rejected", async () => {
  const spoofDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofDb.collection("posts").doc("post_spoof").set({
      authorId: "spoof_uid",
      authorName: "Spoofer",
      type: "ARTICLE",
      title: "Spoofed Post",
      description: "Should fail",
      category: "News",
      likesCount: 0,
      commentsCount: 0,
      createdAt: pastDate(),
      updatedAt: pastDate(),
    })
  );
});

test("Only verified admin can publish, edit, and delete platform posts", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();

  const created = pastDate(5000);
  // Non-admin cannot create a post
  await assertFails(
    aliceDb.collection("posts").doc("post_1").set({
      authorId: ALICE_UID,
      authorName: "Alice",
      type: "LONG_VIDEO",
      title: "Unauthorized Video",
      description: "Test",
      category: "Design",
      likesCount: 0,
      commentsCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Admin creates valid post (NO @DocumentId id field)
  await assertSucceeds(
    adminDb.collection("posts").doc("post_1").set({
      authorId: ADMIN_UID,
      authorName: "Meldin Admin",
      type: "LONG_VIDEO",
      title: "Architecture Deep Dive",
      description: "Designing calm software",
      mediaUrl: "https://example.com/video.mp4",
      category: "Engineering",
      durationSeconds: 720,
      languageTag: "en",
      likesCount: 0,
      commentsCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Reject ghost field / @DocumentId id field
  await assertFails(
    adminDb.collection("posts").doc("post_2").set({
      id: "post_2",
      authorId: ADMIN_UID,
      authorName: "Meldin Admin",
      type: "ARTICLE",
      title: "Bad ID",
      description: "Has id field",
      category: "Tech",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Non-admin can list with matching type filter
  await assertSucceeds(
    aliceDb
      .collection("posts")
      .where("type", "in", ["LONG_VIDEO", "SHORT_VIDEO", "IMAGE", "ARTICLE"])
      .get()
  );

  // Non-admin cannot edit post title or delete post
  await assertFails(
    aliceDb.collection("posts").doc("post_1").update({
      title: "Hijacked",
      updatedAt: pastDate(1000),
    })
  );
  await assertFails(aliceDb.collection("posts").doc("post_1").delete());

  // Admin can update post
  await assertSucceeds(
    adminDb.collection("posts").doc("post_1").update({
      title: "Architecture Deep Dive v2",
      updatedAt: pastDate(1000),
    })
  );
});

test("Users can comment on posts, edit/delete only their own comments, and update comment/like counts by 1", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv
    .authenticatedContext(ALICE_UID, {
      email: "alice@example.com",
      email_verified: true,
    })
    .firestore();
  const bobDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "bob@example.com",
      email_verified: true,
    })
    .firestore();

  const t0 = pastDate(10000);
  await assertSucceeds(
    adminDb.collection("posts").doc("post_1").set({
      authorId: ADMIN_UID,
      authorName: "Meldin Admin",
      type: "ARTICLE",
      title: "Welcome to Meldin",
      description: "First post",
      bodyText: "Full article content.",
      category: "Editorial",
      likesCount: 0,
      commentsCount: 0,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Alice adds a comment
  await assertSucceeds(
    aliceDb
      .collection("posts")
      .doc("post_1")
      .collection("comments")
      .doc("c_1")
      .set({
        postId: "post_1",
        userId: ALICE_UID,
        authorName: "Alice",
        text: "Great article!",
        createdAt: pastDate(4000),
        updatedAt: pastDate(4000),
      })
  );

  // Alice increments commentsCount by 1
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_1").update({
      commentsCount: 1,
      updatedAt: pastDate(3000),
    })
  );

  // Alice cannot jump commentsCount by +50
  await assertFails(
    aliceDb.collection("posts").doc("post_1").update({
      commentsCount: 50,
      updatedAt: pastDate(2000),
    })
  );

  // Query comments with matching postId filter
  await assertSucceeds(
    bobDb
      .collection("posts")
      .doc("post_1")
      .collection("comments")
      .where("postId", "==", "post_1")
      .get()
  );

  // Bob cannot edit or delete Alice's comment
  await assertFails(
    bobDb
      .collection("posts")
      .doc("post_1")
      .collection("comments")
      .doc("c_1")
      .update({ text: "Edited by Bob", updatedAt: pastDate(1000) })
  );
  await assertFails(
    bobDb
      .collection("posts")
      .doc("post_1")
      .collection("comments")
      .doc("c_1")
      .delete()
  );

  // Alice can delete her own comment
  await assertSucceeds(
    aliceDb
      .collection("posts")
      .doc("post_1")
      .collection("comments")
      .doc("c_1")
      .delete()
  );
});

test("Personal library (saved_posts) and Private Studio (private_items) are strictly owner-isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const t0 = pastDate(5000);

  // Alice saves a post
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("saved_posts")
      .doc("post_1")
      .set({
        postId: "post_1",
        userId: ALICE_UID,
        type: "IMAGE",
        title: "Sunset Study",
        description: "Warm tones",
        authorName: "Meldin Admin",
        category: "Photography",
        durationSeconds: 0,
        savedAt: t0,
      })
  );

  // Alice creates a private item
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("private_items")
      .doc("priv_1")
      .set({
        userId: ALICE_UID,
        type: "ARTICLE",
        title: "My Private Draft",
        description: "Personal notes",
        bodyText: "Confidential draft text.",
        category: "Journal",
        durationSeconds: 0,
        createdAt: t0,
        updatedAt: t0,
      })
  );

  // Alice can list her own saved posts & private items
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_posts").get()
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private_items").get()
  );

  // Bob cannot read or write Alice's library or private items
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_posts").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private_items").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("private_items")
      .doc("priv_1")
      .delete()
  );
});

test("Likes and private_settings enforce strict owner access and schema validation", async () => {
  const adminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const t0 = pastDate(5000);

  await assertSucceeds(
    adminDb.collection("posts").doc("post_1").set({
      authorId: ADMIN_UID,
      authorName: "Meldin Admin",
      type: "IMAGE",
      title: "Gallery Item",
      description: "Desc",
      category: "Art",
      likesCount: 0,
      commentsCount: 0,
      createdAt: t0,
      updatedAt: t0,
    })
  );

  // Alice likes post_1
  await assertSucceeds(
    aliceDb
      .collection("posts")
      .doc("post_1")
      .collection("likes")
      .doc(ALICE_UID)
      .set({
        userId: ALICE_UID,
        createdAt: pastDate(2000),
      })
  );

  // Bob cannot delete Alice's like
  await assertFails(
    bobDb
      .collection("posts")
      .doc("post_1")
      .collection("likes")
      .doc(ALICE_UID)
      .delete()
  );

  // Admin stores private_settings password hash
  const validHash = "a".repeat(64);
  await assertSucceeds(
    adminDb
      .collection("users")
      .doc(ADMIN_UID)
      .collection("private_settings")
      .doc("admin_vault")
      .set({
        userId: ADMIN_UID,
        passwordHash: validHash,
        salt: "0123456789abcdef0123456789abcdef",
        createdAt: t0,
        updatedAt: t0,
      })
  );

  // Alice cannot read admin's private_settings
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ADMIN_UID)
      .collection("private_settings")
      .doc("admin_vault")
      .get()
  );
});
