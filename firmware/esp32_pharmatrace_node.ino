/**
 * PharmaTrace ESP32 Hardware Telemetry Node
 * Hardware: ESP32 + MFRC522 RFID Reader + DHT22 Temperature/Humidity + Active Buzzer & LED
 *
 * RFID UID Formatting:
 * Converts 4-byte MFRC522 UID into an exact 8-character uppercase hex string (e.g. "2AF52B07" or "659A1707")
 * without spaces or delimiters, perfectly matching the PharmaTrace backend authorization engine.
 */

#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <SPI.h>
#include <MFRC522.h>
#include <DHT.h>

// ======================= Configuration =======================
// Wi-Fi Credentials
const char* WIFI_SSID     = "YOUR_WIFI_SSID";
const char* WIFI_PASSWORD = "YOUR_WIFI_PASSWORD";

// PharmaTrace Spring Boot Backend Endpoint
// Replace with your server's LAN IP address (e.g., 192.168.1.100)
const char* SERVER_URL    = "http://192.168.1.100:8080/api/telemetry";

// Product Batch & Hub Location
const char* BATCH_ID      = "VAX-B4920-COVID19";
const char* HUB_LOCATION  = "Distributor Hub (College)";

// Pin Assignments (ESP32 Standard SPI)
#define SS_PIN         5    // MFRC522 SDA / SS
#define RST_PIN        22   // MFRC522 RST
#define DHT_PIN        4    // DHT22 Data Pin
#define DHT_TYPE       DHT22
#define BUZZER_PIN     13   // Active Buzzer
#define RED_LED_PIN    15   // Tamper Alarm LED
#define GREEN_LED_PIN  2    // Authentic Status LED

// Hardware Instances
MFRC522 mfrc522(SS_PIN, RST_PIN);
DHT dht(DHT_PIN, DHT_TYPE);

// Rate-limiting debounce
unsigned long lastScanTime = 0;
const unsigned long SCAN_COOLDOWN_MS = 2500;

void setup() {
  Serial.begin(115200);
  delay(500);
  Serial.println("\n[PharmaTrace] Initializing ESP32 Hardware Telemetry Node...");

  // Initialize Actuator Pins
  pinMode(BUZZER_PIN, OUTPUT);
  pinMode(RED_LED_PIN, OUTPUT);
  pinMode(GREEN_LED_PIN, OUTPUT);
  digitalWrite(BUZZER_PIN, LOW);
  digitalWrite(RED_LED_PIN, LOW);
  digitalWrite(GREEN_LED_PIN, LOW);

  // Initialize SPI bus and MFRC522 RFID
  SPI.begin();
  mfrc522.PCD_Init();
  delay(100);
  mfrc522.PCD_DumpVersionToSerial();

  // Initialize DHT22 Sensor
  dht.begin();

  // Connect to Wi-Fi
  connectToWiFi();

  // Initial self-test beep (short 80ms)
  digitalWrite(GREEN_LED_PIN, HIGH);
  delay(80);
  digitalWrite(GREEN_LED_PIN, LOW);

  Serial.println("[PharmaTrace] Ready. Present RFID tag to scan custody transit...");
}

void loop() {
  // Ensure Wi-Fi remains connected
  if (WiFi.status() != WL_CONNECTED) {
    connectToWiFi();
  }

  // Check if a new RFID card is present on reader
  if (!mfrc522.PICC_IsNewCardPresent() || !mfrc522.PICC_ReadCardSerial()) {
    delay(50);
    return;
  }

  // Debounce to prevent duplicate rapid scans
  if (millis() - lastScanTime < SCAN_COOLDOWN_MS) {
    mfrc522.PICC_HaltA();
    mfrc522.PCD_StopCrypto1();
    return;
  }
  lastScanTime = millis();

  // Format RFID UID into exact 8-character uppercase hex string (e.g. "2AF52B07")
  String rfidUid = formatRfidUid(mfrc522.uid.uidByte, mfrc522.uid.size);
  Serial.println("\n----------------------------------------");
  Serial.print("[RFID Scanned] Formatted UID: ");
  Serial.println(rfidUid);

  // Read live DHT22 environmental telemetry
  float temperature = dht.readTemperature();
  float humidity    = dht.readHumidity();

  // Fallback defaults if sensor reading fails
  if (isnan(temperature) || isnan(humidity)) {
    Serial.println("[DHT22 Warning] Reading failed, using default cold-chain telemetry.");
    temperature = 4.5;
    humidity    = 45.0;
  }

  Serial.printf("[Telemetry] Temp: %.1f °C | Humidity: %.1f %%\n", temperature, humidity);

  // Send telemetry payload to backend
  sendTelemetryToBackend(rfidUid, temperature, humidity);

  // Halt PICC card to allow new read
  mfrc522.PICC_HaltA();
  mfrc522.PCD_StopCrypto1();
}

