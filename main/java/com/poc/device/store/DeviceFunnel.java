package com.poc.device.store;

import com.google.common.base.Charsets;
import com.google.common.hash.Funnel;
import com.google.common.hash.PrimitiveSink;

/**
 * Device Funnel funnels implemented as a single-element enum to maintain serialization guarantees.
 */
public enum DeviceFunnel implements Funnel<String> {
    INSTANCE;

    @Override
    public void funnel(String deviceUid, PrimitiveSink into) {
        into.putString(deviceUid, Charsets.UTF_8);
    }
}
