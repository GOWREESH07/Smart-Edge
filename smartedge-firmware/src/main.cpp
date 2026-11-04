#include <Arduino.h>
#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
#include <PZEM004Tv30.h>

#include "secrets.h"

// =====================================================
// PZEM CONFIGURATION
// =====================================================

HardwareSerial pzemSerial(2);

PZEM004Tv30 pzem(
    pzemSerial,
    16,
    17
);


// =====================================================
// SMARTEDGE SERVER
// =====================================================

const char* serverUrl =
    "http://10.136.170.48:8080/api/readings";


// =====================================================
// EDGE FEATURE STATE
// =====================================================

float previousPower = 0.0;
float previousCurrent = 0.0;
float previousPowerFactor = 0.0;

unsigned long previousReadingTime = 0;

bool hasPreviousReading = false;


// =====================================================
// BENCHMARK VARIABLES
// =====================================================

unsigned long featureStartTime = 0;
unsigned long featureEndTime = 0;

unsigned long jsonStartTime = 0;
unsigned long jsonEndTime = 0;

unsigned long httpStartTime = 0;
unsigned long httpEndTime = 0;


// =====================================================
// SENSOR VALIDATION LIMITS
// =====================================================

// These limits are intentionally conservative for the
// present SmartEdge prototype and laboratory setup.

const float MIN_VOLTAGE = 180.0;
const float MAX_VOLTAGE = 280.0;

const float MIN_CURRENT = 0.0;
const float MAX_CURRENT = 20.0;

const float MIN_POWER = 0.0;
const float MAX_POWER = 5000.0;

const float MIN_ENERGY = 0.0;

const float MIN_FREQUENCY = 45.0;
const float MAX_FREQUENCY = 55.0;

const float MIN_POWER_FACTOR = 0.0;
const float MAX_POWER_FACTOR = 1.0;


// =====================================================
// WIFI CONNECTION
// =====================================================

void connectWiFi() {

    WiFi.begin(
        WIFI_SSID,
        WIFI_PASSWORD
    );

    Serial.print("Connecting");

    while (WiFi.status() != WL_CONNECTED) {

        delay(500);

        Serial.print(".");
    }

    Serial.println();

    Serial.println("WiFi Connected!");

    Serial.print("ESP IP: ");

    Serial.println(
        WiFi.localIP()
    );
}


// =====================================================
// SENSOR VALIDATION RESULT
// =====================================================

struct SensorValidationResult {

    bool valid;

    String reason;
};


// =====================================================
// SENSOR VALIDATION
// =====================================================

SensorValidationResult validateSensorData(
    float voltage,
    float current,
    float power,
    float energy,
    float frequency,
    float powerFactor
) {

    // -------------------------------------------------
    // NaN / Infinity checks
    // -------------------------------------------------

    if (
        isnan(voltage) ||
        isnan(current) ||
        isnan(power) ||
        isnan(energy) ||
        isnan(frequency) ||
        isnan(powerFactor)
    ) {

        return {
            false,
            "NAN_VALUE"
        };
    }


    if (
        isinf(voltage) ||
        isinf(current) ||
        isinf(power) ||
        isinf(energy) ||
        isinf(frequency) ||
        isinf(powerFactor)
    ) {

        return {
            false,
            "INFINITE_VALUE"
        };
    }


    // -------------------------------------------------
    // Voltage validation
    // -------------------------------------------------

    if (
        voltage < MIN_VOLTAGE ||
        voltage > MAX_VOLTAGE
    ) {

        return {
            false,
            "VOLTAGE_OUT_OF_RANGE"
        };
    }


    // -------------------------------------------------
    // Current validation
    // -------------------------------------------------

    if (
        current < MIN_CURRENT ||
        current > MAX_CURRENT
    ) {

        return {
            false,
            "CURRENT_OUT_OF_RANGE"
        };
    }


    // -------------------------------------------------
    // Power validation
    // -------------------------------------------------

    if (
        power < MIN_POWER ||
        power > MAX_POWER
    ) {

        return {
            false,
            "POWER_OUT_OF_RANGE"
        };
    }


    // -------------------------------------------------
    // Energy validation
    // -------------------------------------------------

    if (energy < MIN_ENERGY) {

        return {
            false,
            "ENERGY_OUT_OF_RANGE"
        };
    }


    // -------------------------------------------------
    // Frequency validation
    // -------------------------------------------------

    if (
        frequency < MIN_FREQUENCY ||
        frequency > MAX_FREQUENCY
    ) {

        return {
            false,
            "FREQUENCY_OUT_OF_RANGE"
        };
    }


    // -------------------------------------------------
    // Power factor validation
    // -------------------------------------------------

    if (
        powerFactor < MIN_POWER_FACTOR ||
        powerFactor > MAX_POWER_FACTOR
    ) {

        return {
            false,
            "POWER_FACTOR_OUT_OF_RANGE"
        };
    }


    // -------------------------------------------------
    // Cross-parameter sanity check
    // -------------------------------------------------

    // For an AC load:
    //
    // P ≈ V × I × PF
    //
    // Small sensor/calculation differences are expected,
    // therefore a generous tolerance is used.

    float expectedPower =
        voltage *
        current *
        powerFactor;


    if (expectedPower > 1.0) {

        float powerRatio =
            power / expectedPower;

        if (
            powerRatio < 0.40 ||
            powerRatio > 1.60
        ) {

            return {
                false,
                "POWER_INCONSISTENT_WITH_V_I_PF"
            };
        }
    }


    // -------------------------------------------------
    // Reading is valid
    // -------------------------------------------------

    return {
        true,
        "VALID"
    };
}


