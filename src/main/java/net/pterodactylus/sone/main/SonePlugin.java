/*
 * FreenetSone - SonePlugin.java - Copyright © 2010 David Roden
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.pterodactylus.sone.main;

import java.io.File;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import net.pterodactylus.sone.core.Core;
import net.pterodactylus.sone.core.FreenetInterface;
import net.pterodactylus.sone.freenet.PluginStoreConfigurationBackend;
import net.pterodactylus.sone.web.WebInterface;
import net.pterodactylus.util.config.Configuration;
import net.pterodactylus.util.config.ConfigurationException;
import net.pterodactylus.util.config.MapConfigurationBackend;
import net.pterodactylus.util.config.XMLConfigurationBackend;
import net.pterodactylus.util.logging.Logging;
import net.pterodactylus.util.logging.LoggingListener;
import net.pterodactylus.util.version.Version;
import freenet.client.async.DatabaseDisabledException;
import freenet.l10n.BaseL10n.LANGUAGE;
import freenet.l10n.PluginL10n;
import freenet.pluginmanager.FredPlugin;
import freenet.pluginmanager.FredPluginBaseL10n;
import freenet.pluginmanager.FredPluginL10n;
import freenet.pluginmanager.FredPluginThreadless;
import freenet.pluginmanager.FredPluginVersioned;
import freenet.pluginmanager.PluginRespirator;
import freenet.pluginmanager.PluginStore;

/**
 * This class interfaces with Freenet. It is the class that is loaded by the
 * node and starts up the whole Sone system.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SonePlugin implements FredPlugin, FredPluginL10n, FredPluginBaseL10n, FredPluginThreadless, FredPluginVersioned {

	static {
		/* initialize logging. */
		Logging.setup("sone");
		Logging.addLoggingListener(new LoggingListener() {

			@Override
			public void logged(LogRecord logRecord) {
				Class<?> loggerClass = Logging.getLoggerClass(logRecord.getLoggerName());
				int recordLevel = logRecord.getLevel().intValue();
				if (recordLevel < Level.FINE.intValue()) {
					freenet.support.Logger.debug(loggerClass, String.format(logRecord.getMessage(), logRecord.getParameters()), logRecord.getThrown());
				} else if (recordLevel < Level.INFO.intValue()) {
					freenet.support.Logger.minor(loggerClass, String.format(logRecord.getMessage(), logRecord.getParameters()), logRecord.getThrown());
				} else if (recordLevel < Level.WARNING.intValue()) {
					freenet.support.Logger.normal(loggerClass, String.format(logRecord.getMessage(), logRecord.getParameters()), logRecord.getThrown());
				} else if (recordLevel < Level.SEVERE.intValue()) {
					freenet.support.Logger.warning(loggerClass, String.format(logRecord.getMessage(), logRecord.getParameters()), logRecord.getThrown());
				} else {
					freenet.support.Logger.error(loggerClass, String.format(logRecord.getMessage(), logRecord.getParameters()), logRecord.getThrown());
				}
			}

		});
	}

	/** The version. */
	public static final Version VERSION = new Version("RC3", 0, 1);

	/** The logger. */
	private static final Logger logger = Logging.getLogger(SonePlugin.class);

	/** The plugin respirator. */
	private PluginRespirator pluginRespirator;

	/** The core. */
	private Core core;

	/** The web interface. */
	private WebInterface webInterface;

	/** The l10n helper. */
	private PluginL10n l10n;

	/** The plugin store. */
	private PluginStore pluginStore;

	//
	// ACCESSORS
	//

	/**
	 * Returns the plugin respirator for this plugin.
	 *
	 * @return The plugin respirator
	 */
	public PluginRespirator pluginRespirator() {
		return pluginRespirator;
	}

	/**
	 * Returns the core started by this plugin.
	 *
	 * @return The core
	 */
	public Core core() {
		return core;
	}

	/**
	 * Returns the plugin’s l10n helper.
	 *
	 * @return The plugin’s l10n helper
	 */
	public PluginL10n l10n() {
		return l10n;
	}

	//
	// FREDPLUGIN METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void runPlugin(PluginRespirator pluginRespirator) {
		this.pluginRespirator = pluginRespirator;

		/* create a configuration. */
		Configuration configuration;
		try {
			configuration = new Configuration(new PluginStoreConfigurationBackend(pluginStore = pluginRespirator.getStore()));
		} catch (DatabaseDisabledException dde1) {
			logger.log(Level.WARNING, "Could not load plugin store, using XML files.");
			try {
				configuration = new Configuration(new XMLConfigurationBackend(new File("sone.xml"), true));
			} catch (ConfigurationException ce1) {
				logger.log(Level.SEVERE, "Could not load or create the “sone.xml” configuration file!");
				configuration = new Configuration(new MapConfigurationBackend(Collections.<String, String> emptyMap()));
			}
		}

		/* create freenet interface. */
		FreenetInterface freenetInterface = new FreenetInterface(pluginRespirator.getNode(), pluginRespirator.getHLSimpleClient());

		/* create the web interface. */
		webInterface = new WebInterface(this);

		/* create core. */
		core = new Core();
		core.configuration(configuration);
		core.freenetInterface(freenetInterface);

		/* start core! */
		boolean startupFailed = true;
		try {
			core.start();
			webInterface.start();
			startupFailed = false;
		} finally {
			if (startupFailed) {
				/*
				 * we let the exception bubble up but shut the logging down so
				 * that the logfile is not swamped by the installed logging
				 * handlers of the failed instances.
				 */
				Logging.shutdown();
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void terminate() {
		try {
			/* stop the web interface. */
			webInterface.stop();

			/* stop the core. */
			core.stop();

			/* TODO wait for core to stop? */
			try {
				pluginRespirator.putStore(pluginStore);
			} catch (DatabaseDisabledException dde1) {
				logger.log(Level.WARNING, "Could not store plugin store, database is disabled.", dde1);
			}

		} finally {
			/* shutdown logger. */
			Logging.shutdown();
		}
	}

	//
	// INTERFACE FredPluginL10n
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getString(String key) {
		return l10n.getBase().getString(key);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLanguage(LANGUAGE newLanguage) {
		l10n = new PluginL10n(this, newLanguage);
	}

	//
	// INTERFACE FredPluginBaseL10n
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getL10nFilesBasePath() {
		return "i18n";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getL10nFilesMask() {
		return "sone.${lang}.properties";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getL10nOverrideFilesMask() {
		return "sone.${lang}.override.properties";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public ClassLoader getPluginClassLoader() {
		return SonePlugin.class.getClassLoader();
	}

	//
	// INTERFACE FredPluginVersioned
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getVersion() {
		return VERSION.toString();
	}

}
