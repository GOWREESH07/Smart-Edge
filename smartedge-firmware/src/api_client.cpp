#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>

#include "api_client.h"
#include "config.h"

void ApiClient::sendData(
    float voltage,
    float current,
    float power,
    float energy,
    float frequency,
    float powerFactor,
    float tariffCost,
    int wifiStrength
) {

    HTTPClient http;

    http.begin(API_URL);

    http.addHeader("Content-Type", "application/json");

    JsonDocument doc;

    doc["deviceId"] = DEVICE_ID;
    doc["voltage"] = voltage;
    doc["current"] = current;
    doc["power"] = power;
    doc["energy"] = energy;
    doc["frequency"] = frequency;
    doc["powerFactor"] = powerFactor;
    doc["tariffCost"] = tariffCost;
    doc["wifiStrength"] = wifiStrength;
    doc["firmwareVersion"] = FIRMWARE_VERSION;

    String body;

    serializeJson(doc, body);

    Serial.println(body);

    int code = http.POST(body);

    Serial.print("HTTP Code : ");
    Serial.println(code);

    if(code > 0){

        Serial.println(http.getString());

    }

    http.end();

}