// =====================================================
// SETUP
// =====================================================

void setup() {

    Serial.begin(115200);

    delay(1000);

    pzemSerial.begin(9600);

    connectWiFi();

    Serial.println();

    Serial.println("========================================");

    Serial.println(
        " SMARTEDGE EDGE RESOURCE BENCHMARK"
    );

    Serial.println(
        " SENSOR VALIDATION ENABLED"
    );

    Serial.println("========================================");
}


// =====================================================
// LOOP
// =====================================================

void loop() {

    // -------------------------------------------------
    // CHECK WIFI
    // -------------------------------------------------

    if (WiFi.status() != WL_CONNECTED) {

        connectWiFi();
    }


    // -------------------------------------------------
    // READ PZEM SENSOR
    // -------------------------------------------------

    float voltage =
        pzem.voltage();

    float current =
        pzem.current();

    float power =
        pzem.power();

    float energy =
        pzem.energy();

    float frequency =
        pzem.frequency();

    float pf =
        pzem.pf();


    // =================================================
    // SENSOR VALIDATION
    // =================================================

    SensorValidationResult validation =
        validateSensorData(
            voltage,
            current,
            power,
            energy,
            frequency,
            pf
        );


    // =================================================
    // INVALID READING
    // =================================================

    if (!validation.valid) {

        Serial.println();

        Serial.println(
            "========== SENSOR VALIDATION =========="
        );

        Serial.println(
            "Status : INVALID"
        );

        Serial.print(
            "Reason : "
        );

        Serial.println(
            validation.reason
        );

        Serial.println(
            "Action : READING REJECTED"
        );

        Serial.println(
            "Previous valid reading preserved."
        );

        Serial.println(
            "======================================="
        );

        delay(5000);

        return;
    }


    // =================================================
    // VALID READING
    // =================================================

    float safeVoltage =
        voltage;

    float safeCurrent =
        current;

    float safePower =
        power;

    float safeEnergy =
        energy;

    float safeFrequency =
        frequency;

    float safePowerFactor =
        pf;


    // =================================================
    // DISPLAY SENSOR DATA
    // =================================================

    Serial.println();

    Serial.println(
        "=========== LIVE DATA ==========="
    );

    Serial.print(
        "Voltage : "
    );

    Serial.println(
        safeVoltage
    );

    Serial.print(
        "Current : "
    );

    Serial.println(
        safeCurrent
    );

    Serial.print(
        "Power : "
    );

    Serial.println(
        safePower
    );

    Serial.print(
        "Energy : "
    );

    Serial.println(
        safeEnergy
    );

    Serial.print(
        "Frequency : "
    );

    Serial.println(
        safeFrequency
    );

    Serial.print(
        "Power Factor : "
    );

    Serial.println(
        safePowerFactor
    );


    // =================================================
    // EDGE FEATURE BENCHMARK START
    // =================================================

    featureStartTime =
        micros();


    // -------------------------------------------------
    // EDGE FEATURES
    // -------------------------------------------------

    float deltaPower = 0.0;

    float deltaCurrent = 0.0;

    float deltaPowerFactor = 0.0;

    float powerChangeRate = 0.0;


    if (hasPreviousReading) {

        deltaPower =
            safePower -
            previousPower;

        deltaCurrent =
            safeCurrent -
            previousCurrent;

        deltaPowerFactor =
            safePowerFactor -
            previousPowerFactor;


        unsigned long currentTime =
            millis();


        unsigned long elapsedMilliseconds =
            currentTime -
            previousReadingTime;


        if (elapsedMilliseconds > 0) {

            float elapsedSeconds =
                elapsedMilliseconds /
                1000.0;

            powerChangeRate =
                deltaPower /
                elapsedSeconds;
        }
    }


    featureEndTime =
        micros();


    // =================================================
    // UPDATE PREVIOUS READING
    // =================================================

    // IMPORTANT:
    //
    // This section is reached ONLY after validation.
    //
    // Therefore invalid/corrupted PZEM readings can never
    // become the previous reading used for edge features.

    previousPower =
        safePower;

    previousCurrent =
        safeCurrent;

    previousPowerFactor =
        safePowerFactor;

    previousReadingTime =
        millis();

    hasPreviousReading =
        true;


    // =================================================
    // FEATURE EXECUTION TIME
    // =================================================

    unsigned long featureExecutionTime =
        featureEndTime -
        featureStartTime;


    // =================================================
    // HEAP BEFORE JSON
    // =================================================

    uint32_t heapBeforeJson =
        ESP.getFreeHeap();


    // =================================================
    // JSON SERIALIZATION BENCHMARK
    // =================================================

    jsonStartTime =
        micros();


    JsonDocument doc;


    // -------------------------------------------------
    // BASIC SENSOR DATA
    // -------------------------------------------------

    doc["deviceId"] =
        "ESP32-001";

    doc["voltage"] =
        safeVoltage;

    doc["current"] =
        safeCurrent;

    doc["power"] =
        safePower;

    doc["energy"] =
        safeEnergy;

    doc["frequency"] =
        safeFrequency;

    doc["powerFactor"] =
        safePowerFactor;

    doc["tariffCost"] =
        safeEnergy * 8.0;

    doc["wifiStrength"] =
        WiFi.RSSI();

    doc["firmwareVersion"] =
        "1.2.1";


    // -------------------------------------------------
    // EDGE FEATURES
    // -------------------------------------------------

    doc["deltaPower"] =
        deltaPower;

    doc["deltaCurrent"] =
        deltaCurrent;

    doc["deltaPowerFactor"] =
        deltaPowerFactor;

    doc["powerChangeRate"] =
        powerChangeRate;

    doc["edgeFeatureReady"] =
        hasPreviousReading;


    // -------------------------------------------------
    // SERIALIZE JSON
    // -------------------------------------------------

    String json;

    serializeJson(
        doc,
        json
    );


    jsonEndTime =
        micros();


    // =================================================
    // HEAP AFTER JSON
    // =================================================

    uint32_t heapAfterJson =
        ESP.getFreeHeap();


    uint32_t heapUsedByJson =
        heapBeforeJson -
        heapAfterJson;


    // =================================================
    // JSON BENCHMARK
    // =================================================

    unsigned long jsonExecutionTime =
        jsonEndTime -
        jsonStartTime;


    size_t payloadSize =
        json.length();


    // =================================================
    // DISPLAY EDGE FEATURES
    // =================================================

    Serial.println();

    Serial.println(
        "======= EDGE FEATURES ======="
    );

    Serial.print(
        "Delta Power : "
    );

    Serial.println(
        deltaPower
    );

    Serial.print(
        "Delta Current : "
    );

    Serial.println(
        deltaCurrent
    );

    Serial.print(
        "Delta Power Factor : "
    );

    Serial.println(
        deltaPowerFactor
    );

    Serial.print(
        "Power Change Rate : "
    );

    Serial.print(
        powerChangeRate
    );

    Serial.println(
        " W/s"
    );


    // =================================================
    // RESOURCE BENCHMARK RESULTS
    // =================================================

    Serial.println();

    Serial.println(
        "======= RESOURCE BENCHMARK ======="
    );

    Serial.print(
        "Feature Extraction Time : "
    );

    Serial.print(
        featureExecutionTime
    );

    Serial.println(
        " us"
    );


    Serial.print(
        "JSON Serialization Time : "
    );

    Serial.print(
        jsonExecutionTime
    );

    Serial.println(
        " us"
    );


    Serial.print(
        "Heap Before JSON : "
    );

    Serial.print(
        heapBeforeJson
    );

    Serial.println(
        " bytes"
    );


    Serial.print(
        "Heap After JSON : "
    );

    Serial.print(
        heapAfterJson
    );

    Serial.println(
        " bytes"
    );


    Serial.print(
        "JSON Heap Difference : "
    );

    Serial.print(
        heapUsedByJson
    );

    Serial.println(
        " bytes"
    );


    Serial.print(
        "Payload Size : "
    );

    Serial.print(
        payloadSize
    );

    Serial.println(
        " bytes"
    );


    Serial.print(
        "Free Heap : "
    );

    Serial.print(
        ESP.getFreeHeap()
    );

    Serial.println(
        " bytes"
    );


    // =================================================
    // SEND JSON TO SERVER
    // =================================================

    Serial.println();

    Serial.println(
        "Sending JSON:"
    );

    Serial.println(
        json
    );


    HTTPClient http;


    http.begin(
        serverUrl
    );

    http.addHeader(
        "Content-Type",
        "application/json"
    );


    // =================================================
    // HTTP BENCHMARK START
    // =================================================

    httpStartTime =
        millis();


    int responseCode =
        http.POST(json);


    httpEndTime =
        millis();


    unsigned long httpExecutionTime =
        httpEndTime -
        httpStartTime;


    // =================================================
    // HTTP RESULT
    // =================================================

    Serial.print(
        "HTTP Response : "
    );

    Serial.println(
        responseCode
    );


    Serial.print(
        "HTTP Request Time : "
    );

    Serial.print(
        httpExecutionTime
    );

    Serial.println(
        " ms"
    );


    if (responseCode > 0) {

        String response =
            http.getString();

        Serial.println(
            response
        );
    }


    http.end();


    // =================================================
    // FINAL MEMORY STATUS
    // =================================================

    Serial.println();

    Serial.print(
        "Final Free Heap : "
    );

    Serial.print(
        ESP.getFreeHeap()
    );

    Serial.println(
        " bytes"
    );


    Serial.println();

    Serial.println(
        "========================================"
    );


    // =================================================
    // WAIT BEFORE NEXT READING
    // =================================================

    delay(5000);
}