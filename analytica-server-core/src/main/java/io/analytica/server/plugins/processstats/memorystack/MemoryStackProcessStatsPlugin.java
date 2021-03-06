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
package io.analytica.server.plugins.processstats.memorystack;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.google.gson.Gson;

import io.analytica.api.AProcess;
import io.analytica.server.impl.ProcessStatsPlugin;
import io.vertigo.lang.Activeable;

/**
 * Stockage des process, et conservation statistique de l'arbre.
 *
 * Transformation d'un Process constitué de sous-process.
 * Chaque Process (et donc sous process) est transformé en Cube avec :
 * - une agregation des mesures de ce process
 * - une agregation des mesures des sous process
 *
 *
 * @author npiedeloup
 * @version $Id: StandardProcessEncoderPlugin.java,v 1.16 2012/10/16 17:27:12 pchretien Exp $
 */
public final class MemoryStackProcessStatsPlugin implements ProcessStatsPlugin, Activeable, LastProcessMXBean {
	private final LimitedDelayQueue processQueue = new LimitedDelayQueue(24 * 60); //24h

	@Override
	public void merge(final AProcess process) {
		processQueue.add(process);
	}

	/** {@inheritDoc} */
	@Override
	public void start() {
		try {
			// Get the Platform MBean Server
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			// Construct the ObjectName for the processStorePlugin MBean we will register
			final ObjectName mbeanName = new ObjectName("analytica:type=LastProcessMXBean");
			// Register the processStorePlugin MBean
			mbs.registerMBean(this, mbeanName);
			System.out.println(mbeanName.getCanonicalName() + " : " + mbs.isRegistered(mbeanName));
		} catch (final Throwable th) {
			throw new RuntimeException("Erreur de publication JMX du LastProcessMXBean", th);
		}
	}

	/** {@inheritDoc} */
	@Override
	public void stop() {
		try {
			// Get the Platform MBean Server
			final MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			// Construct the ObjectName for the processStorePlugin MBean we will register
			final ObjectName mbeanName = new ObjectName("analytica:type=LastProcessMXBean");
			// Unregister the processStorePlugin MBean
			mbs.unregisterMBean(mbeanName);
		} catch (final Throwable th) {
			throw new RuntimeException("Erreur de dépublication JMX du LastProcessMXBean", th);
		}
	}

	/** {@inheritDoc} */
	@Override
	public String getLastProcessesJson() {
		final List<AProcess> processes = new ArrayList<>(processQueue);
		return new Gson().toJson(processes);
	}

	/**
	 * Liste de process limité en durée, seul les plus récents sont gardés.
	 * @author npiedeloup
	 * @version $Id: $
	 */
	static class LimitedDelayQueue extends LinkedList<AProcess> {

		private static final long serialVersionUID = -6085444623815188157L;
		private final int delayMinute;

		public LimitedDelayQueue(final int delayMinute) {
			this.delayMinute = delayMinute;
		}

		public int getDelayMinute() {
			return delayMinute;
		}

		/** {@inheritDoc} */
		@Override
		public boolean add(final AProcess o) {
			return super.add(o);
			//			final Date limit = new Date(System.currentTimeMillis() - delayMinute * 60 * 1000);
			//			while (!isEmpty() && getFirst().getStartDate().before(limit)) {
			//				super.remove();
			//			}
			//			return true;
		}
	}
}
