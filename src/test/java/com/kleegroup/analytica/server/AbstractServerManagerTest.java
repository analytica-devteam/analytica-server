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
package com.kleegroup.analytica.server;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import kasper.AbstractTestCaseJU4;
import kasper.kernel.util.Assertion;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.kleegroup.analytica.core.KProcess;
import com.kleegroup.analytica.core.KProcessBuilder;
import com.kleegroup.analytica.hcube.cube.DataKey;
import com.kleegroup.analytica.hcube.cube.DataType;
import com.kleegroup.analytica.hcube.cube.MetricKey;
import com.kleegroup.analytica.hcube.dimension.TimeDimension;
import com.kleegroup.analytica.hcube.dimension.WhatDimension;
import com.kleegroup.analytica.hcube.query.Query;
import com.kleegroup.analytica.hcube.query.QueryBuilder;
import com.kleegroup.analytica.server.data.Data;
import com.kleegroup.analytica.server.data.DataSet;

/**
 * Cas de Test JUNIT de l'API Analytics.
 * 
 * @author pchretien, npiedeloup
 * @version $Id: AbstractAnalyticaTest.java,v 1.11 2013/01/14 16:35:20 npiedeloup Exp $
 */
public abstract class AbstractServerManagerTest extends AbstractTestCaseJU4 {

	//	private static final String TEST_META_DATA_2 = "TEST_META_DATA_2";
	//	private static final String TEST_META_DATA_1 = "TEST_META_DATA_1";
	private static final MetricKey TEST_METRIC_MEAN_VALUE = new MetricKey("TEST_MEAN_VALUE");
	private static final MetricKey TEST_VALUE_PCT = new MetricKey("TEST_VALUE_PCT");
	private static final MetricKey MONTANT = new MetricKey("MONTANT");
	private static final MetricKey POIDS = new MetricKey("POIDS");

	/** Base de donn�es g�rant les articles envoy�s dans une commande. */
	private static final String PROCESS1_TYPE = "ARTICLE";
	private static final String PROCESS2_TYPE = "COMMANDE";

	/** Logger. */
	private final Logger log = Logger.getLogger(getClass());

	@Inject
	private ServerManager serverManager;

	//-------------------------------------------------------------------------

	/**
	 * Test simple avec deux compteurs. 
	 * Test sur l'envoi de 1000 articles d'un poids de 25 kg. 
	 * Chaque article coute 10�.
	 */
	@Test
	public void test1000Articles() {
		final KProcess process = createNArticles(1000);
		serverManager.push(process);
		printDatas(MONTANT);
	}

	/**
	 * M�me test apr�s d�sactivation.
	 */
	@Test
	public void testOff() {
		final KProcess process = createNArticles(50);
		serverManager.push(process);
		printDatas(MONTANT);
	}

	/**
	 * Test de r�cursivit�. 
	 * Test sur l'envoi de 500 commandes contenant chacune 500 articles d'un poids de 25 kg. 
	 * Chaque article coute 10�. 
	 * Les frais d'envoi sont de 5�.
	 */
	@Test
	public void test500Commandes() {
		final long start = System.currentTimeMillis();
		final KProcess process = createNCommande(5, 15);
		serverManager.push(process);
		log.trace("elapsed = " + (System.currentTimeMillis() - start));
		printDatas(MONTANT);
	}

	/**
	 * Test de parall�lisme. 
	 * Test sur l'envoi de 500 commandes contenant chacune 1000 articles d'un poids de 25 kg.
	 * L'envoi est simuler avec 20 clients (thread).
	 * Chaque article coute 10�. 
	 * Les frais d'envoi sont de 5�.
	 * @throws InterruptedException Interruption
	 */
	@Test
	public void testMultiThread() throws InterruptedException {
		final long start = System.currentTimeMillis();
		final ExecutorService workersPool = Executors.newFixedThreadPool(20);

		for (int i = 0; i < 50; i++) {
			workersPool.execute(new CommandeTask(String.valueOf(i), 5));
		}
		workersPool.shutdown();
		workersPool.awaitTermination(2, TimeUnit.MINUTES); //On laisse 2 minute pour vider la pile   
		Assertion.invariant(workersPool.isTerminated(), "Les threads ne sont pas tous stopp�s");

		log.trace("elapsed = " + (System.currentTimeMillis() - start));
		printDatas(MONTANT);
		//System.out.println(analyticaUIManager.toString(serverManager.getProcesses()));
	}

