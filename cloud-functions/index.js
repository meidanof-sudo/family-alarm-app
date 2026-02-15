const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

const db = admin.firestore();
const messaging = admin.messaging();

/**
 * Triggered when a new alarm document is created in the "alarms" collection.
 * Reads the family group, collects all FCM tokens (except the sender's),
 * and sends a high-priority data message to each device.
 *
 * The Android app's AlarmFCMService receives this data message and starts
 * the AlarmSoundService, which plays the alarm sound using USAGE_ALARM
 * audio attributes — this bypasses Do Not Disturb and silent mode.
 */
exports.onAlarmCreated = functions.firestore
  .document("alarms/{alarmId}")
  .onCreate(async (snap) => {
    const alarm = snap.data();
    const { familyCode, senderName, senderUid } = alarm;

    if (!familyCode) {
      console.error("No familyCode in alarm document");
      return;
    }

    // Get the family document
    const familyDoc = await db.collection("families").doc(familyCode).get();
    if (!familyDoc.exists) {
      console.error(`Family ${familyCode} not found`);
      return;
    }

    const family = familyDoc.data();
    const members = family.members || [];

    // Collect FCM tokens of all members except the sender
    const tokens = members
      .filter((m) => m.uid !== senderUid && m.fcmToken)
      .map((m) => m.fcmToken);

    if (tokens.length === 0) {
      console.log("No recipients to notify");
      return;
    }

    // Send high-priority data-only message (no notification payload).
    // Data messages are always delivered to the app's onMessageReceived handler,
    // even if the app is in the background. The "priority: high" ensures
    // the message wakes the device from doze mode.
    const message = {
      data: {
        type: "family_alarm",
        senderName: senderName || "Family Member",
        familyCode: familyCode,
        timestamp: Date.now().toString(),
      },
      android: {
        priority: "high",
        ttl: 60 * 1000, // 1 minute TTL
      },
    };

    // Send to each token individually to handle errors per-device
    const sendPromises = tokens.map(async (token) => {
      try {
        await messaging.send({ ...message, token });
        console.log(`Sent alarm to token: ${token.substring(0, 10)}...`);
      } catch (error) {
        console.error(`Failed to send to ${token.substring(0, 10)}...`, error);
        // If token is invalid, remove it from the family
        if (
          error.code === "messaging/invalid-registration-token" ||
          error.code === "messaging/registration-token-not-registered"
        ) {
          const memberToRemove = members.find((m) => m.fcmToken === token);
          if (memberToRemove) {
            await db
              .collection("families")
              .doc(familyCode)
              .update({
                members: admin.firestore.FieldValue.arrayRemove(memberToRemove),
              });
          }
        }
      }
    });

    await Promise.all(sendPromises);
    console.log(`Alarm sent to ${tokens.length} devices for family ${familyCode}`);
  });
