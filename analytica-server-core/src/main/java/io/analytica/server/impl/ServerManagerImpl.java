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
package io.analytica.server.impl;

import io.analytica.api.KProcess;
import io.analytica.hcube.HCubeManager;
import io.analytica.hcube.query.HQuery;
import io.analytica.hcube.result.HResult;
import io.analytica.server.ServerManager;
import io.analytica.server.impl.ProcessEncoder.Dual;
import io.vertigo.lang.Activeable;
import io.vertigo.lang.Assertion;
import io.vertigo.lang.Option;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.inject.Inject;

/**
 * Manager Serveur d'Analytica.
 * @author npiedeloup
 * @version $Id: ServerManagerImpl.java,v 1.22 2013/01/14 16:35:19 npiedeloup Exp $
 */
public final class ServerManagerImpl implements ServerManager, Activeable {
	private final HCubeManager hcubeManager;
	private final ProcessStorePlugin processStorePlugin;
	private Timer asyncCubeStoreTimer = null;
	private final ProcessEncoder processEncoder;
	private final Option<ProcessStatsPlugin> processStatsPlugin;
	private final QueryNetApiPlugin queryNetApiPlugin;
	private final ProcessNetApiPlugin processNetApiPlugin;

	/**
	 * Constructeur.
	 * @param processStorePlugin Plugin de stockage des Process
	 * @param hcubeManager Manager de stockage des Cubes
	 */
	@Inject
	public ServerManagerImpl(final HCubeManager hcubeManager, final ProcessStorePlugin processStorePlugin, final Option<ProcessStatsPlugin> processStatsPlugin, final QueryNetApiPlugin queryNetApiPlugin, final ProcessNetApiPlugin processNetApiPlugin) {
		super();
		Assertion.checkNotNull(hcubeManager);
		Assertion.checkNotNull(processStorePlugin);
		Assertion.checkNotNull(processStatsPlugin);
		//-----------------------------------------------------------------
		this.hcubeManager = hcubeManager;
		this.processStorePlugin = processStorePlugin;
		processEncoder = new ProcessEncoder();
		this.processStatsPlugin = processStatsPlugin;
		this.queryNetApiPlugin = queryNetApiPlugin;
		this.processNetApiPlugin = processNetApiPlugin;
	}

	/** {@inheritDoc} */
	@Override
	public void push(final KProcess process) {
		processStorePlugin.add(process);
		//hcubeManager.push(process);
	}

	/** {@inheritDoc} */
	@Override
	public HResult execute(final String appName, final String type, final HQuery query) {
		return hcubeManager.getApp(appName).execute(type, query);
	}

	/** {@inheritDoc} */
	@Override
	public void start() {
		asyncCubeStoreTimer = new Timer("pushProcessToHCube", true);
		final TimerTask storeCubeTask = new StoreCubeTask();
		asyncCubeStoreTimer.schedule(storeCubeTask, 1000, 250); //X processes toutes les 250ms.. pour la vrai vie ok, pour les tests unitaires pas suffisant
	}

	/** {@inheritDoc} */
	@Override
	public void stop() {
		asyncCubeStoreTimer.cancel();
		asyncCubeStoreTimer = null;
	}

	private class StoreCubeTask extends TimerTask {
		/** {@inheritDoc} */
		@Override
		public void run() {
			storeNextProcessesAsCube();
		}
	}

	private String lastProcessIdStored = null;

	/** {@inheritDoc} */
	private int storeNextProcessesAsCube() {
		final List<Identified<KProcess>> nextProcesses = processStorePlugin.getProcess(lastProcessIdStored, 500);
		for (final Identified<KProcess> process : nextProcesses) {
			final List<Dual> duals = processEncoder.encode(process.getData());
			if (processStatsPlugin.isDefined()) {
				processStatsPlugin.get().merge(process.getData());
			}
			for (final Dual dual : duals) {
				hcubeManager.getApp(process.getData().getAppName()).push(process.getData().getType(), dual.getKey(), dual.getCubeBuilder().build());
			}
			lastProcessIdStored = process.getKey();
		}
		return nextProcesses.size();
	}
}

//
//	private void storeAsCube(final KProcess process) {
//		//Encode le process et ses sous process
//		final List<Cube> cubes = encoderPlugin.encode(process);
//		for (final Cube cube : cubes) {
//			cubeStorePlugin.merge(cube);
//		}
//		System.out.println(cubeStorePlugin);
//	}

//
//	/** {@inheritDoc} */
//	public List<Data> getData(final Query query) {
//		return null;