	private static KProcessBuilder createProcess(final String module, final String fullName) {
		return new KProcessBuilder(module, fullName, new Date(), 10);
	}

	private static KProcessBuilder createProcess(final String module, final String fullName, final long time) {
		return new KProcessBuilder(module, fullName, new Date(), time);
	}

	//	@Test
	//	public void testMetaData() {
	//		final KProcess kProcess1 = createProcess("TEST_META_DATA", "Process1")//
	//				.setMetaData(TEST_META_DATA_1, "MD1")//
	//				.build();
	//		serverManager.push(kProcess1);
	//
	//		final KProcess kProcess2 = createProcess("TEST_META_DATA", "Process2")//
	//				.setMetaData(TEST_META_DATA_1, "MD2")//
	//				.setMetaData(TEST_META_DATA_2, "MD3")//
	//				.build();
	//		serverManager.push(kProcess2);
	//
	//		//---------------------------------------------------------------------
	//		final DataKey[] metrics = new DataKey[] { new DataKey(TEST_META_DATA_1, DataType.metaData), new DataKey(TEST_META_DATA_2, DataType.metaData) };
	//
	//		final List<Data> datas = getCubeToday("TEST_META_DATA", metrics);
	//		Set<String> value = getMetaData(datas, TEST_META_DATA_1);
	//		Assert.assertTrue("Le cube ne contient pas la metaData attendue : MD1\n" + datas, value.contains("MD1"));
	//		Assert.assertTrue("Le cube ne contient pas la metaData attendue : MD2\n" + datas, value.contains("MD2"));
	//		//---------------------------------------------------------------------
	//		value = getMetaData(datas, TEST_META_DATA_2);
	//		Assert.assertTrue("Le cube ne contient pas la metaData attendue\n" + datas, value.contains("MD3"));
	//	}

	@Test
	public void testMean() {
		final KProcess kProcess1 = createProcess("TEST_MEAN1", "Process1")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 100)//
				.build();
		serverManager.push(kProcess1);

