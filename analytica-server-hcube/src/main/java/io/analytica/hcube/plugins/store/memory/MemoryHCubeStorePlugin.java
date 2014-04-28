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
package io.analytica.hcube.plugins.store.memory;

import io.analytica.hcube.cube.HCube;
import io.analytica.hcube.dimension.HCategory;
import io.analytica.hcube.impl.HCubeStorePlugin;
import io.analytica.hcube.query.HQuery;
import io.analytica.hcube.result.HSerie;
import io.vertigo.kernel.lang.Assertion;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Impl�mentation m�moire du stockage des Cubes.
 * 
 * @author npiedeloup, pchretien
 */
public final class MemoryHCubeStorePlugin implements HCubeStorePlugin {
	private static final AppCubeStore EMPTY = new AppCubeStore("EMPTY");
	private final Map<String, AppCubeStore> appCubeStores = new HashMap<>();

	/** {@inheritDoc} */
	public synchronized void merge(String appName, final HCube cube) {
		Assertion.checkArgNotEmpty(appName);
		//---------------------------------------------------------------------
		AppCubeStore appCubeStore = appCubeStores.get(appName);
		if (appCubeStore == null) {
			appCubeStore = new AppCubeStore(appName);
			appCubeStores.put(appName, appCubeStore);
		}
		appCubeStore.addCategory(cube.getKey().getCategory());
		appCubeStore.merge(cube);
	}

	/** {@inheritDoc} */
	public synchronized Map<HCategory, HSerie> findAll(final String appName, final HQuery query) {
		final AppCubeStore appCubeStore = appCubeStores.get(appName);
		if (appCubeStore == null) {
			return EMPTY.findAll(query, this);
		}
		return appCubeStore.findAll(query, this);
	}

	/** {@inheritDoc} */
	public synchronized Set<HCategory> getAllRootCategories(String appName) {
		Assertion.checkArgNotEmpty(appName);
		//---------------------------------------------------------------------
		final AppCubeStore appCubeStore = appCubeStores.get(appName);
		if (appCubeStore == null) {
			return Collections.emptySet();
		}
		return appCubeStore.getAllRootCategories();
	}

	/** {@inheritDoc} */
	public synchronized Set<HCategory> getAllSubCategories(String appName, HCategory category) {
		Assertion.checkArgNotEmpty(appName);
		Assertion.checkNotNull(category);
		//---------------------------------------------------------------------
		final AppCubeStore appCubeStore = appCubeStores.get(appName);
		if (appCubeStore == null) {
			return Collections.emptySet();
		}
		return appCubeStore.getAllSubCategories(category);
	}

}