package com.ailudick.capitalismmod.compat;

import java.util.Map;

/**
 * Stable transport contract for factory inventories.  Future integrations
 * (Create belts, funnels and trains) should talk to this port instead of
 * changing the company production or warehouse code.
 */
public interface IndustrialTransportPort {
    Map<String, Integer> inputBuffer();
    Map<String, Integer> outputBuffer();
    int insertInput(String itemId, int amount);
    int extractOutput(String itemId, int amount);
}
