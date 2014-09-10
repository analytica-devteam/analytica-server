/**
 * Analytica - beta version - Systems Monitoring Tool
 *
 * Copyright (C) 2013, KleeGroup, direction.technique@kleegroup.com (http://www.kleegroup.com)
 * KleeGroup, Centre d'affaire la Boursidi�re - BP 159 - 92357 Le Plessis Robinson Cedex - France
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
package io.analytica.hcube.query;

import io.vertigo.core.lang.Assertion;

/**
 * Requ�te permettant de d�finir les zones de s�lections sur les diff�rents axes du cube.
 * Cette requ�te doit �tre construite avec QueryBuilder.
 * Cette Requ�te est descriptive, elle peut fonctionner avec des dates absolues ou relatives via la notion NOW.
 * @author npiedeloup, pchretien
 */
public final class HQuery {
	private final String type;
	private final HTimeSelection timeSelection;
	private final HCategorySelection categorySelection;

	HQuery(final String type, final HTimeSelection timeSelection, final HCategorySelection categorySelection) {
		Assertion.checkArgNotEmpty(type);
		Assertion.checkNotNull(timeSelection);
		Assertion.checkNotNull(categorySelection);
		//---------------------------------------------------------------------
		this.type = type;
		this.timeSelection = timeSelection;
		this.categorySelection = categorySelection;
	}

	public String getType() {
		return type;
	}

	public HCategorySelection getCategorySelection() {
		return categorySelection;
	}

	public HTimeSelection getTimeSelection() {
		return timeSelection;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "{timeSelection :" + timeSelection + ", categorySelection :" + categorySelection + "}";
	}
}