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
package io.analytica.server.plugins.processstore.berkeley;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.je.DatabaseEntry;

import io.analytica.api.AProcess;

/**
 * Objet d'accés en écriture é la base Berkeley.
 *
 * @author pchretien
 * @version $Id: BerkeleyDatabaseWriter.java,v 1.3 2012/04/06 16:09:07 npiedeloup Exp $
 */
final class BerkeleyDatabaseWriter {
	private final EntryBinding uuidBinding = TupleBinding.getPrimitiveBinding(Long.class);
	private final TupleBinding processBinding = new AProcessBinding();

	AProcess readProcess(final DatabaseEntry process) {
		return (AProcess) processBinding.entryToObject(process);
	}

	public BerkeleyDatabaseWriter() {
		System.out.println("Constructeur BerkeleyDatabaseWriter");
	}

	DatabaseEntry writeProcess(final AProcess process) {
		final DatabaseEntry resultKey = new DatabaseEntry();
		if (process != null) {
			processBinding.objectToEntry(process, resultKey);
		}
		return resultKey;
	}

	long readKey(final DatabaseEntry key) {
		final long longKey = (Long) uuidBinding.entryToObject(key);
		return longKey;
	}

	DatabaseEntry writeKey(final long key) {
		final DatabaseEntry resultKey = new DatabaseEntry();
		uuidBinding.objectToEntry(key, resultKey);
		return resultKey;
	}
}
