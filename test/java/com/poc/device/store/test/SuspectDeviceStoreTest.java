package com.poc.device.store.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import com.poc.device.store.SuspectDeviceStore;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.ExpectedException;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Unit test for {@link com.poc.device.store.SuspectDeviceStore} */
public class SuspectDeviceStoreTest {
    private static final String DEVICE_ID_PREFIX = "DEVICE_";

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void whenAddASuspectDeviceToStore_thenShouldAddAndReturnTrue(){
        SuspectDeviceStore suspectDeviceStore = SuspectDeviceStore.getSuspectDeviceStore();
        String deviceId = DEVICE_ID_PREFIX+ "101";
        suspectDeviceStore.addSuspectDevice(deviceId);
        assertTrue(suspectDeviceStore.isSuspectDevice(deviceId));
    }

    @Test
    public void whenAddAnEmptyId_thenShouldThrowException(){
        SuspectDeviceStore suspectDeviceStore = SuspectDeviceStore.getSuspectDeviceStore();
        String deviceId = "";

        exceptionRule.expect(IllegalArgumentException.class);
        exceptionRule.expectMessage("Device Id cannot be null");
        suspectDeviceStore.addSuspectDevice(deviceId);
    }

    @Test
    public void whenAddNDevicesToSuspectDeviceStore_thenShouldNotReturnAnyFalsePositive(){
        List<String> devices = new ArrayList<>();
        devices.add(DEVICE_ID_PREFIX+ "101");
        devices.add(DEVICE_ID_PREFIX+ "102");
        devices.add(DEVICE_ID_PREFIX+ "103");
        devices.add(DEVICE_ID_PREFIX+ "104");
        devices.add(DEVICE_ID_PREFIX+ "105");

        SuspectDeviceStore suspectDeviceStore = SuspectDeviceStore.getSuspectDeviceStore();
        suspectDeviceStore.addAllSuspectDevices(devices);

        for(String deviceId: devices) {
            Assert.assertTrue(suspectDeviceStore.isSuspectDevice(deviceId));
        }
        assertFalse(suspectDeviceStore.isSuspectDevice("NON_EXISTENT"));
    }

    @Test
    public void whenAddSmallerOrCloserToExpectedDevicesInSuspectDeviceStore_thenExpectFPPOfOnePercent() {
        List<String> devices = new ArrayList<>();
        for (int i = 0; i <= 100000; i++) {
            devices.add(DEVICE_ID_PREFIX+ i);
        }

        SuspectDeviceStore suspectDeviceStore = SuspectDeviceStore.getSuspectDeviceStore();
        suspectDeviceStore.addAllSuspectDevices(devices);

        assertEquals("Should yield accurate results when N less than or closer to  DEFAULT_SUSPECT_DEVICE_SIZE.",
                0.01, suspectDeviceStore.suspectDeviceStoreFPP(),
                suspectDeviceStore.suspectDeviceStoreFPP() - 0.01);
    }

    @Test
    public void whenAddMoreThanExpectedDevicesToTheSuspectDeviceStore_thenReturnsMoreThanExpectedFPP() {
        List<String> devices = new ArrayList<>();
        for (int i = 0; i <= 1000000; i++) {
            devices.add(DEVICE_ID_PREFIX+ i);
        }

        SuspectDeviceStore suspectDeviceStore = SuspectDeviceStore.getSuspectDeviceStore();
        suspectDeviceStore.addAllSuspectDevices(devices);

        assertTrue(suspectDeviceStore.suspectDeviceStoreFPP() > 0.01 );
    }

