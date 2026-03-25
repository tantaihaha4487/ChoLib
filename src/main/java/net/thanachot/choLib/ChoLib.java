package net.thanachot.choLib;

import net.fabricmc.api.ModInitializer;
import net.thanachot.choLib.internal.shift.ShiftListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChoLib implements ModInitializer {
    public static final String MOD_ID = "cholib";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing ChoLib Shift Mechanism");
        ShiftListener.init();
        LOGGER.info("ChoLib Shift Mechanism initialized successfully");
    }
}
