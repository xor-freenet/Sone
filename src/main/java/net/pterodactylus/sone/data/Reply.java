/*
 * Sone - Reply.java - Copyright © 2010 David Roden
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

package net.pterodactylus.sone.data;

import java.util.UUID;

/**
 * A reply is like a {@link Post} but can never be posted on its own, it always
 * refers to another {@link Post}.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Reply {

	/** The ID of the reply. */
	private final UUID id;

	/** The Sone that posted this reply. */
	private volatile Sone sone;

	/** The Post this reply refers to. */
	private volatile Post post;

	/** The time of the reply. */
	private volatile long time;

	/** The text of the reply. */
	private volatile String text;

	/**
	 * Creates a new reply.
	 *
	 * @param id
	 *            The ID of the reply
	 */
	public Reply(String id) {
		this(id, null, null, 0, null);
	}

	/**
	 * Creates a new reply.
	 *
	 * @param sone
	 *            The sone that posted the reply
	 * @param post
	 *            The post to reply to
	 * @param text
	 *            The text of the reply
	 */
	public Reply(Sone sone, Post post, String text) {
		this(sone, post, System.currentTimeMillis(), text);
	}

	/**
	 * Creates a new reply-
	 *
	 * @param sone
	 *            The sone that posted the reply
	 * @param post
	 *            The post to reply to
	 * @param time
	 *            The time of the reply
	 * @param text
	 *            The text of the reply
	 */
	public Reply(Sone sone, Post post, long time, String text) {
		this(UUID.randomUUID().toString(), sone, post, time, text);
	}

	/**
	 * Creates a new reply-
	 *
	 * @param sone
	 *            The sone that posted the reply
	 * @param id
	 *            The ID of the reply
	 * @param post
	 *            The post to reply to
	 * @param time
	 *            The time of the reply
	 * @param text
	 *            The text of the reply
	 */
	public Reply(String id, Sone sone, Post post, long time, String text) {
		this.id = UUID.fromString(id);
		this.sone = sone;
		this.post = post;
		this.time = time;
		this.text = text;
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the ID of the reply.
	 *
	 * @return The ID of the reply
	 */
	public String getId() {
		return id.toString();
	}

	/**
	 * Returns the Sone that posted this reply.
	 *
	 * @return The Sone that posted this reply
	 */
	public Sone getSone() {
		return sone;
	}

	/**
	 * Sets the Sone that posted this reply.
	 *
	 * @param sone
	 *            The Sone that posted this reply
	 * @return This reply (for method chaining)
	 */
	public Reply setSone(Sone sone) {
		this.sone = sone;
		return this;
	}

	/**
	 * Returns the post this reply refers to.
	 *
	 * @return The post this reply refers to
	 */
	public Post getPost() {
		return post;
	}

	/**
	 * Sets the post this reply refers to.
	 *
	 * @param post
	 *            The post this reply refers to
	 * @return This reply (for method chaining)
	 */
	public Reply setPost(Post post) {
		this.post = post;
		return this;
	}

	/**
	 * Returns the time of the reply.
	 *
	 * @return The time of the reply (in milliseconds since Jan 1, 1970 UTC)
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Sets the time of this reply.
	 *
	 * @param time
	 *            The time of this reply (in milliseconds since Jan 1, 1970 UTC)
	 * @return This reply (for method chaining)
	 */
	public Reply setTime(long time) {
		this.time = time;
		return this;
	}

	/**
	 * Returns the text of the reply.
	 *
	 * @return The text of the reply
	 */
	public String getText() {
		return text;
	}

	/**
	 * Sets the text of this reply.
	 *
	 * @param text
	 *            The text of this reply
	 * @return This reply (for method chaining)
	 */
	public Reply setText(String text) {
		this.text = text;
		return this;
	}

	//
	// OBJECT METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object object) {
		if (!(object instanceof Reply)) {
			return false;
		}
		Reply reply = (Reply) object;
		return reply.id.equals(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getClass().getName() + "[id=" + id + ",sone=" + sone + ",post=" + post + ",time=" + time + ",text=" + text + "]";
	}

}
