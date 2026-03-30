package com.example.elevator;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

enum AnnouncementKey {
    WEIGHT_LIMIT_EXCEEDED,
    EMERGENCY_MODE_ENABLED,
    MAINTENANCE_MODE_ENABLED
}

interface SoundPlayer {
    void play(String soundKey);
}

final class MockSoundPlayer implements SoundPlayer {
    @Override
    public void play(String soundKey) {
        System.out.println("[SOUND] " + soundKey);
    }
}

interface AnnouncementService {
    void announce(AnnouncementKey key, Locale locale, Map<String, String> params);
}

final class ConsoleAnnouncementService implements AnnouncementService {
    @Override
    public void announce(AnnouncementKey key, Locale locale, Map<String, String> params) {
        Objects.requireNonNull(key, "key");
        Locale use = locale == null ? Locale.ENGLISH : locale;
        Map<String, String> p = params == null ? Map.of() : params;

        String lang = use.getLanguage().toLowerCase(Locale.ROOT);
        if (lang.startsWith("hi")) {
            announceHindi(key, p);
        } else {
            announceEnglish(key, p);
        }
    }

    private void announceEnglish(AnnouncementKey key, Map<String, String> params) {
        switch (key) {
            case WEIGHT_LIMIT_EXCEEDED -> System.out.println("Announcement: Weight limit exceeded. Doors will stay open.");
            case EMERGENCY_MODE_ENABLED -> System.out.println("Announcement: Emergency mode enabled. Elevators returning to ground floor.");
            case MAINTENANCE_MODE_ENABLED -> System.out.println("Announcement: Elevator in maintenance. Service disabled.");
            default -> System.out.println("Announcement: " + key);
        }
    }

    private void announceHindi(AnnouncementKey key, Map<String, String> params) {
        // Lightweight multilingual stub (as required: interface supports multilingual announcements).
        switch (key) {
            case WEIGHT_LIMIT_EXCEEDED -> System.out.println("घोषणा: वजन सीमा पार हो गई। दरवाजे खुले रहेंगे।");
            case EMERGENCY_MODE_ENABLED -> System.out.println("घोषणा: इमरजेंसी मोड चालू। लिफ्टें ग्राउंड फ्लोर पर जाएंगी।");
            case MAINTENANCE_MODE_ENABLED -> System.out.println("घोषणा: लिफ्ट में मेंटेनेंस। सेवा बंद है।");
            default -> System.out.println("घोषणा: " + key);
        }
    }
}

