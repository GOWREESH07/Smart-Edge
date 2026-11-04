#include <WiFi.h>
#include "wifi_manager.h"
#include "secrets.h"

void WiFiManager::connect() {

    Serial.println();
    Serial.println("========== SMARTEDGE ==========");
    Serial.print("Connecting to: ");
    Serial.println(WIFI_SSID);

    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    int retries = 0;

    while (WiFi.status() != WL_CONNECTED && retries < 30) {

        delay(500);
        Serial.print(".");

        retries++;

    }

    Serial.println();

    if (WiFi.status() == WL_CONNECTED) {

        Serial.println("WiFi Connected!");
        Serial.print("IP: ");
        Serial.println(WiFi.localIP());

    } else {

        Serial.println("Connection Failed!");
        Serial.print("Status Code: ");
        Serial.println(WiFi.status());

    }
}

bool WiFiManager::isConnected() {
    return WiFi.status() == WL_CONNECTED;
}

int WiFiManager::getRSSI() {
    return WiFi.RSSI();
}