		final KProcess kProcess2 = createProcess("TEST_MEAN1", "Process2")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 50)//
				.build();
		serverManager.push(kProcess2);
		//---------------------------------------------------------------------
		final DataKey[] metrics = new DataKey[] { new DataKey(TEST_METRIC_MEAN_VALUE, DataType.mean) };
		final List<Data> datas = getCubeToday("TEST_MEAN1", metrics);
		final double valueMean = getMean(datas, TEST_METRIC_MEAN_VALUE);
		Assert.assertEquals("Le cube ne contient pas la moyenne attendue\n" + datas, 75.0, valueMean, 0);
	}

	@Test
	public void testMeanZero() {
		final KProcess kProcess1 = createProcess("TEST_MEAN2", "Process1")//
				.incMeasure(TEST_VALUE_PCT.id(), 90)//
				.build();
		serverManager.push(kProcess1);

		final KProcess kProcess2 = createProcess("TEST_MEAN2", "Process2")//
				.build();
		//TEST_VALUE_PCT = 0 implicite
		serverManager.push(kProcess2);

		//---------------------------------------------------------------------
		final DataKey[] metrics = new DataKey[] { new DataKey(TEST_VALUE_PCT, DataType.mean) };
		final List<Data> datas = getCubeToday("TEST_MEAN2", metrics);
		final double valueMean = getMean(datas, TEST_VALUE_PCT);
		Assert.assertEquals("Le cube ne contient pas la moyenne attendue\n" + datas, 45.0, valueMean, 0);
	}

	@Test
	public void testData() {
		final KProcess kProcess1 = createProcess("TEST_MEAN3", "Process1")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 100)//
				.build();
		serverManager.push(kProcess1);

		final KProcess kProcess2 = createProcess("TEST_MEAN3", "Process2")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 50)//
				.build();
		serverManager.push(kProcess2);

		//---------------------------------------------------------------------
		final Query query = new QueryBuilder(asList(new DataKey(TEST_METRIC_MEAN_VALUE, DataType.mean)))//
				.on(WhatDimension.Type)//
				.with(WhatDimension.SEPARATOR + "TEST_MEAN3")//
				.on(TimeDimension.Minute)//
				.from(new Date(System.currentTimeMillis() - 1 * 1000))//
				.to(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))//
				.build();
		serverManager.store50NextProcessesAsCube();
		final List<Data> datas = serverManager.getData(query);
		final double valueMean = datas.get(0).getValue();
		Assert.assertEquals("Les datas ne contiennent pas la moyenne attendue\n" + datas, 75.0, valueMean, 0);
	}

	private static List<DataKey> asList(final DataKey... dataKey) {
		return Arrays.asList(dataKey);
	}

	@Test
	public void testDataWhatSelection() {
		final KProcess kProcess1 = createProcess("TEST_WHAT_SELECTION1", "Process1")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 100)//
				.build();
		serverManager.push(kProcess1);

		final KProcess kProcess2 = createProcess("TEST_WHAT_SELECTION1", "Process1")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 50)//
				.build();
		serverManager.push(kProcess2);

		final KProcess kProcess3 = createProcess("TEST_WHAT_SELECTION1", "Process2")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 80)//
				.build();
		serverManager.push(kProcess3);

		final KProcess kProcess4 = createProcess("TEST_WHAT_SELECTION1", "Process2")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 50)//
				.build();
		serverManager.push(kProcess4);

		final KProcess kProcess5 = createProcess("TEST_WHAT_SELECTION1", "Process3")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 10)//
				.build();
		serverManager.push(kProcess5);

		//---------------------------------------------------------------------
		serverManager.store50NextProcessesAsCube();
		final Query query = new QueryBuilder(asList(new DataKey(TEST_METRIC_MEAN_VALUE, DataType.mean)))//
				.on(WhatDimension.SimpleName)//
				.with("/TEST_WHAT_SELECTION1/Process1", "/TEST_WHAT_SELECTION1/Process2")//
				.on(TimeDimension.Minute)//
				.from(new Date())//
				.to(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))//
				.build();
		final List<Data> datas = serverManager.getData(query);
		final double valueMean = datas.get(0).getValue();
		Assert.assertEquals("Les datas ne contiennent pas la moyenne attendue\n" + datas, 70.0, valueMean, 0);
	}

	//
	//	@Test
	//	public void testDataMetaData() {
	//		final KProcess kProcess1 = createProcess("TEST_META_DATA3", "Process1")//
	//				.incMeasure("TEST_VALUE", 100)//
	//				.setMetaData(TEST_META_DATA_1, "MD1")//
	//				.build();
	//		serverManager.push(kProcess1);
	//
	//		final KProcess kProcess2 = createProcess("TEST_META_DATA3", "Process2")//
	//				.incMeasure("TEST_VALUE", 50)//
	//				.setMetaData(TEST_META_DATA_1, "MD2")//
	//				.setMetaData(TEST_META_DATA_2, "MD3")//
	//				.build();
	//		serverManager.push(kProcess2);
	//
	//		//---------------------------------------------------------------------
	//		serverManager.store50NextProcessesAsCube();
	//		final Query query = new QueryBuilder(asList(new DataKey(TEST_META_DATA_1, DataType.metaData), new DataKey(TEST_META_DATA_2, DataType.metaData)))//
	//				.on(WhatDimension.Type)//
	//				.with(WhatDimension.SEPARATOR + "TEST_META_DATA3")//
	//				.on(TimeDimension.Minute)//
	//				.from(new Date(System.currentTimeMillis() - 1 * 1000))//
	//				.to(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))//
	//				.build();
	//
	//		final List<Data> datas = serverManager.getData(query);
	//		final List<String> valueMetaData1 = datas.get(0).getStringValues();
	//		Assert.assertTrue("Le cube ne contient pas la metaData attendue : MD1\n" + datas, valueMetaData1.contains("MD1"));
	//		Assert.assertTrue("Le cube ne contient pas la metaData attendue : MD2\n" + datas, valueMetaData1.contains("MD2"));
	//		//---------------------------------------------------------------------
	//		final List<String> valueMetaData2 = datas.get(1).getStringValues();
	//		Assert.assertTrue("Le cube ne contient pas la metaData attendue\n" + datas, valueMetaData2.contains("MD3"));
	//
	//	}

	@Test
	public void testDataMultiMetric() {
		final KProcess kProcess1 = createProcess("TEST_MEAN4", "Process1")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 100)//
				.build();
		serverManager.push(kProcess1);

		final KProcess kProcess2 = createProcess("TEST_MEAN4", "Process2")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 50)//
				.build();
		serverManager.push(kProcess2);

		final KProcess kProcess3 = createProcess("TEST_MEAN4", "Process1")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 60)//
				.build();
		serverManager.push(kProcess3);

		final KProcessBuilder kProcessBuilder4 = createProcess("TEST_MEAN4", "Process2");
		kProcessBuilder4.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 90);
		serverManager.push(kProcessBuilder4.build());

		//---------------------------------------------------------------------
		final List<DataKey> metrics = asList(new DataKey(TEST_METRIC_MEAN_VALUE, DataType.mean), new DataKey(TEST_METRIC_MEAN_VALUE, DataType.max), new DataKey(TEST_METRIC_MEAN_VALUE, DataType.min), new DataKey(TEST_METRIC_MEAN_VALUE, DataType.count), new DataKey(TEST_METRIC_MEAN_VALUE, DataType.stdDev));
		serverManager.store50NextProcessesAsCube();
		final Query query = new QueryBuilder(metrics)//
				.on(WhatDimension.Type)//
				.with(WhatDimension.SEPARATOR + "TEST_MEAN4")//
				.on(TimeDimension.Minute)//
				.from(new Date(System.currentTimeMillis() - 1 * 1000))//
				.to(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))//
				.build();
		final List<Data> datas = serverManager.getData(query);
		final double valueMean = datas.get(0).getValue();
		final double valueMax = datas.get(1).getValue();
		final double valueMin = datas.get(2).getValue();
		final double valueCount = datas.get(3).getValue();
		final double valueStdDev = datas.get(4).getValue();
		Assert.assertEquals("Les datas ne contiennent pas la moyenne attendue\n" + datas, 75.0, valueMean, 0);
		Assert.assertEquals("Les datas ne contiennent pas le max attendu\n" + datas, 100.0, valueMax, 0);
		Assert.assertEquals("Les datas ne contiennent pas le min attendu\n" + datas, 50.0, valueMin, 0);
		Assert.assertEquals("Les datas ne contiennent pas le count attendu\n" + datas, 4.0, valueCount, 0);
		//L'ecart type attendu est arrondit a 2 chiffres : le r�el est 23.80476143
		Assert.assertEquals("Les datas ne contiennent pas l'�cart type attendu\n" + datas, 23.80, valueStdDev, 0);
	}

	@Test
	public void testDataSetWhatLine() {
		final KProcess kProcess1 = createProcess("TEST_MEAN5", "Process1")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 100)//
				.build();
		serverManager.push(kProcess1);

		final KProcess kProcess2 = createProcess("TEST_MEAN5", "Process2")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 50)//
				.build();
		serverManager.push(kProcess2);

		final KProcess kProcess3 = createProcess("TEST_MEAN5", "Process2")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 60)//
				.build();
		serverManager.push(kProcess3);

		final KProcess kProcess4 = createProcess("TEST_MEAN5", "Process3")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 90)//
				.build();
		serverManager.push(kProcess4);

		//---------------------------------------------------------------------
		final List<DataKey> metrics = asList(new DataKey(TEST_METRIC_MEAN_VALUE, DataType.mean), new DataKey(TEST_METRIC_MEAN_VALUE, DataType.count));
		serverManager.store50NextProcessesAsCube();
		final Query query = new QueryBuilder(metrics)//
				.on(WhatDimension.FullName)//
				.with(WhatDimension.SEPARATOR + "TEST_MEAN5")//
				.on(TimeDimension.Minute)//
				.from(new Date())//
				.to(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))//
				.build();

		final List<DataSet<String, ?>> datas = serverManager.getDataWhatLine(query);
		final DataSet<String, ?> dataSetMean = datas.get(0);
		Assert.assertEquals("Les datas ne contiennent pas le label attendu\n" + datas, "/TEST_MEAN5/Process1", dataSetMean.getLabels().get(0));
		Assert.assertEquals("Les datas ne contiennent pas le label attendu\n" + datas, "/TEST_MEAN5/Process2", dataSetMean.getLabels().get(1));
		Assert.assertEquals("Les datas ne contiennent pas le label attendu\n" + datas, "/TEST_MEAN5/Process3", dataSetMean.getLabels().get(2));
		Assert.assertEquals("Les datas ne contiennent pas la moyenne attendue\n" + datas, 100d, dataSetMean.getValues().get(0));
		Assert.assertEquals("Les datas ne contiennent pas la moyenne attendue\n" + datas, 55d, dataSetMean.getValues().get(1));
		Assert.assertEquals("Les datas ne contiennent pas la moyenne attendue\n" + datas, 90d, dataSetMean.getValues().get(2));

		final DataSet<String, ?> dataSetCount = datas.get(1);
		Assert.assertEquals("Les datas ne contiennent pas le label attendu\n" + datas, "/TEST_MEAN5/Process1", dataSetCount.getLabels().get(0));
		Assert.assertEquals("Les datas ne contiennent pas le label attendu\n" + datas, "/TEST_MEAN5/Process2", dataSetCount.getLabels().get(1));
		Assert.assertEquals("Les datas ne contiennent pas le label attendu\n" + datas, "/TEST_MEAN5/Process3", dataSetCount.getLabels().get(2));
		Assert.assertEquals("Les datas ne contiennent pas le count attendu\n" + datas, 1d, dataSetCount.getValues().get(0));
		Assert.assertEquals("Les datas ne contiennent pas le count attendu\n" + datas, 2d, dataSetCount.getValues().get(1));
		Assert.assertEquals("Les datas ne contiennent pas le count attendu\n" + datas, 1d, dataSetCount.getValues().get(2));
	}

	@Test
	public void testDataSetTimeLine() {
		final KProcess kProcess1 = createProcess("TEST_MEAN6", "Process1")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 100)//
				.build();
		serverManager.push(kProcess1);

		final KProcess kProcess2 = createProcess("TEST_MEAN6", "Process2")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 50)//
				.build();
		serverManager.push(kProcess2);

		final KProcess kProcess3 = createProcess("TEST_MEAN6", "Process2")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 60)//
				.build();
		serverManager.push(kProcess3);

		final KProcess kProcess4 = createProcess("TEST_MEAN6", "Process3")//
				.incMeasure(TEST_METRIC_MEAN_VALUE.id(), 90)//
				.build();
		serverManager.push(kProcess4);

		//---------------------------------------------------------------------
		final List<DataKey> metrics = asList(new DataKey(TEST_METRIC_MEAN_VALUE, DataType.mean), new DataKey(TEST_METRIC_MEAN_VALUE, DataType.count));
		serverManager.store50NextProcessesAsCube();

		final Query query = new QueryBuilder(metrics)//
				.on(WhatDimension.FullName)//
				.with(WhatDimension.SEPARATOR + "TEST_MEAN6")//
				.on(TimeDimension.Minute)//
				.from(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))//
				.to(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))//
				.build();

		final List<DataSet<Date, ?>> datas = serverManager.getDataTimeLine(query);
		System.out.println(datas);
		//		final DataSet<Date, ?> dataSetMean = datas.get(0);
		//		final DataSet<Date, ?> dataSetCount = datas.get(1);
	}

	private List<Data> getCubeToday(final String module, final DataKey... metrics) {
		final Query query = new QueryBuilder(asList(metrics))//
				.on(WhatDimension.Type)//
				.with(WhatDimension.SEPARATOR + module)//
				.on(TimeDimension.Minute)//
				.from(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000))//
				.to(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))//
				.build();

		serverManager.store50NextProcessesAsCube();

		return serverManager.getData(query);
	}

	private static double getMean(final List<Data> datas, final MetricKey metricKey) {
		for (final Data data : datas) {
			if (data.getKey().getType() == DataType.mean && metricKey.equals(data.getKey().getMetricKey())) {
				return data.getValue();
			}
		}
		throw new IllegalArgumentException("La mesure " + metricKey + " n'est pas trouv�e dans le module \n" + datas);
	}

	//	private static Set<String> getMetaData(final List<Data> datas, final String metadataName) {
	//		final Set<String> metaDatas = new HashSet<String>();
	//		for (final Data data : datas) {
	//			if (metadataName.equals(data.getKey().getMetricName())) {
	//				metaDatas.addAll(data.getStringValues());
	//			}
	//		}
	//		Assert.assertTrue("La metaData " + metadataName + " n'est pas trouv�e dans le module\n" + datas, metaDatas.size() >= 1);
	//		return metaDatas;
	//	}

	private void printDatas(final MetricKey... metricKeys) {
		final List<DataKey> dataKeys = new ArrayList<DataKey>(metricKeys.length);
		for (final MetricKey metricKey : metricKeys) {
			dataKeys.add(new DataKey(metricKey, DataType.count));
			dataKeys.add(new DataKey(metricKey, DataType.mean));
		}
		serverManager.store50NextProcessesAsCube();
		final Query query = new QueryBuilder(dataKeys)//
				.on(WhatDimension.SimpleName)//
				.with("/")//
				.on(TimeDimension.Day)//
				.from(new Date())//
				.to(new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000))//
				.build();

		final List<Data> datas = serverManager.getData(query);
		for (final Data data : datas) {
			System.out.println(data);
		}
	}

	/**
	 * Passe N commandes.
	 * @param nbCommandes Numero de la commande
	 * @param nbArticles Nombre d'article
	 */
	private static KProcess createNCommande(final int nbCommandes, final int nbArticles) {
		final KProcessBuilder kProcessBuilder1 = createProcess(PROCESS2_TYPE, nbCommandes + " Commandes");
		for (int i = 0; i < nbCommandes; i++) {
			kProcessBuilder1.addSubProcess(createOneCommande(String.valueOf(i), nbArticles));
		}
		return kProcessBuilder1.build();
	}

	/**
	 * Passe une commande.
	 * @param numCommande Numero de la commande
	 * @param nbArticles Nombre d'article
	 * @return Kprocess resultat
	 */
	private static KProcess createOneCommande(final String numCommande, final int nbArticles) {
		return createProcess(PROCESS2_TYPE, "1 Commande")//
				.setMetaData(numCommande, "NUMERO")//
				.incMeasure(MONTANT.id(), 5)//
				.addSubProcess(createNArticles(nbArticles))//
				.build();
	}

	/**
	 * Ajoute N articles.
	 * @param nbArticles Nombre d'article
	 * @return Kprocess resultat
	 */
	private static KProcess createNArticles(final int nbArticles) {
		final KProcessBuilder kProcessBuilder2 = createProcess(PROCESS1_TYPE, nbArticles + " Articles 25 Kg", nbArticles * 10);
		for (int i = 0; i < nbArticles; i++) {
			final KProcess kProcess1 = createProcess(PROCESS1_TYPE, "1 Article 25 Kg")//
					.setMeasure(POIDS.id(), 25)//
					.incMeasure(MONTANT.id(), 10)//
					.build();
			kProcessBuilder2.addSubProcess(kProcess1);
		}
		return kProcessBuilder2.build();
	}

	private final class CommandeTask implements Runnable {
		private final String numCommande;
		private final int nbArticles;

		public CommandeTask(final String numCommande, final int nbArticles) {
			this.numCommande = numCommande;
			this.nbArticles = nbArticles;
		}

		public void run() {
			final KProcess process = createOneCommande(numCommande, nbArticles);
			serverManager.push(process);
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				//rien
			}
			System.out.println("Finish commande n�" + numCommande);
		}
	}
}