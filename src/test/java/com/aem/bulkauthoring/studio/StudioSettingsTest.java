package com.aem.bulkauthoring.studio;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudioSettingsTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void defaultsPackageDeliveryToDownload() {
        StudioSettings settings = new StudioSettings();

        assertEquals("download", settings.getDeliveryMethod());
        assertEquals("download", settings.toMap().get("deliveryMethod"));
    }

    @Test
    void appliesInstallDeliveryMethodFromJson() throws Exception {
        StudioSettings settings = new StudioSettings();

        settings.applyFrom(MAPPER.readTree("{\"deliveryMethod\":\"install\"}"));

        assertEquals("install", settings.getDeliveryMethod());
    }

    @Test
    void normalizesUnknownDeliveryMethodToDownload() {
        StudioSettings settings = new StudioSettings();

        settings.setDeliveryMethod("unknown");

        assertEquals("download", settings.getDeliveryMethod());
    }
}
