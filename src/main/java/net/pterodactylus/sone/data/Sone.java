/*
 * FreenetSone - Sone.java - Copyright © 2010 David Roden
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.pterodactylus.sone.template.SoneAccessor;
import net.pterodactylus.util.logging.Logging;
import freenet.keys.FreenetURI;

/**
 * A Sone defines everything about a user: her profile, her status updates, her
 * replies, her likes and dislikes, etc.
 * <p>
 * Operations that modify the Sone need to synchronize on the Sone in question.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class Sone {

	/** The logger. */
	private static final Logger logger = Logging.getLogger(Sone.class);

	/** A GUID for this Sone. */
	private final UUID id;

	/** The name of this Sone. */
	private volatile String name;

	/** The URI under which the Sone is stored in Freenet. */
	private volatile FreenetURI requestUri;

	/** The URI used to insert a new version of this Sone. */
	/* This will be null for remote Sones! */
	private volatile FreenetURI insertUri;

	/** The time of the last inserted update. */
	private volatile long time;

	/** The profile of this Sone. */
	private volatile Profile profile;

	/** All friend Sones. */
	private final Set<Sone> friendSones = Collections.synchronizedSet(new HashSet<Sone>());

	/** All posts. */
	private final Set<Post> posts = Collections.synchronizedSet(new HashSet<Post>());

	/** All replies. */
	private final Set<Reply> replies = Collections.synchronizedSet(new HashSet<Reply>());

	/** The IDs of all blocked Sones. */
	private final Set<String> blockedSoneIds = Collections.synchronizedSet(new HashSet<String>());

	/** The IDs of all liked posts. */
	private final Set<String> likedPostIds = Collections.synchronizedSet(new HashSet<String>());

	/** The IDs of all liked replies. */
	private final Set<String> likedReplyIds = Collections.synchronizedSet(new HashSet<String>());

	/** Modification count. */
	private volatile long modificationCounter = 0;

	/**
	 * Creates a new Sone.
	 *
	 * @param id
	 *            The ID of this Sone
	 */
	public Sone(String id) {
		this.id = UUID.fromString(id);
	}

	//
	// ACCESSORS
	//

	/**
	 * Returns the ID of this Sone.
	 *
	 * @return The ID of this Sone
	 */
	public String getId() {
		return id.toString();
	}

	/**
	 * Returns the name of this Sone.
	 *
	 * @return The name of this Sone
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the name of this Sone.
	 *
	 * @param name
	 *            The name of this Sone
	 * @return This sone (for method chaining)
	 */
	public Sone setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Returns the request URI of this Sone.
	 *
	 * @return The request URI of this Sone
	 */
	public FreenetURI getRequestUri() {
		return requestUri;
	}

	/**
	 * Sets the request URI of this Sone.
	 *
	 * @param requestUri
	 *            The request URI of this Sone
	 * @return This Sone (for method chaining)
	 */
	public Sone setRequestUri(FreenetURI requestUri) {
		if (this.requestUri == null) {
			this.requestUri = requestUri;
			updateEditions();
			return this;
		}
		if (!this.requestUri.equalsKeypair(requestUri)) {
			logger.log(Level.WARNING, "Request URI %s tried to overwrite %s!", new Object[] { requestUri, this.requestUri });
			return this;
		}
		long latestEdition = requestUri.getEdition();
		if ((latestEdition > this.requestUri.getEdition()) || (latestEdition > this.requestUri.getSuggestedEdition())) {
			this.requestUri.setSuggestedEdition(latestEdition);
		}
		return this;
	}

	/**
	 * Returns the insert URI of this Sone.
	 *
	 * @return The insert URI of this Sone
	 */
	public FreenetURI getInsertUri() {
		return insertUri;
	}

	/**
	 * Sets the insert URI of this Sone.
	 *
	 * @param insertUri
	 *            The insert URI of this Sone
	 * @return This Sone (for method chaining)
	 */
	public Sone setInsertUri(FreenetURI insertUri) {
		if (this.insertUri == null) {
			this.insertUri = insertUri;
			updateEditions();
			return this;
		}
		if (!this.insertUri.equalsKeypair(insertUri)) {
			logger.log(Level.WARNING, "Request URI %s tried to overwrite %s!", new Object[] { insertUri, this.insertUri });
			return this;
		}
		long latestEdition = insertUri.getEdition();
		if ((latestEdition > this.insertUri.getEdition()) || (latestEdition > this.insertUri.getSuggestedEdition())) {
			this.insertUri.setSuggestedEdition(latestEdition);
		}
		return this;
	}

	/**
	 * Return the time of the last inserted update of this Sone.
	 *
	 * @return The time of the update (in milliseconds since Jan 1, 1970 UTC)
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Sets the time of the last inserted update of this Sone.
	 *
	 * @param time
	 *            The time of the update (in milliseconds since Jan 1, 1970 UTC)
	 * @return This Sone (for method chaining)
	 */
	public Sone setTime(long time) {
		this.time = time;
		return this;
	}

	/**
	 * Returns a copy of the profile. If you want to update values in the
	 * profile of this Sone, update the values in the returned {@link Profile}
	 * and use {@link #setProfile(Profile)} to change the profile in this Sone.
	 *
	 * @return A copy of the profile
	 */
	public Profile getProfile() {
		return new Profile(profile);
	}

	/**
	 * Sets the profile of this Sone. A copy of the given profile is stored so
	 * that subsequent modifications of the given profile are not reflected in
	 * this Sone!
	 *
	 * @param profile
	 *            The profile to set
	 */
	public synchronized void setProfile(Profile profile) {
		this.profile = new Profile(profile);
		modificationCounter++;
	}

	/**
	 * Returns all friend Sones of this Sone.
	 *
	 * @return The friend Sones of this Sone
	 */
	public List<Sone> getFriends() {
		List<Sone> friends = new ArrayList<Sone>(friendSones);
		Collections.sort(friends, new Comparator<Sone>() {

			@Override
			public int compare(Sone leftSone, Sone rightSone) {
				int diff = SoneAccessor.getNiceName(leftSone).compareToIgnoreCase(SoneAccessor.getNiceName(rightSone));
				if (diff != 0) {
					return diff;
				}
				return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rightSone.getTime() - leftSone.getTime()));
			}
		});
		return friends;
	}

	/**
	 * Sets all friends of this Sone at once.
	 *
	 * @param friends
	 *            The new (and only) friends of this Sone
	 * @return This Sone (for method chaining)
	 */
	public Sone setFriends(Collection<Sone> friends) {
		friendSones.clear();
		friendSones.addAll(friends);
		return this;
	}

	/**
	 * Returns whether this Sone has the given Sone as a friend Sone.
	 *
	 * @param friendSone
	 *            The friend Sone to check for
	 * @return {@code true} if this Sone has the given Sone as a friend,
	 *         {@code false} otherwise
	 */
	public boolean hasFriend(Sone friendSone) {
		return friendSones.contains(friendSone);
	}

	/**
	 * Adds the given Sone as a friend Sone.
	 *
	 * @param friendSone
	 *            The friend Sone to add
	 * @return This Sone (for method chaining)
	 */
	public Sone addFriend(Sone friendSone) {
		if (!friendSone.equals(this)) {
			friendSones.add(friendSone);
		}
		return this;
	}

	/**
	 * Removes the given Sone as a friend Sone.
	 *
	 * @param friendSone
	 *            The friend Sone to remove
	 * @return This Sone (for method chaining)
	 */
	public Sone removeFriend(Sone friendSone) {
		friendSones.remove(friendSone);
		return this;
	}

	/**
	 * Returns the list of posts of this Sone, sorted by time, newest first.
	 *
	 * @return All posts of this Sone
	 */
	public List<Post> getPosts() {
		List<Post> sortedPosts = new ArrayList<Post>(posts);
		Collections.sort(sortedPosts, new Comparator<Post>() {

			@Override
			public int compare(Post leftPost, Post rightPost) {
				return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rightPost.getTime() - leftPost.getTime()));
			}

		});
		return sortedPosts;
	}

	/**
	 * Sets all posts of this Sone at once.
	 *
	 * @param posts
	 *            The new (and only) posts of this Sone
	 * @return This Sone (for method chaining)
	 */
	public synchronized Sone setPosts(Collection<Post> posts) {
		this.posts.clear();
		this.posts.addAll(posts);
		modificationCounter++;
		return this;
	}

	/**
	 * Adds the given post to this Sone. The post will not be added if its
	 * {@link Post#getSone() Sone} is not this Sone.
	 *
	 * @param post
	 *            The post to add
	 */
	public synchronized void addPost(Post post) {
		if (post.getSone().equals(this) && posts.add(post)) {
			logger.log(Level.FINEST, "Adding %s to “%s”.", new Object[] { post, getName() });
			modificationCounter++;
		}
	}

	/**
	 * Removes the given post from this Sone.
	 *
	 * @param post
	 *            The post to remove
	 */
	public synchronized void removePost(Post post) {
		if (post.getSone().equals(this) && posts.remove(post)) {
			modificationCounter++;
		}
	}

	/**
	 * Returns all replies this Sone made.
	 *
	 * @return All replies this Sone made
	 */
	public Set<Reply> getReplies() {
		logger.log(Level.FINEST, "Friends of %s: %s", new Object[] { this, friendSones });
		return Collections.unmodifiableSet(replies);
	}

	/**
	 * Sets all replies of this Sone at once.
	 *
	 * @param replies
	 *            The new (and only) replies of this Sone
	 * @return This Sone (for method chaining)
	 */
	public synchronized Sone setReplies(Collection<Reply> replies) {
		this.replies.clear();
		this.replies.addAll(replies);
		modificationCounter++;
		return this;
	}

	/**
	 * Adds a reply to this Sone. If the given reply was not made by this Sone,
	 * nothing is added to this Sone.
	 *
	 * @param reply
	 *            The reply to add
	 */
	public synchronized void addReply(Reply reply) {
		if (reply.getSone().equals(this) && replies.add(reply)) {
			modificationCounter++;
		}
	}

	/**
	 * Removes a reply from this Sone.
	 *
	 * @param reply
	 *            The reply to remove
	 */
	public synchronized void removeReply(Reply reply) {
		if (reply.getSone().equals(this) && replies.remove(reply)) {
			modificationCounter++;
		}
	}

	/**
	 * Returns the IDs of all blocked Sones. These Sones will not propagated
	 * using the “known Sones” mechanism.
	 *
	 * @return The IDs of all blocked Sones
	 */
	public Set<String> getBlockedSoneIds() {
		return Collections.unmodifiableSet(blockedSoneIds);
	}

	/**
	 * Returns whether the given Sone ID is blocked.
	 *
	 * @param soneId
	 *            The Sone ID to check
	 * @return {@code true} if the given Sone ID is blocked, {@code false}
	 *         otherwise
	 */
	public boolean isSoneBlocked(String soneId) {
		return blockedSoneIds.contains(soneId);
	}

	/**
	 * Adds the given ID to the list of blocked IDs.
	 *
	 * @param soneId
	 *            The Sone ID to block
	 */
	public synchronized void addBlockedSoneId(String soneId) {
		if (blockedSoneIds.add(soneId)) {
			modificationCounter++;
		}
	}

	/**
	 * Removes the given ID from the list of blocked IDs.
	 *
	 * @param soneId
	 *            The Sone ID to unblock
	 */
	public synchronized void removeBlockedSoneId(String soneId) {
		if (blockedSoneIds.remove(soneId)) {
			modificationCounter++;
		}
	}

	/**
	 * Returns the IDs of all liked posts.
	 *
	 * @return All liked posts’ IDs
	 */
	public Set<String> getLikedPostIds() {
		return Collections.unmodifiableSet(likedPostIds);
	}

	/**
	 * Sets the IDs of all liked posts.
	 *
	 * @param likedPostIds
	 *            All liked posts’ IDs
	 * @return This Sone (for method chaining)
	 */
	public synchronized Sone setLikePostIds(Set<String> likedPostIds) {
		this.likedPostIds.clear();
		this.likedPostIds.addAll(likedPostIds);
		modificationCounter++;
		return this;
	}

	/**
	 * Checks whether the given post ID is liked by this Sone.
	 *
	 * @param postId
	 *            The ID of the post
	 * @return {@code true} if this Sone likes the given post, {@code false}
	 *         otherwise
	 */
	public boolean isLikedPostId(String postId) {
		return likedPostIds.contains(postId);
	}

	/**
	 * Adds the given post ID to the list of posts this Sone likes.
	 *
	 * @param postId
	 *            The ID of the post
	 * @return This Sone (for method chaining)
	 */
	public synchronized Sone addLikedPostId(String postId) {
		if (likedPostIds.add(postId)) {
			modificationCounter++;
		}
		return this;
	}

	/**
	 * Removes the given post ID from the list of posts this Sone likes.
	 *
	 * @param postId
	 *            The ID of the post
	 * @return This Sone (for method chaining)
	 */
	public synchronized Sone removeLikedPostId(String postId) {
		if (likedPostIds.remove(postId)) {
			modificationCounter++;
		}
		return this;
	}

	/**
	 * Returns the IDs of all liked replies.
	 *
	 * @return All liked replies’ IDs
	 */
	public Set<String> getLikedReplyIds() {
		return Collections.unmodifiableSet(likedReplyIds);
	}

	/**
	 * Sets the IDs of all liked replies.
	 *
	 * @param likedReplyIds
	 *            All liked replies’ IDs
	 * @return This Sone (for method chaining)
	 */
	public synchronized Sone setLikeReplyIds(Set<String> likedReplyIds) {
		this.likedReplyIds.clear();
		this.likedReplyIds.addAll(likedReplyIds);
		modificationCounter++;
		return this;
	}

	/**
	 * Checks whether the given reply ID is liked by this Sone.
	 *
	 * @param replyId
	 *            The ID of the reply
	 * @return {@code true} if this Sone likes the given reply, {@code false}
	 *         otherwise
	 */
	public boolean isLikedReplyId(String replyId) {
		return likedReplyIds.contains(replyId);
	}

	/**
	 * Adds the given reply ID to the list of replies this Sone likes.
	 *
	 * @param replyId
	 *            The ID of the reply
	 * @return This Sone (for method chaining)
	 */
	public synchronized Sone addLikedReplyId(String replyId) {
		if (likedReplyIds.add(replyId)) {
			modificationCounter++;
		}
		return this;
	}

	/**
	 * Removes the given post ID from the list of replies this Sone likes.
	 *
	 * @param replyId
	 *            The ID of the reply
	 * @return This Sone (for method chaining)
	 */
	public synchronized Sone removeLikedReplyId(String replyId) {
		if (likedReplyIds.remove(replyId)) {
			modificationCounter++;
		}
		return this;
	}

	/**
	 * Returns the modification counter.
	 *
	 * @return The modification counter
	 */
	public synchronized long getModificationCounter() {
		return modificationCounter;
	}

	/**
	 * Sets the modification counter.
	 *
	 * @param modificationCounter
	 *            The new modification counter
	 */
	public synchronized void setModificationCounter(long modificationCounter) {
		this.modificationCounter = modificationCounter;
	}

	/**
	 * Updates the suggested edition in both the request URI and the insert URI.
	 *
	 * @param latestEdition
	 *            The latest edition to update the URIs to
	 */
	public void updateUris(long latestEdition) {
		if ((requestUri != null) && (requestUri.getEdition() < latestEdition)) {
			requestUri = requestUri.setSuggestedEdition(latestEdition);
		}
		if ((insertUri != null) && (insertUri.getEdition() < latestEdition)) {
			insertUri = insertUri.setSuggestedEdition(latestEdition);
		}
	}

	//
	// PRIVATE METHODS
	//

	/**
	 * Updates the editions of the request URI and the insert URI (if latter is
	 * not {@code null}) with the greater edition of either one.
	 */
	private void updateEditions() {
		long requestEdition = 0;
		if (requestUri != null) {
			requestEdition = requestUri.getEdition();
		}
		long insertEdition = 0;
		if (insertUri != null) {
			insertEdition = insertUri.getEdition();
		}
		updateUris(Math.max(requestEdition, insertEdition));
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
		if (!(object instanceof Sone)) {
			return false;
		}
		return ((Sone) object).id.equals(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return getClass().getName() + "[id=" + id + ",name=" + name + ",requestUri=" + requestUri + ",insertUri=" + insertUri + ",friends(" + friendSones.size() + "),posts(" + posts.size() + "),replies(" + replies.size() + ")]";
	}

}
