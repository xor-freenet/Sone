/*
 * Sone - DeleteReplysAjaxPage.java - Copyright © 2010 David Roden
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

package net.pterodactylus.sone.web.ajax;

import net.pterodactylus.sone.data.Reply;
import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.web.WebInterface;
import net.pterodactylus.util.json.JsonObject;

/**
 * This AJAX page deletes a reply.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class DeleteReplyAjaxPage extends JsonPage {

	/**
	 * Creates a new AJAX page that deletes a reply.
	 *
	 * @param webInterface
	 *            The Sone web interface
	 */
	public DeleteReplyAjaxPage(WebInterface webInterface) {
		super("ajax/deleteReply.ajax", webInterface);
	}

	//
	// JSONPAGE METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected JsonObject createJsonObject(Request request) {
		String replyId = request.getHttpRequest().getParam("reply");
		Reply reply = webInterface.core().getReply(replyId);
		Sone currentSone = getCurrentSone(request.getToadletContext());
		if (reply == null) {
			return new JsonObject().put("success", false).put("error", "invalid-reply-id");
		}
		if (currentSone == null) {
			return new JsonObject().put("success", false).put("error", "auth-required");
		}
		if (!reply.getSone().equals(currentSone)) {
			return new JsonObject().put("success", false).put("error", "not-authorized");
		}
		webInterface.core().deleteReply(reply);
		return new JsonObject().put("success", true);
	}

}