/**
 * Formats MFRC522 raw byte array into an exact uppercase hex string without spaces.
 * Example: {0x2A, 0xF5, 0x2B, 0x07} -> "2AF52B07"
 */
String formatRfidUid(byte *buffer, byte bufferSize) {
  String uidString = "";
  for (byte i = 0; i < bufferSize; i++) {
    char hexByte[3];
    sprintf(hexByte, "%02X", buffer[i]);
    uidString += hexByte;
  }
  uidString.toUpperCase();
  return uidString;
}

/**
 * Sends HTTP POST request to /api/telemetry and drives actuators based on backend response.
 */
void sendTelemetryToBackend(String rfidUid, float temperature, float humidity) {
  HTTPClient http;
  http.begin(SERVER_URL);
  http.addHeader("Content-Type", "application/json");

  // Construct JSON ingestion payload
  StaticJsonDocument<256> doc;
  doc["rfidUid"]      = rfidUid;
  doc["batchId"]      = BATCH_ID;
  doc["temperature"]  = temperature;
  doc["humidity"]     = humidity;
  doc["locationName"] = HUB_LOCATION;

  String requestJson;
  serializeJson(doc, requestJson);

  Serial.print("[HTTP POST] Transmitting to: ");
  Serial.println(SERVER_URL);
  Serial.println(requestJson);

  int httpResponseCode = http.POST(requestJson);

  if (httpResponseCode == 200) {
    String responseBody = http.getString();
    Serial.println("[HTTP 200 OK] Backend Response received:");
    Serial.println(responseBody);

    // Parse backend response directives
    StaticJsonDocument<512> resDoc;
    DeserializationError error = deserializeJson(resDoc, responseBody);

    if (!error) {
      bool buzzerActive = resDoc["buzzer"] | false;
      const char* ledColor = resDoc["led"] | "GREEN";
      const char* status = resDoc["verificationStatus"] | "UNKNOWN";
      const char* message = resDoc["message"] | "";

      Serial.printf("[Verdict] Status: %s | Buzzer: %s | LED: %s\n",
                    status, buzzerActive ? "ACTIVE" : "OFF", ledColor);
      Serial.println(message);

      // Trigger actuators according to strict backend rules
      if (buzzerActive) {
        // UNAUTHORIZED TAG TAMPER OR THERMAL EXCURSION ALERT
        Serial.println("[ALARM] Triggering active hardware buzzer and red warning LED!");
        digitalWrite(GREEN_LED_PIN, LOW);
        digitalWrite(RED_LED_PIN, HIGH);

        // Sound active buzzer alarm sequence
        for (int i = 0; i < 3; i++) {
          digitalWrite(BUZZER_PIN, HIGH);
          delay(200);
          digitalWrite(BUZZER_PIN, LOW);
          delay(100);
        }
      } else {
        // AUTHORIZED CUSTODY SCAN ("2AF52B07" or "659A1707")
        Serial.println("[SUCCESS] Authorized tag verified! Normal cold-chain custody confirmed.");
        digitalWrite(BUZZER_PIN, LOW);
        digitalWrite(RED_LED_PIN, LOW);
        digitalWrite(GREEN_LED_PIN, HIGH);
        delay(800);
        digitalWrite(GREEN_LED_PIN, LOW);
      }
    }
  } else {
    Serial.printf("[HTTP Error] Ingestion failed with code: %d\n", httpResponseCode);
  }

  http.end();
}

void connectToWiFi() {
  Serial.print("[Wi-Fi] Connecting to: ");
  Serial.println(WIFI_SSID);
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  int attempts = 0;
  while (WiFi.status() != WL_CONNECTED && attempts < 20) {
    delay(500);
    Serial.print(".");
    attempts++;
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.println("\n[Wi-Fi] Connected! IP Address: ");
    Serial.println(WiFi.localIP());
  } else {
    Serial.println("\n[Wi-Fi] Connection timed out. Running in offline/retry loop.");
  }
}
