#ifndef PZEM_READER_H
#define PZEM_READER_H

class PzemReader {

public:

    void begin();

    float voltage();

    float current();

    float power();

    float energy();

    float frequency();

    float powerFactor();

};

#endif