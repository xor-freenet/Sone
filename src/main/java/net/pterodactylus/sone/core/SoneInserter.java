/*
 * FreenetSone - SoneInserter.java - Copyright © 2010 David Roden
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

package net.pterodactylus.sone.core;

import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.pterodactylus.sone.core.Core.SoneStatus;
import net.pterodactylus.sone.data.Post;
import net.pterodactylus.sone.data.Reply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.freenet.StringBucket;
import net.pterodactylus.util.filter.Filter;
import net.pterodactylus.util.filter.Filters;
import net.pterodactylus.util.io.Closer;
import net.pterodactylus.util.logging.Logging;
import net.pterodactylus.util.service.AbstractService;
import net.pterodactylus.util.template.DefaultTemplateFactory;
import net.pterodactylus.util.template.ReflectionAccessor;
import net.pterodactylus.util.template.Template;
import net.pterodactylus.util.template.TemplateException;
import net.pterodactylus.util.template.XmlFilter;
import freenet.client.async.ManifestElement;
import freenet.keys.FreenetURI;

/**
 * A Sone inserter is responsible for inserting a Sone if it has changed.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class SoneInserter extends AbstractService {

	/** The logger. */
	private static final Logger logger = Logging.getLogger(SoneInserter.class);

	/** The insertion delay (in seconds). */
	private static volatile int insertionDelay = 60;

	/** The template factory used to create the templates. */
	private static final DefaultTemplateFactory templateFactory = new DefaultTemplateFactory();

	static {
		templateFactory.addAccessor(Object.class, new ReflectionAccessor());
		templateFactory.addFilter("xml", new XmlFilter());
	}

	/** The UTF-8 charset. */
	private static final Charset utf8Charset = Charset.forName("UTF-8");

	/** The core. */
	private final Core core;

	/** The Freenet interface. */
	private final FreenetInterface freenetInterface;

	/** The Sone to insert. */
	private final Sone sone;

	/**
	 * Creates a new Sone inserter.
	 *
	 * @param core
	 *            The core
	 * @param freenetInterface
	 *            The freenet interface
	 * @param sone
	 *            The Sone to insert
	 */
	public SoneInserter(Core core, FreenetInterface freenetInterface, Sone sone) {
		super("Sone Inserter for “" + sone.getName() + "”", false);
		this.core = core;
		this.freenetInterface = freenetInterface;
		this.sone = sone;
	}

	//
	// ACCESSORS
	//

	/**
	 * Changes the insertion delay, i.e. the time the Sone inserter waits after
	 * it has noticed a Sone modification before it starts the insert.
	 *
	 * @param insertionDelay
	 *            The insertion delay (in seconds)
	 */
	public static void setInsertionDelay(int insertionDelay) {
		SoneInserter.insertionDelay = insertionDelay;
	}

	//
	// SERVICE METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void serviceRun() {
		long modificationCounter = 0;
		long lastModificationTime = 0;
		while (!shouldStop()) {
			/* check every seconds. */
			sleep(1000);

			InsertInformation insertInformation = null;
			synchronized (sone) {
				if (sone.getModificationCounter() > modificationCounter) {
					modificationCounter = sone.getModificationCounter();
					lastModificationTime = System.currentTimeMillis();
					sone.setTime(lastModificationTime);
					logger.log(Level.FINE, "Sone %s has been modified, waiting %d seconds before inserting.", new Object[] { sone.getName(), insertionDelay });
				}
				if ((lastModificationTime > 0) && ((System.currentTimeMillis() - lastModificationTime) > (insertionDelay * 1000))) {
					insertInformation = new InsertInformation(sone);
				}
			}

			if (insertInformation != null) {
				logger.log(Level.INFO, "Inserting Sone “%s”…", new Object[] { sone.getName() });

				boolean success = false;
				try {
					core.setSoneStatus(sone, SoneStatus.inserting);
					FreenetURI finalUri = freenetInterface.insertDirectory(insertInformation.getInsertUri().setKeyType("USK").setSuggestedEdition(0), insertInformation.generateManifestEntries(), "index.html");
					sone.updateUris(finalUri.getEdition());
					success = true;
					logger.log(Level.INFO, "Inserted Sone “%s” at %s.", new Object[] { sone.getName(), finalUri });
				} catch (SoneException se1) {
					logger.log(Level.WARNING, "Could not insert Sone “" + sone.getName() + "”!", se1);
				} finally {
					core.setSoneStatus(sone, SoneStatus.idle);
				}

				/*
				 * reset modification counter if Sone has not been modified
				 * while it was inserted.
				 */
				if (success) {
					synchronized (sone) {
						if (sone.getModificationCounter() == modificationCounter) {
							logger.log(Level.FINE, "Sone “%s” was not modified further, resetting counter…", new Object[] { sone });
							sone.setModificationCounter(0);
							modificationCounter = 0;
							lastModificationTime = 0;
						}
					}
				}
			}
		}
	}

	/**
	 * Container for information that are required to insert a Sone. This
	 * container merely exists to copy all relevant data without holding a lock
	 * on the {@link Sone} object for too long.
	 *
	 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
	 */
	private class InsertInformation {

		/** All properties of the Sone, copied for thread safety. */
		private final Map<String, Object> soneProperties = new HashMap<String, Object>();

		/**
		 * Creates a new insert information container.
		 *
		 * @param sone
		 *            The sone to insert
		 */
		public InsertInformation(Sone sone) {
			soneProperties.put("id", sone.getId());
			soneProperties.put("name", sone.getName());
			soneProperties.put("time", sone.getTime());
			soneProperties.put("requestUri", sone.getRequestUri());
			soneProperties.put("insertUri", sone.getInsertUri());
			soneProperties.put("profile", sone.getProfile());
			soneProperties.put("posts", new ArrayList<Post>(sone.getPosts()));
			soneProperties.put("replies", new HashSet<Reply>(sone.getReplies()));
			soneProperties.put("blockedSoneIds", new HashSet<String>(sone.getBlockedSoneIds()));
			soneProperties.put("likedPostIds", new HashSet<String>(sone.getLikedPostIds()));
			soneProperties.put("likeReplyIds", new HashSet<String>(sone.getLikedReplyIds()));
		}

		//
		// ACCESSORS
		//

		/**
		 * Returns the insert URI of the Sone.
		 *
		 * @return The insert URI of the Sone
		 */
		public FreenetURI getInsertUri() {
			return (FreenetURI) soneProperties.get("insertUri");
		}

		//
		// ACTIONS
		//

		/**
		 * Generates all manifest entries required to insert this Sone.
		 *
		 * @return The manifest entries for the Sone insert
		 */
		public HashMap<String, Object> generateManifestEntries() {
			HashMap<String, Object> manifestEntries = new HashMap<String, Object>();

			/* first, create an index.html. */
			manifestEntries.put("index.html", createManifestElement("index.html", "text/html; charset=utf-8", "/templates/insert/index.html"));

			/* now, store the sone. */
			manifestEntries.put("sone.xml", createManifestElement("sone.xml", "text/xml; charset=utf-8", "/templates/insert/sone.xml"));

			return manifestEntries;
		}

		//
		// PRIVATE METHODS
		//

		/**
		 * Creates a new manifest element.
		 *
		 * @param name
		 *            The name of the file
		 * @param contentType
		 *            The content type of the file
		 * @param templateName
		 *            The name of the template to render
		 * @return The manifest element
		 */
		@SuppressWarnings("synthetic-access")
		private ManifestElement createManifestElement(String name, String contentType, String templateName) {
			InputStreamReader templateInputStreamReader;
			Template template = templateFactory.createTemplate(templateInputStreamReader = new InputStreamReader(getClass().getResourceAsStream(templateName), utf8Charset));
			try {
				template.parse();
			} catch (TemplateException te1) {
				logger.log(Level.SEVERE, "Could not parse template “" + templateName + "”!", te1);
				return null;
			} finally {
				Closer.close(templateInputStreamReader);
			}
			@SuppressWarnings("unchecked")
			final Set<String> blockedSoneIds = (Set<String>) soneProperties.get("blockedSoneIds");
			Collection<Sone> knownSones = Filters.filteredCollection(core.getKnownSones(), new Filter<Sone>() {

				/**
				 * {@inheritDoc}
				 */
				@Override
				public boolean filterObject(Sone object) {
					return !blockedSoneIds.contains(object.getId()) && !object.getId().equals(soneProperties.get("id"));
				}
			});

			template.set("currentSone", soneProperties);
			template.set("knownSones", knownSones);
			StringWriter writer = new StringWriter();
			StringBucket bucket = null;
			try {
				template.render(writer);
				bucket = new StringBucket(writer.toString(), utf8Charset);
				return new ManifestElement(name, bucket, contentType, bucket.size());
			} catch (TemplateException te1) {
				logger.log(Level.SEVERE, "Could not render template “" + templateName + "”!", te1);
				return null;
			} finally {
				Closer.close(writer);
				if (bucket != null) {
					bucket.free();
				}
			}
		}

	}

}