/*		final List<Cube> aggregatedCubes = cubeStorePlugin.load(query, true, true);
		if (aggregatedCubes.isEmpty()) {
			return Collections.emptyList(); //TODO npi que faire si pas de ligne, l'aggregation devrait retourner toujours une ligne, non ?
		}
		//---------------------------------------------------------------------
		Assertion.postcondition(aggregatedCubes.size() == 1, "La liste de cube doit �tre agr�g�e sur tout les axes, il doit dont y avoir un seul �l�ment dans la liste (size:{0})", aggregatedCubes.size());
		//---------------------------------------------------------------------
		final Cube aggregatedCube = aggregatedCubes.get(0);
		//On convertit le cube en liste de Data : 1 par metrics
		final List<Data> datas = new ArrayList<Data>(query.getKeys().size());
		for (final DataKey dataKey : query.getKeys()) {
			//		if (dataKey.getType() == DataType.metaData) {
			//				final List<String> metaDataValues = new ArrayList<String>();
			//				for (final MetaData metaData : aggregatedCube.getMetaData(dataKey.getName())) {
			//					metaDataValues.add(metaData.getValue());
			//				}
			//datas.add(new Data(dataKey, metaDataValues));
			//			} else {
			datas.add(new Data(dataKey, getCubeValue(aggregatedCube, dataKey)));
			//			}
		}
		return datas;*/
//	}
//
//	/** {@inheritDoc} */
//	public List<DataSet<Date, ?>> getDataTimeLine(final Query query) {
//		return null;
/*		final List<Cube> aggregatedCubes = cubeStorePlugin.load(query, false, true);
		//On convertit la liste de cube en liste de DataSet : 1 par metrics
		return convertToDataSet(aggregatedCubes, true, query.getKeys());*/
//}

////	/** {@inheritDoc} */
////	public List<DataSet<String, ?>> getDataWhatLine(final Query query) {
////		return null;
////		/*
//		final List<Cube> aggregatedCubes = cubeStorePlugin.load(query, true, false);
//		//On convertit la liste de cube en liste de DataSet : 1 par metrics
//		return convertToDataSet(aggregatedCubes, false, query.getKeys());
//		*/
//	}

//	private <X> List<DataSet<X, ?>> convertToDataSet(final List<Cube> aggregatedCubes, final boolean dateAsLabels, final List<DataKey> metrics) {
//		final List<X> labels = new ArrayList<X>(aggregatedCubes.size());
//		final Map<DataKey, List<Double>> valuesMap = new HashMap<DataKey, List<Double>>();
//		//		final Map<DataKey, List<Set<String>>> metaDatasMap = new HashMap<DataKey, List<Set<String>>>();
//		//1- on pr�pare les listes de valeurs et de metaData
//		for (final DataKey dataKey : metrics) {
//			//			if (dataKey.getType() == DataType.metaData) {
//			//				metaDatasMap.put(dataKey, new ArrayList<Set<String>>());
//			//			} else {
//			valuesMap.put(dataKey, new ArrayList<Double>());
//			//			}
//		}
//		//2- On parcour les cubes et on remplit les donn�es
//		for (final Cube cube : aggregatedCubes) {
//			//Si la liste des cubes est limit� on peut �viter ce if,
//			//mais il reste int�r�ssant de remplir les labels au m�me endroit que les valeurs
//			if (dateAsLabels) {
//				labels.add((X) cube.getPosition().getTime().getValue());
//			} else {
//				labels.add((X) cube.getPosition().getWhat().getValue());
//			}
//			for (final DataKey dataKey : metrics) {
//				//if (dataKey.getType() == DataType.metaData) {
//				//					final List<Set<String>> metadatas = metaDatasMap.get(dataKey);
//				//					final Set<String> values = new HashSet<String>();
//				//					for (final MetaData metaData : cube.getMetaData(dataKey.getName())) {
//				//						values.add(metaData.getValue());
//				//					}
//				//					metadatas.add(values);
//				//} else {
//				final List<Double> values = valuesMap.get(dataKey);
//				values.add(getCubeValue(cube, dataKey));
//				//}
//			}
//		}
//
//		//3- On cr�e les DataSet
//		final List<DataSet<X, ?>> datas = new ArrayList<DataSet<X, ?>>(metrics.size());
//		for (final DataKey dataKey : metrics) {
//			//			if (dataKey.getType() == DataType.metaData) {
//			//				datas.add(new DataSet<X, Set<String>>(dataKey, labels, metaDatasMap.get(dataKey)));
//			//			} else {
//			datas.add(new DataSet<X, Double>(dataKey, labels, valuesMap.get(dataKey)));
//			//			}
//		}
//		return datas;
//	}

//	private double getCubeValue(final Cube cube, final DataKey dataKey) {
//		final Metric metric = cube.getMetric(dataKey.getMetricKey());
//		//Assertion.checkNotNull(metric,"La metric {0} n''a pas �t� trouv�e dans le cube {1}", dataKey.getName(), cube.getKey());
//		//---------------------------------------------------------------------
//		if (metric == null) { //la metric peut-�tre null sur certain cube (exemple 'CACHE_HIT' n'est pr�sent que sur quelques cubes)
//			return Double.NaN;
//		}
//		return metric.get(dataKey.getType());
//	}
//

//	/** {@inheritDoc} */

