package io.analytica.hcube.result;

import io.analytica.hcube.cube.HCube;
import io.analytica.hcube.cube.HMetric;
import io.analytica.hcube.cube.HMetricBuilder;
import io.analytica.hcube.cube.HMetricKey;
import io.analytica.hcube.cube.HVirtualCube;
import io.analytica.hcube.dimension.HCategory;
import io.vertigo.kernel.lang.Assertion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * R�sultat d'une s�rie.
 * Une s�rie est un parall�l�pip�de continu pour une cat�gorie donn�e, qui peut se voir comme autant un cube virtuel.
 * 
 * @author pchretien, npiedeloup
 * @version $Id: ServerManager.java,v 1.8 2012/09/14 15:04:13 pchretien Exp $
 */
public final class HSerie implements HVirtualCube {
	private final HCategory category;
	private final List<HCube> cubes;
	private Map<HMetricKey, HMetric> metrics; //lazy

	/**
	 * Constructeur.
	 * @param category Cat�gorie de la s�rie
	 * @param cubes Liste ordonn�e des �lements du parall�l�pip�de
	 */
	public HSerie(final HCategory category, final List<HCube> cubes) {
		Assertion.checkNotNull(category);
		Assertion.checkNotNull(cubes);
		//---------------------------------------------------------------------
		this.category = category;
		this.cubes = cubes;
	}

	/**
	 * @return Category de la s�rie
	 */
	public HCategory getCategory() {
		return category;
	}

	/**
	 * @return Liste ordonn�e des �lements du parall�l�pip�de
	 */
	public List<HCube> getCubes() {
		Assertion.checkNotNull(category);
		//-------------------------------------------------------------------------
		return Collections.unmodifiableList(cubes);
	}

	//-------------------------------------------------------------------------
	private Map<HMetricKey, HMetric> getLazyMetrics() {
		if (metrics == null) {
			final Map<HMetricKey, HMetricBuilder> metricBuilders = new HashMap<>();
			for (final HCube cube : cubes) {
				for (final HMetric metric : cube.getMetrics()) {
					HMetricBuilder metricBuilder = metricBuilders.get(metric.getKey());
					if (metricBuilder == null) {
						metricBuilder = new HMetricBuilder(metric.getKey());
						metricBuilders.put(metric.getKey(), metricBuilder);
					}
					metricBuilder.withMetric(metric);
				}
			}
			metrics = new HashMap<HMetricKey, HMetric>();
			for (final Entry<HMetricKey, HMetricBuilder> entry : metricBuilders.entrySet()) {
				metrics.put(entry.getKey(), entry.getValue().build());
			}
		}
		return metrics;
	}

	/** {@inheritDoc} */
	public HMetric getMetric(final HMetricKey metricKey) {
		Assertion.checkNotNull(metricKey);
		//---------------------------------------------------------------------
		return getLazyMetrics().get(metricKey);
	}

	/** {@inheritDoc} */
	public Collection<HMetric> getMetrics() {
		return getLazyMetrics().values();
	}

	public List<HPoint> getPoints(final HMetricKey metricKey) {
		final List<HPoint> points = new ArrayList<>();
		for (final HCube cube : cubes) {
			points.add(new HPoint() {
				/** {@inheritDoc} */
				public HMetric getMetric() {
					return cube.getMetric(metricKey);
				}

				/** {@inheritDoc} */
				public Date getDate() {
					return cube.getKey().getTime().getValue();
				}
			});
		}
		return Collections.unmodifiableList(points);
	}
}