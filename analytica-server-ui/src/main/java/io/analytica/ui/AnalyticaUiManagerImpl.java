/**
 * Analytica - beta version - Systems Monitoring Tool
 *
 * Copyright (C) 2013, KleeGroup, direction.technique@kleegroup.com (http://www.kleegroup.com)
 * KleeGroup, Centre d'affaire la Boursidiére - BP 159 - 92357 Le Plessis Robinson Cedex - France
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation;
 * either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, see <http://www.gnu.org/licenses>
 */
package io.analytica.ui;

import io.analytica.restserver.RestServerManager;

import javax.inject.Inject;

/**
 * Implémentation du tableau de bord.
 * 
 * @author pchretien
 * @version $Id: SpacesManagerImpl.java,v 1.2 2013/05/15 17:35:48 pchretien Exp $
 */
public final class AnalyticaUiManagerImpl implements AnalyticaUiManager {

	/**
	 * Constructeur simple pour instanciation par jersey.
	 * @param restServerManager Manager de server Rest
	 */
	@Inject
	public AnalyticaUiManagerImpl(final RestServerManager restServerManager) {
		restServerManager.addStaticPath("/static/", "/static/");
		restServerManager.addStaticPath("/pages/", "/");
	}

}
