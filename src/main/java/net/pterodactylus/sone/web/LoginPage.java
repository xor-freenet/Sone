/*
 * FreenetSone - LoginPage.java - Copyright © 2010 David Roden
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

package net.pterodactylus.sone.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.pterodactylus.sone.data.Sone;
import net.pterodactylus.sone.template.SoneAccessor;
import net.pterodactylus.sone.web.page.Page.Request.Method;
import net.pterodactylus.util.template.Template;
import freenet.clients.http.ToadletContext;

/**
 * The login page manages logging the user in.
 *
 * @author <a href="mailto:bombe@pterodactylus.net">David ‘Bombe’ Roden</a>
 */
public class LoginPage extends SoneTemplatePage {

	/**
	 * Creates a new login page.
	 *
	 * @param template
	 *            The template to render
	 * @param webInterface
	 *            The Sone web interface
	 */
	public LoginPage(Template template, WebInterface webInterface) {
		super("login.html", template, "Page.Login.Title", webInterface, false);
	}

	//
	// TEMPLATEPAGE METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void processTemplate(Request request, Template template) throws RedirectException {
		super.processTemplate(request, template);
		List<Sone> localSones = new ArrayList<Sone>(webInterface.core().getSones());
		Collections.sort(localSones, new Comparator<Sone>() {

			@Override
			public int compare(Sone leftSone, Sone rightSone) {
				int diff = SoneAccessor.getNiceName(leftSone).compareToIgnoreCase(SoneAccessor.getNiceName(rightSone));
				if (diff != 0) {
					return diff;
				}
				return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, rightSone.getTime() - leftSone.getTime()));
			}

		});
		template.set("sones", localSones);
		if (request.getMethod() == Method.POST) {
			String soneId = request.getHttpRequest().getPartAsStringFailsafe("sone-id", 100);
			Sone selectedSone = null;
			for (Sone sone : webInterface.core().getSones()) {
				if (sone.getId().equals(soneId)) {
					selectedSone = sone;
					break;
				}
			}
			if (selectedSone != null) {
				setCurrentSone(request.getToadletContext(), selectedSone);
				throw new RedirectException("index.html");
			}
		}
	}

	//
	// SONETEMPLATEPAGE METHODS
	//

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isEnabled(ToadletContext toadletContext) {
		return getCurrentSone(toadletContext) == null;
	}

}
