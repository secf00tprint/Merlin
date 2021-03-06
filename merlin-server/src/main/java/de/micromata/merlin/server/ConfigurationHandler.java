package de.micromata.merlin.server;

import com.fasterxml.jackson.core.type.TypeReference;
import de.micromata.merlin.server.json.JsonUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ConfigurationHandler {
    private Logger log = LoggerFactory.getLogger(ConfigurationHandler.class);
    private static final ConfigurationHandler instance = new ConfigurationHandler();
    private static final String WEBSERVER_PORT_PREF = "webserver-port";
    public static final int WEBSERVER_PORT_DEFAULT = 8042;
    private static final String LANGUAGE_PREF = "language";
    private static final String LANGUAGE_DEFAULT = null;
    private static final String SHOW_TEST_DATA_PREF = "show-test-data";
    private static final boolean SHOW_TEST_DATA_PREF_DEFAULT = true;
    private static final String WEB_DEVELOPMENT_MODE_PREF = "web-development-mode";
    private static final boolean WEB_DEVELOPMENT_MODE_PREF_DEFAULT = false;
    static final String TEMPLATES_DIRS = "template-directories";

    private Preferences preferences;
    private Configuration configuration = new Configuration();
    private List<ConfigurationListener> configurationListeners = new ArrayList<>();
    private Set<String> extraPreferences = new HashSet<>();

    /**
     * Only for test case.
     *
     * @param preferences
     */
    ConfigurationHandler(Preferences preferences) {
        this.preferences = preferences;
    }

    private ConfigurationHandler() {
        preferences = Preferences.userRoot().node("de").node("micromata").node("merlin");
        load();
    }

    public static ConfigurationHandler getInstance() {
        return instance;
    }

    public static Configuration getDefaultConfiguration() {
        return instance.getConfiguration();
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void register(ConfigurationListener listener) {
        configurationListeners.add(listener);
    }

    public void load() {
        configuration.setPort(preferences.getInt(WEBSERVER_PORT_PREF, WEBSERVER_PORT_DEFAULT));
        configuration.setShowTestData(preferences.getBoolean(SHOW_TEST_DATA_PREF, SHOW_TEST_DATA_PREF_DEFAULT));
        configuration.setWebDevelopmentMode(preferences.getBoolean(WEB_DEVELOPMENT_MODE_PREF, WEB_DEVELOPMENT_MODE_PREF_DEFAULT));
        String json = preferences.get(TEMPLATES_DIRS, null);
        if (json != null) {
            try {
                List<ConfigurationTemplatesDir> templateDirs =
                        JsonUtils.fromJson(new TypeReference<List<ConfigurationTemplatesDir>>() {
                        }, json);
                configuration.setTemplatesDirs(templateDirs);
            } catch (Exception ex) {
                log.error("Exception while parsing template dirs from json: " + ex.getMessage(), ex);
            }
        } else {
            configuration.setTemplatesDirs(null);
        }
        configuration.resetModifiedFlag();
    }

    public void save() {
        log.info("Saving configuration to user prefs.");
        preferences.putInt(WEBSERVER_PORT_PREF, configuration.getPort());
        preferences.putBoolean(SHOW_TEST_DATA_PREF, configuration.isShowTestData());
        preferences.putBoolean(WEB_DEVELOPMENT_MODE_PREF, configuration.isWebDevelopmentMode());
        if (CollectionUtils.isNotEmpty(configuration.getTemplatesDirs())) {
            String json = JsonUtils.toJson(configuration.getTemplatesDirs());
            preferences.put(TEMPLATES_DIRS, json);
        } else {
            preferences.remove(TEMPLATES_DIRS);
        }
        try {
            preferences.flush();
        } catch (BackingStoreException ex) {
            log.error("Couldn't flush user preferences: " + ex.getMessage(), ex);
        }
        notifyListeners(false);
    }

    /**
     * For saving own properties.
     * Doesn't call {@link #notifyListeners(boolean)}.
     *
     * @param key   The key under which to save the given value.
     * @param value The value to store. If null, any previous stored value under the given key will be removed.
     */
    public void save(String key, String value) {
        if (StringUtils.isEmpty(value)) {
            preferences.remove(key);
        } else {
            preferences.put(key, value);
            extraPreferences.add(key);
        }
        try {
            preferences.flush();
        } catch (BackingStoreException ex) {
            log.error("Couldn't flush user preferences: " + ex.getMessage(), ex);
        }
    }

    /**
     * @param key Gets own property saved with {@link #save()}.
     */
    public String get(String key, String defaultValue) {
        extraPreferences.add(key);
        return preferences.get(key, defaultValue);
    }

    private void notifyListeners(boolean force) {
        if (force || configuration.isTemplatesDirModified()) {
            for (ConfigurationListener listener : configurationListeners) {
                listener.templatesDirsModified();
            }
        }
    }

    public void removeAllSettings() {
        log.warn("Removes all configuration settings from user prefs.");
        preferences.remove(WEBSERVER_PORT_PREF);
        preferences.remove(LANGUAGE_PREF);
        preferences.remove(TEMPLATES_DIRS);
        preferences.remove(SHOW_TEST_DATA_PREF);
        preferences.remove(WEB_DEVELOPMENT_MODE_PREF);
        for(String extraKey : extraPreferences) {
            preferences.remove(extraKey);
        }
        try {
            preferences.flush();
        } catch (BackingStoreException ex) {
            log.error("Couldn't flush user preferences: " + ex.getMessage(), ex);
        }
        load();
        notifyListeners(true);
    }
}
