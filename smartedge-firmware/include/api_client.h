#ifndef API_CLIENT_H
#define API_CLIENT_H

class ApiClient {

public:

    void sendData(
        float voltage,
        float current,
        float power,
        float energy,
        float frequency,
        float powerFactor,
        float tariffCost,
        int wifiStrength
    );

};

#endif