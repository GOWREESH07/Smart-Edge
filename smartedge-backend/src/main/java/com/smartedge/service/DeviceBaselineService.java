package com.smartedge.service;

import com.smartedge.entity.DeviceBaseline;
import com.smartedge.entity.EnergyReading;
import com.smartedge.repository.DeviceBaselineRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class DeviceBaselineService {

    private final DeviceBaselineRepository repository;

    public DeviceBaselineService(DeviceBaselineRepository repository) {
        this.repository = repository;
    }

    /**
     * Updates the learned baseline using Welford's online algorithm.
     *
     * The baseline is updated only when the reading contains
     * physically reasonable values.
     */
    @Transactional
    public void updateBaseline(EnergyReading reading) {

        if (reading == null) {
            return;
        }

        if (!isValidForLearning(reading)) {
            return;
        }

        String deviceId = reading.getDeviceId();

        if (deviceId == null || deviceId.isBlank()) {
            return;
        }

        DeviceBaseline baseline =
                repository.findByDeviceId(deviceId)
                        .orElseGet(() -> createNewBaseline(deviceId));

        long currentCount =
                baseline.getSampleCount() == null
                        ? 0L
                        : baseline.getSampleCount();

        long newCount = currentCount + 1;

        /*
         * First valid reading.
         */
        if (currentCount == 0) {

            baseline.setVoltageMean(reading.getVoltage());
            baseline.setVoltageM2(0.0);

            baseline.setCurrentMean(reading.getCurrent());
            baseline.setCurrentM2(0.0);

            baseline.setPowerMean(reading.getPower());
            baseline.setPowerM2(0.0);

            baseline.setFrequencyMean(reading.getFrequency());
            baseline.setFrequencyM2(0.0);

            baseline.setPowerFactorMean(reading.getPowerFactor());
            baseline.setPowerFactorM2(0.0);

        } else {

            /*
             * Welford update for each parameter.
             */
            double voltageDelta =
                    reading.getVoltage() - baseline.getVoltageMean();

            double newVoltageMean =
                    baseline.getVoltageMean()
                            + voltageDelta / newCount;

            double voltageDelta2 =
                    reading.getVoltage() - newVoltageMean;

            double newVoltageM2 =
                    baseline.getVoltageM2()
                            + voltageDelta * voltageDelta2;

            baseline.setVoltageMean(newVoltageMean);
            baseline.setVoltageM2(newVoltageM2);


            double currentDelta =
                    reading.getCurrent() - baseline.getCurrentMean();

            double newCurrentMean =
                    baseline.getCurrentMean()
                            + currentDelta / newCount;

            double currentDelta2 =
                    reading.getCurrent() - newCurrentMean;

            double newCurrentM2 =
                    baseline.getCurrentM2()
                            + currentDelta * currentDelta2;

            baseline.setCurrentMean(newCurrentMean);
            baseline.setCurrentM2(newCurrentM2);


            double powerDelta =
                    reading.getPower() - baseline.getPowerMean();

            double newPowerMean =
                    baseline.getPowerMean()
                            + powerDelta / newCount;

            double powerDelta2 =
                    reading.getPower() - newPowerMean;

            double newPowerM2 =
                    baseline.getPowerM2()
                            + powerDelta * powerDelta2;

            baseline.setPowerMean(newPowerMean);
            baseline.setPowerM2(newPowerM2);


            double frequencyDelta =
                    reading.getFrequency() - baseline.getFrequencyMean();

            double newFrequencyMean =
                    baseline.getFrequencyMean()
                            + frequencyDelta / newCount;

            double frequencyDelta2 =
                    reading.getFrequency() - newFrequencyMean;

            double newFrequencyM2 =
                    baseline.getFrequencyM2()
                            + frequencyDelta * frequencyDelta2;

            baseline.setFrequencyMean(newFrequencyMean);
            baseline.setFrequencyM2(newFrequencyM2);


            double powerFactorDelta =
                    reading.getPowerFactor()
                            - baseline.getPowerFactorMean();

            double newPowerFactorMean =
                    baseline.getPowerFactorMean()
                            + powerFactorDelta / newCount;

            double powerFactorDelta2 =
                    reading.getPowerFactor()
                            - newPowerFactorMean;

            double newPowerFactorM2 =
                    baseline.getPowerFactorM2()
                            + powerFactorDelta * powerFactorDelta2;

            baseline.setPowerFactorMean(newPowerFactorMean);
            baseline.setPowerFactorM2(newPowerFactorM2);
        }

        baseline.setSampleCount(newCount);
        baseline.setLastUpdated(LocalDateTime.now());

        repository.save(baseline);
    }


    /**
     * Creates an empty baseline for a new device.
     */
    private DeviceBaseline createNewBaseline(String deviceId) {

        DeviceBaseline baseline = new DeviceBaseline();

        baseline.setDeviceId(deviceId);

        baseline.setVoltageMean(0.0);
        baseline.setVoltageM2(0.0);

        baseline.setCurrentMean(0.0);
        baseline.setCurrentM2(0.0);

        baseline.setPowerMean(0.0);
        baseline.setPowerM2(0.0);

        baseline.setFrequencyMean(0.0);
        baseline.setFrequencyM2(0.0);

        baseline.setPowerFactorMean(0.0);
        baseline.setPowerFactorM2(0.0);

        baseline.setSampleCount(0L);

        return baseline;
    }


    /**
     * Prevents abnormal or physically impossible readings
     * from becoming part of the learned normal baseline.
     */
    private boolean isValidForLearning(EnergyReading reading) {

        if (reading.getVoltage() == null
                || reading.getCurrent() == null
                || reading.getPower() == null
                || reading.getFrequency() == null
                || reading.getPowerFactor() == null) {
            return false;
        }

        double voltage = reading.getVoltage();
        double current = reading.getCurrent();
        double power = reading.getPower();
        double frequency = reading.getFrequency();
        double powerFactor = reading.getPowerFactor();

        /*
         * Reasonable operating ranges.
         */
        if (voltage < 180.0 || voltage > 270.0) {
            return false;
        }

        if (current < 0.0 || current > 20.0) {
            return false;
        }

        if (power < 0.0 || power > 5000.0) {
            return false;
        }

        if (frequency < 49.0 || frequency > 51.0) {
            return false;
        }

        if (powerFactor < 0.0 || powerFactor > 1.0) {
            return false;
        }

        return true;
    }


    /**
     * Returns the baseline for a device.
     */
    public DeviceBaseline getBaseline(String deviceId) {

        return repository.findByDeviceId(deviceId)
                .orElse(null);
    }
}