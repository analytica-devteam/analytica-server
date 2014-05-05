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

import io.analytica.hcube.HCubeStore;
import io.analytica.hcube.dimension.HCategory;
import io.analytica.hcube.dimension.HTime;
import io.vertigo.kernel.lang.Assertion;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Requ�te permettant de d�finir les zones de s�lections sur les diff�rents axes du cube.
 * Cette requ�te doit �tre construite avec QueryBuilder.
 * Cette Requ�te est descriptive, elle peut fonctionner avec des dates absolues ou relatives via la notion NOW. 
 * @author npiedeloup, pchretien
 */
public final class HQuery {
	private final HTimeSelection timeSelection;
	private final HCategorySelection categorySelection;

	HQuery(final HTimeSelection timeSelection, final HCategorySelection categorySelection) {
		Assertion.checkNotNull(timeSelection);
		Assertion.checkNotNull(categorySelection);
		//---------------------------------------------------------------------
		this.timeSelection = timeSelection;
		this.categorySelection = categorySelection;
	}

	//-----------------------What----------------------------------------------
	/**
	 * Liste tri�e par ordre alphab�tique des cat�gories matchant la s�lection
	 * @return
	 */
	//A d�placer 
	//A d�placer 
	//A d�placer 
	//A d�placer 
	//A d�placer 
	//A d�placer 
	//A d�placer 
	public Set<HCategory> getAllCategories(final String appName, final HCubeStore cubeStore) {
		Assertion.checkNotNull(cubeStore);
		// ---------------------------------------------------------------------
		if (categorySelection.hasChildren()) {
			return cubeStore.getAllSubCategories(appName, categorySelection.getCategory());
		}
		return Collections.singleton(categorySelection.getCategory());
	}

	//-----------------------When----------------------------------------------
	public List<HTime> getAllTimes() {
		return timeSelection.getAllTimes();
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "{timeSelection :" + timeSelection + ", categorySelection :" + categorySelection + "}";
	}
}