    /**
     * Compares memory consumption of SuspectDeviceStore using bloom filter vs HashSet.
     *
     * Prints out approximate memory consumption of SuspectDeviceStore for 1000, 1 Million and 10 Million device ids.
     *
     * The memory consumption of SuspectDeviceStore is calculated in two ways:
     *  1. The serialized form of the whole object when written to a byte stream
     *  2. The no of bits the BloomFilter expects to use for a given configuration of the number of expected entries
     *
     * @throws IOException
     */
    @Test
    public void memoryEfficiency_comparedToHashSet() throws IOException {
        List<String> devices = new ArrayList<>();
        HashSet<String> devicesSet = new HashSet<>();

        //Comparing memory consumption of bloom filter vs HashSet
        for (int i = 0; i <= 1000; i++) {
            devices.add(DEVICE_ID_PREFIX + i);
            devicesSet.add(DEVICE_ID_PREFIX + i);
        }
        // Initialize SuspectDeviceStore with a size of 1000 for test
        SuspectDeviceStore suspectDeviceStore = SuspectDeviceStore.getSuspectDeviceStore(1000);
        suspectDeviceStore.addAllSuspectDevices(devices);

        double sizeOfDevicesSetInMB = (devicesSet.size() * 32 + 4 * (devicesSet.size() / 0.75)/1024)/1024;
        double sizeOfStoreInMB =  ((getSuspectDeviceStoreSize(suspectDeviceStore)/1024.0)/1024.0);

        System.out.println("Size of devicesSet with 1000 devices is:" + sizeOfDevicesSetInMB + " " + "MB");
        System.out.println("Size of store with 1000 devices: "+ sizeOfStoreInMB + " " + "MB");

        long optimalSizeInBytes = suspectDeviceStore.optimalNumOfBits(1000)/8;
        System.out.println("Total bits of bloom filter for 1000 devices: " +
                suspectDeviceStore.optimalNumOfBits(1000) + "bits.");
        System.out.println("Size in MB is " +
                (optimalSizeInBytes/1024.0)/1024.0 + " MB");

        assertTrue(sizeOfDevicesSetInMB > sizeOfStoreInMB);


        //Memory consumption for 1 Million suspect devices
        for (int i = 0; i <= 1000000; i++) {
            devices.add(DEVICE_ID_PREFIX + i);
        }
        suspectDeviceStore = SuspectDeviceStore.getSuspectDeviceStore(1000000);
        suspectDeviceStore.addAllSuspectDevices(devices);

        sizeOfStoreInMB =  ((getSuspectDeviceStoreSize(suspectDeviceStore)/1024.0)/1024.0);
        System.out.println("Size of store with 1,000,000 devices: "+ sizeOfStoreInMB + " " + "MB");

        optimalSizeInBytes = suspectDeviceStore.optimalNumOfBits(1000000)/8;
        System.out.println("Total bits of bloom filter for 1,000,000 devices: " +
                suspectDeviceStore.optimalNumOfBits(1000000) + "bits.");
        System.out.println("Size in MB is " +
                (optimalSizeInBytes/1024.0)/1024.0 + " MB");

        //Memory consumption for 10 Million suspect devices
        devices = new ArrayList<>();
        for (int i = 0; i <= 10000000; i++) {
            devices.add("6" + i);
        }
        suspectDeviceStore = SuspectDeviceStore.getSuspectDeviceStore(10000000);
        suspectDeviceStore.addAllSuspectDevices(devices);
        sizeOfStoreInMB =  ((getSuspectDeviceStoreSize(suspectDeviceStore)/1024.0)/1024.0);
        System.out.println("Size of store with 10,000,000: "+ sizeOfStoreInMB + " " + "MB");
        optimalSizeInBytes = suspectDeviceStore.optimalNumOfBits(10000000)/8;
        System.out.println("Total bits of bloom filter for 10,000,000 devices: "+
                suspectDeviceStore.optimalNumOfBits(10000000) + " bits.");
        System.out.println("Size in MB is " +
                (optimalSizeInBytes/1024.0)/1024.0 + " MB");

    }

    /** Gets the size of the suspect device store object in serialized form */
    private int getSuspectDeviceStoreSize(SuspectDeviceStore store) throws IOException {
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteOutputStream);
        store.writeTo(objectOutputStream);
        objectOutputStream.flush();
        objectOutputStream.close();

        return byteOutputStream.toByteArray().length;
    }
}
