package de.aspua.gui.Utils;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import static java.util.Locale.ENGLISH;

import com.vaadin.flow.i18n.I18NProvider;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * I18N Provider from Vaadin to use translations in components via 'this.getTranslation("code")'.
 * Provider can be manually set via mvn spring-boot:run -Dvaadin.i18n.provider=de.aspua.gui.Utils.TranslationsProvider.
 */
@Component
public class TranslationProvider implements I18NProvider
{
    public static final String BUNDLE_PREFIX = "translation";
    // Saves all currently supported locales (defined in resource/translation_<countrycode>.properties)
    private static final List<Locale> providedLocales = Collections.unmodifiableList(Arrays.asList(ENGLISH));

    @Override
    public List<Locale> getProvidedLocales() {
        return providedLocales;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params)
    {
        if (key == null)
        {
            LoggerFactory.getLogger(TranslationProvider.class.getName())
                    .warn("Got lang request for key with null value!");
            return "";
        }

        final ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale);
        String value;

        try {
            value = bundle.getString(key);
        } catch (final MissingResourceException e) {
            LoggerFactory.getLogger(TranslationProvider.class.getName())
                    .warn("Missing resource", e);
            return "!" + locale.getLanguage() + ": " + key;
        }

        if (params.length > 0)
            value = MessageFormat.format(value, params);

        return value;
    }
}
