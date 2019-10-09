package com.poc.device.store;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.hash.BloomFilter;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;

/**
 * A suspect device store.
 *
 * This is a store of all suspect devices. It provides interfaces to
 *
 * add a suspect device to the store
 * add a list of suspect devices to the store
 * check if a device might be a malicious device
 *
 * Example usage:
 *
 * com.poc.device.store.SuspectDeviceStore suspectStore = com.poc.device.store.SuspectDeviceStore.getSuspectDeviceStore();
 * boolean isSuspect = suspectStore.isSuspectDevice("DEVICE_101");
 *
 * // To add a device to suspect list
 * suspectStore.addSuspectDevice("DEVICE_101");
 *
 *
 */
public final class SuspectDeviceStore implements Serializable {
    /** These could be externalized via some flag to configure the store */
    private static final int DEFAULT_SUSPECT_DEVICE_SIZE = 10_00_0000;
    private static final double DEFAULT_MAX_ERROR_PERCENTAGE = 0.01;

    private static SuspectDeviceStore suspectDeviceStore;
    private final BloomFilter<String> suspectDeviceBloomFilter;

    /** Private constructor to create the store instance */
    private SuspectDeviceStore(BloomFilter<String> suspectDeviceBloomFilter) {
        this.suspectDeviceBloomFilter = suspectDeviceBloomFilter;
    }

    /** A holder for the singleton com.poc.device.store.SuspectDeviceStore instance */
    private static class DeviceStoreHolder {
        private static SuspectDeviceStore INSTANCE
                = initSuspectDeviceStore(DEFAULT_SUSPECT_DEVICE_SIZE, DEFAULT_MAX_ERROR_PERCENTAGE);
    }

    /** Initializes the com.poc.device.store.SuspectDeviceStore with the size and error percentage */
    private static final SuspectDeviceStore initSuspectDeviceStore(int size, double maximumErrorPercentage) {
        BloomFilter<String> bloomFilter = BloomFilter.create(DeviceFunnel.INSTANCE, size, maximumErrorPercentage);
        suspectDeviceStore = new SuspectDeviceStore(bloomFilter);
        return suspectDeviceStore;
    }

    /** Returns the instance of the com.poc.device.store.SuspectDeviceStore */
    public static SuspectDeviceStore getSuspectDeviceStore() {
        return DeviceStoreHolder.INSTANCE;
    }

    /** Adds a suspect device to the store */
    public void addSuspectDevice(String deviceUid) {
        if(deviceUid == null || deviceUid.isEmpty()) {
            throw new IllegalArgumentException("Device Id cannot be null");
        }
        suspectDeviceBloomFilter.put(deviceUid);
    }

    /** Adds a list of suspect devices the store */
    public void addAllSuspectDevices(List<String> deviceUidList) {
        for (String deviceUid: deviceUidList) {
            addSuspectDevice(deviceUid);
        }
    }

    /** Checks if a device might be a suspect device.  */
    public boolean isSuspectDevice(String deviceUid) {
        return suspectDeviceBloomFilter.mightContain(deviceUid);
    }

    /**
     * Returns the probability of erroneously returning true for an device that has not actually been put in the
     * suspectDeviceBloomFilter.
     */
    public double suspectDeviceStoreFPP(){
        return suspectDeviceBloomFilter.expectedFpp();
    }

    /**
     * Computes m (total bits of Bloom filter) which is expected to achieve, for the specified
     * expected insertions, the required false positive probability.
     *
     * @param expectedInsertions the number of expected insertions(n) to the constructed suspectDeviceBloomFilter
     * @return total bits(m) of Bloom filter
     */
    public long optimalNumOfBits(int expectedInsertions) {
        return (long) Math.ceil(-expectedInsertions * Math.log(DEFAULT_MAX_ERROR_PERCENTAGE) / (Math.log(2) * Math.log(2)));
    }

    /**
     * Writes this {@code BloomFilter} to an output stream, with a custom format (not Java
     * serialization). This has been measured to save at least 400 bytes compared to regular serialization.
     *
     * @param stream
     * @throws IOException
     */
    public void writeTo(ObjectOutputStream stream) throws IOException {
        this.suspectDeviceBloomFilter.writeTo(stream);
    }

    /**
     * Returns the instance of the com.poc.device.store.SuspectDeviceStore with a specified number of expected
     * suspect device ids
     */
    @VisibleForTesting
    public static SuspectDeviceStore getSuspectDeviceStore(int expectedNumberOfInsertions) {
        DeviceStoreHolder.INSTANCE = initSuspectDeviceStore(expectedNumberOfInsertions, DEFAULT_MAX_ERROR_PERCENTAGE);
        return DeviceStoreHolder.INSTANCE;
    }
}
