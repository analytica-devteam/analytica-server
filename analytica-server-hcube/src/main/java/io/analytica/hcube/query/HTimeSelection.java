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

import io.analytica.hcube.dimension.HTime;
import io.analytica.hcube.dimension.HTimeDimension;
import io.vertigo.kernel.lang.Assertion;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Selection temporelle permettant de d�finir un ensemble de positions sur un niveau temporel donn�.
 * exemple : 
 *  - tous les jours du 15 septembre 2000 au 15 novembre 2000.
 *  - toutes les ann�es de 1914 � 1918 
 * @author npiedeloup, pchretien
 */
final class HTimeSelection {
	private final HTime minTime;
	private final HTime maxTime;

	//	private final TimeDimension dimension;

	HTimeSelection(final HTimeDimension dimension, final Date minDate, final Date maxDate) {
		Assertion.checkNotNull(minDate);
		Assertion.checkNotNull(maxDate);
		Assertion.checkArgument(minDate.equals(maxDate) || minDate.before(maxDate), "la date min doit �tre inf�rieure � la date max");
		Assertion.checkNotNull(dimension);
		//---------------------------------------------------------------------
		minTime = new HTime(minDate, dimension);
		maxTime = new HTime(maxDate, dimension);
	}

	List<HTime> getAllTimes() {
		final List<HTime> times = new ArrayList<>();
		//On pr�pare les bornes de temps
		int loops = 0;
		HTime currentTime = minTime;
		do {
			times.add(currentTime);
			//---------------
			currentTime = currentTime.next();
			loops++;
			if (loops > 1000) {
				throw new RuntimeException("time range is too large : more than 1000 positions");
			}
		} while (currentTime.inMillis() < maxTime.inMillis());

		return times;
	}

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return "{ from:" + minTime + ", to:" + maxTime + " }";
	}
}
