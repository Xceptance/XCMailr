/*
 * Copyright (c) Xceptance Software Technologies GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.xceptance.xcmailr.config;

import java.util.Objects;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Resolves and validates the active web UI theme stylesheet name based on application configuration.
 * <p>
 * To prevent directory traversal, path injection, or unstyled page renders, candidate theme names are
 * sanitized against a strict slug regex pattern ({@code ^[a-zA-Z0-9_-]+$}), restricted to a maximum
 * length of 32 characters, and verified against classpath availability in {@code /static/css/{theme}-theme.css}.
 * If the configured theme is null, blank, malformed, or absent on the classpath, the resolver safely falls back
 * to the default corporate theme ({@value #DEFAULT_THEME}) and logs an appropriate warning.
 * </p>
 *
 * @author Xceptance Software Technologies GmbH
 */
@Component("themeResolver")
@ControllerAdvice
public class ThemeResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ThemeResolver.class);

    /**
     * Default corporate theme identifier.
     */
    public static final String DEFAULT_THEME = "xceptance";

    /**
     * Maximum permitted length for theme slug names.
     */
    public static final int MAX_THEME_SLUG_LENGTH = 32;

    /**
     * Regex pattern restricting theme slugs to alphanumeric characters, dashes, and underscores.
     */
    public static final Pattern THEME_SLUG_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");

    /**
     * Classpath location prefix where theme stylesheets are stored.
     */
    public static final String THEME_CSS_PATH_PREFIX = "classpath:/static/css/";

    /**
     * Suffix for theme stylesheet filenames.
     */
    public static final String THEME_CSS_FILE_SUFFIX = "-theme.css";

    private final ResourceLoader resourceLoader;
    private final String configuredTheme;
    private final String activeTheme;

    /**
     * Constructs a new {@link ThemeResolver} with the configured theme name and Spring {@link ResourceLoader}.
     *
     * @param configuredTheme the theme identifier configured via {@code xcmailr.ui.theme}, defaulting to {@value #DEFAULT_THEME}
     * @param resourceLoader  the Spring resource loader used to verify classpath asset availability
     */
    public ThemeResolver(@Value("${xcmailr.ui.theme:xceptance}") final String configuredTheme,
                         final ResourceLoader resourceLoader)
    {
        this.resourceLoader = Objects.requireNonNull(resourceLoader, "resourceLoader must not be null");
        this.configuredTheme = configuredTheme;
        this.activeTheme = resolveTheme(configuredTheme);
    }

    /**
     * Resolves and validates a candidate theme name against slug format, length constraints,
     * and classpath resource existence.
     *
     * @param themeSlug the candidate theme name to evaluate
     * @return the verified theme slug, or {@value #DEFAULT_THEME} if invalid or missing
     */
    public String resolveTheme(final String themeSlug)
    {
        if (themeSlug == null || themeSlug.isEmpty())
        {
            LOGGER.debug("No custom UI theme configured; defaulting to '{}'.", DEFAULT_THEME);
            return DEFAULT_THEME;
        }

        if (themeSlug.isBlank())
        {
            LOGGER.warn("Configured UI theme is blank. Falling back to default theme '{}'.", DEFAULT_THEME);
            return DEFAULT_THEME;
        }

        if (themeSlug.length() > MAX_THEME_SLUG_LENGTH)
        {
            LOGGER.warn("Configured UI theme '{}' exceeds maximum allowed slug length of {} characters. Falling back to default theme '{}'.",
                themeSlug, MAX_THEME_SLUG_LENGTH, DEFAULT_THEME);
            return DEFAULT_THEME;
        }

        if (!THEME_SLUG_PATTERN.matcher(themeSlug).matches())
        {
            LOGGER.warn("Configured UI theme '{}' contains invalid characters (must match {}). Potential path traversal or injection rejected. Falling back to default theme '{}'.",
                themeSlug, THEME_SLUG_PATTERN.pattern(), DEFAULT_THEME);
            return DEFAULT_THEME;
        }

        final String resourcePath = THEME_CSS_PATH_PREFIX + themeSlug + THEME_CSS_FILE_SUFFIX;
        final Resource resource = resourceLoader.getResource(resourcePath);

        if (resource == null || !resource.exists())
        {
            LOGGER.warn("Configured UI theme stylesheet '{}' does not exist on the classpath. Falling back to default theme '{}'.",
                resourcePath, DEFAULT_THEME);
            return DEFAULT_THEME;
        }

        LOGGER.info("Active UI theme resolved: '{}' (resource: '{}')", themeSlug, resourcePath);
        return themeSlug;
    }

    /**
     * Returns the actively resolved theme slug to be linked in UI layouts.
     * Exposes {@code activeTheme} as a global model attribute across all web views.
     *
     * @return the active theme slug name
     */
    @ModelAttribute("activeTheme")
    public String getActiveTheme()
    {
        return activeTheme;
    }

    /**
     * Returns the raw theme property configured on application startup.
     *
     * @return the raw configured theme property value
     */
    public String getConfiguredTheme()
    {
        return configuredTheme;
    }
}
