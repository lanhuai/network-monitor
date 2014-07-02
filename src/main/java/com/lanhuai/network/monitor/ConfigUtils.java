package com.lanhuai.network.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:lanhuai@gmail.com">Ning Yubin</a>
 */
public final class ConfigUtils {
    private static final Logger logger = LoggerFactory.getLogger(ConfigUtils.class);
    public static final String[] EMPTY_STRING_ARRAY = {};
    private static final Pattern DELIMITER_PATTERN = Pattern.compile(",");
    private static final Properties PROPERTIES;

    static {
        PROPERTIES = new Properties();
        InputStream inputStream = null;
        try {
            inputStream = ConfigUtils.class.getClassLoader().getResourceAsStream("config.properties");
            PROPERTIES.load(inputStream);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
            System.exit(1);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
    }

    private ConfigUtils() {
    }

    public static String getValue(String key) {
        return PROPERTIES.getProperty(key);
    }

    public static String[] getValues(String key) {
        String value = getValue(key);
        if (value == null || value.trim().isEmpty()) {
            return EMPTY_STRING_ARRAY;
        } else {
            return DELIMITER_PATTERN.split(value);
        }
    }

    public static void main(String[] args) {
        System.out.println(getValue("servers"));
    }
}
