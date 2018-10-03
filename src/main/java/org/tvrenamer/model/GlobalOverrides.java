package org.tvrenamer.model;

import static org.tvrenamer.model.util.Constants.*;

import org.tvrenamer.controller.GlobalOverridesPersistence;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class GlobalOverrides {
    private static final Logger logger = Logger.getLogger(GlobalOverrides.class.getName());

    private static final GlobalOverrides INSTANCE = load();

    private final Map<String, String> showNames;

    private GlobalOverrides() {
        showNames = new HashMap<>();
    }

    public static GlobalOverrides getInstance() {
        return INSTANCE;
    }

    private static GlobalOverrides load() {
        GlobalOverrides overrides = GlobalOverridesPersistence.retrieve(OVERRIDES_FILE);

        if (overrides != null) {
            logger.fine("Successfully read overrides from: " + OVERRIDES_FILE.toAbsolutePath());
            logger.fine("Successfully read overrides: " + overrides.toString());
        } else {
            overrides = new GlobalOverrides();
            store(overrides);
        }

        return overrides;
    }

    private static void store(GlobalOverrides overrides) {
        GlobalOverridesPersistence.persist(overrides, OVERRIDES_FILE);
        logger.fine("Successfully saved/updated overrides");
    }

    public String getShowName(String showName) {
        String name = this.showNames.get(showName);
        if (name == null) {
            name = showName;
        }
        return name;
    }
}
