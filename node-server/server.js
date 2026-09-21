const express = require("express");
const { initializeApp, cert } = require("firebase-admin/app");
const { getMessaging } = require("firebase-admin/messaging");

const serviceAccount = require("./service-account.json");

initializeApp({
  credential: cert(serviceAccount),
});

const app = express();
app.use(express.json({ limit: "256kb" }));

app.get("/", (req, res) => {
  res.json({
    status: "FCM Tester Pro sender running",
    endpoints: ["POST /send", "POST /send-topic"],
  });
});

app.post("/send", async (req, res) => {
  try {
    const {
      token,
      title = "FCM Tester Pro",
      body = "Hello from Node.js",
      data = {},
      imageUrl,
      deepLink,
      customSound = false,
      clickAction,
      dataOnly = false,
      silent = false,
      ttlSeconds = 3600,
      collapseKey,
    } = req.body;

    if (!token) {
      return res.status(400).json({ success: false, error: "token is required" });
    }

    const stringData = stringifyData(data);

    if (deepLink) {
      stringData.deep_link = String(deepLink);
    }

    if (customSound) {
      stringData.sound = "custom";
    }

    if (silent) {
      stringData.silent = "true";
    }

    const message = {
      token: String(token),
      data: stringData,
      android: {
        priority: "high",
        ttl: Math.max(0, Number(ttlSeconds) || 0) * 1000,
      },
    };

    if (!dataOnly) {
      message.notification = {
        title: String(title),
        body: String(body),
      };

      if (imageUrl) {
        message.notification.imageUrl = String(imageUrl);
      }

      message.android.notification = {
        channelId: customSound ? "fcm_tester_custom" : "fcm_tester",
      };

      if (customSound) {
        message.android.notification.sound = "fcm_test";
      }

      if (clickAction) {
        message.android.notification.clickAction = String(clickAction);
      }

      if (imageUrl) {
        message.android.notification.imageUrl = String(imageUrl);
      }
    } else {
      // Data-only messages need title/body in data if you want this tester
      // to create its own local notification.
      if (!silent) {
        if (!stringData.title) stringData.title = String(title);
        if (!stringData.body) stringData.body = String(body);
      }
    }

    if (collapseKey) {
      message.android.collapseKey = String(collapseKey);
    }

    const response = await getMessaging().send(message);
    res.json({ success: true, messageId: response, message });
  } catch (error) {
    console.error(error);
    res.status(500).json({
      success: false,
      code: error.code || null,
      error: error.message || String(error),
    });
  }
});

app.post("/send-topic", async (req, res) => {
  try {
    const {
      topic,
      title = "Topic test",
      body = "Hello topic subscribers",
      data = {},
      imageUrl,
      customSound = false,
      dataOnly = false,
      silent = false,
    } = req.body;

    if (!topic) {
      return res.status(400).json({ success: false, error: "topic is required" });
    }

    const cleanTopic = String(topic).replace(/^\/topics\//, "");
    const stringData = stringifyData(data);
    if (customSound) stringData.sound = "custom";
    if (silent) stringData.silent = "true";

    const message = {
      topic: cleanTopic,
      data: stringData,
      android: { priority: "high" },
    };

    if (!dataOnly) {
      message.notification = {
        title: String(title),
        body: String(body),
      };
      if (imageUrl) message.notification.imageUrl = String(imageUrl);

      message.android.notification = {
        channelId: customSound ? "fcm_tester_custom" : "fcm_tester",
      };
      if (customSound) message.android.notification.sound = "fcm_test";
      if (imageUrl) message.android.notification.imageUrl = String(imageUrl);
    } else if (!silent) {
      if (!stringData.title) stringData.title = String(title);
      if (!stringData.body) stringData.body = String(body);
    }

    const response = await getMessaging().send(message);
    res.json({ success: true, messageId: response, message });
  } catch (error) {
    console.error(error);
    res.status(500).json({
      success: false,
      code: error.code || null,
      error: error.message || String(error),
    });
  }
});

function stringifyData(input) {
  const out = {};
  for (const [key, value] of Object.entries(input || {})) {
    if (value === undefined || value === null) continue;
    out[String(key)] = typeof value === "string" ? value : JSON.stringify(value);
  }
  return out;
}

const port = Number(process.env.PORT || 3000);
app.listen(port, () => {
  console.log(`FCM Tester Pro sender: http://localhost:${port}`);
});
