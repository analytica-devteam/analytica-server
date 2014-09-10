package io.analytica.hcube.impl;

import io.analytica.hcube.dimension.HTime;
import io.analytica.hcube.query.HTimeSelection;
import io.vertigo.core.lang.Assertion;

import java.util.ArrayList;
import java.util.List;

final class HTimeSelector {
	/** {@inheritDoc} */
	public List<HTime> findTimes(final HTimeSelection timeSelection) {
		Assertion.checkNotNull(timeSelection);
		//---------------------------------------------------------------------
		final List<HTime> times = new ArrayList<>();
		//On pr�pare les bornes de temps
		int loops = 0;
		HTime currentTime = timeSelection.getMinTime();
		do {
			times.add(currentTime);
			//---------------
			currentTime = currentTime.next();
			loops++;
			if (loops > 1000) {
				throw new RuntimeException("time range is too large : more than 1000 positions");
			}
		} while (currentTime.inMillis() < timeSelection.getMaxTime().inMillis());

		return times;
	}

}