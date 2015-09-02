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
package io.analytica.server;

import io.analytica.hcube.HCubeManager;
import io.analytica.hcube.impl.HCubeManagerImpl;
import io.analytica.hcube.plugin.store.lucene.LuceneHCubeStorePlugin;
import io.analytica.restserver.RestServerManager;
import io.analytica.restserver.impl.RestServerManagerImpl;
import io.analytica.server.impl.ServerManagerImpl;
import io.analytica.server.plugins.processapi.rest.RestProcessNetApiPlugin;
import io.analytica.server.plugins.processstats.memorystack.MemoryStackProcessStatsPlugin;
import io.analytica.server.plugins.processstats.socketio.SocketIoProcessStatsPlugin;
import io.analytica.server.plugins.processstore.berkeley.BerkeleyProcessStorePlugin;
import io.analytica.server.plugins.queryapi.rest.RestQueryNetApiPlugin;
import io.vertigo.core.App;
import io.vertigo.core.config.AppConfig;
import io.vertigo.core.config.AppConfigBuilder;
import io.vertigo.core.config.ComponentConfigBuilder;
import io.vertigo.core.config.ModuleConfigBuilder;
import io.vertigo.lang.Assertion;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Charge et d�marre un environnement.
 * @author pchretien, npiedeloup
 */
public class Starter implements Runnable {
	private static final String PROCESS_STORE_PATH = "processStorePath";
	private static final String CUBE_STORE_PATH = "cubeStorePath";
	private static final String SOCKET_IO_URL = "socketIoUrl";
	private static final String STACK_PROCESS_STATS = "stackProcessStats";
	private static final String REST_API_PATH = "restApiPath";
	private static final String REST_API_PORT = "restApiPort";
	private static final String TYPE_API_NONE = "NONE";
	private static final String TYPE_API_REST = "REST";
	private static final String PROCESS_API = "processApi";
	private static final String QUERY_API = "queryApi";

	private final Class<?> relativeRootClass;
	private final String propertiesFileName;
	private boolean started;
	private App app;

	/**
	 * @param propertiesFileName Fichier de propri�t�s
	 * @param relativeRootClass Racine du chemin relatif, le cas ech�ant
	 */
	public Starter(final String propertiesFileName, final Class<?> relativeRootClass) {
		Assertion.checkNotNull(propertiesFileName);
		Assertion.checkNotNull(relativeRootClass);
		//---------------------------------------------------------------------
		this.propertiesFileName = propertiesFileName;
		this.relativeRootClass = relativeRootClass;
	}

	/**
	 * Lance l'environnement et attend ind�finiment.
	 * @param args "Usage: java kasper.kernel.Starter managers.xml <conf.properties>"
	 */
	public static void main(final String[] args) {
		final String usageMsg = "Usage: java io.analytica.server.Starter <conf.properties>";
		Assertion.checkArgument(args.length == 1, usageMsg + " ( conf attendue : " + args.length + ")");
		Assertion.checkArgument(args[0].endsWith(".properties"), usageMsg + " ( .properties attendu : " + args[0] + ")");
		//---------------------------------------------------------------------
		final String propertiesFileName = args[0];
		final Starter starter = new Starter(propertiesFileName, Starter.class);
		starter.run();
	}

	/** {@inheritDoc} */
	@Override
	public final void run() {
		try {
			start();

			final Object lock = new Object();
			synchronized (lock) {
				lock.wait(0); //on attend le temps demand� et 0 => illimit�
			}
		} catch (final InterruptedException e) {
			//rien arret normal
		} catch (final Exception e) {
			e.printStackTrace();
		} finally {
			stop();
		}
	}

	/**
	 * D�marre l'application.
	 * @throws IOException Erreur de cr�ation du server Web
	 * @throws NumberFormatException Erreur de format du port
	 */
	public final void start() throws NumberFormatException, IOException {
		// Cr�ation de l'�tat de l'application
		// Initialisation de l'�tat de l'application
		final Properties properties = new Properties();
		appendFileProperties(properties, propertiesFileName, relativeRootClass);
		final AppConfig appConfig = createComponentSpaceConfig(properties);
		app = new App(appConfig);
		started = true;
	}

	/**
	 * @param properties Propri�t�s de l'environnement.
	 * @return ComponentSpaceConfig configuration de l'environnement
	 */
	protected final AppConfig createComponentSpaceConfig(final Properties properties) {
		final AppConfigBuilder componentSpaceConfigBuilder = new AppConfigBuilder()
				.beginBoot().silently().endBoot();
		appendModuleAnalytica(properties, componentSpaceConfigBuilder);
		appendOtherModules(properties, componentSpaceConfigBuilder);
		return componentSpaceConfigBuilder.build();
	}

	/**
	 * Ajoute d'autre modules � la configuration de l'environnement.
	 * @param properties  Propri�t�s de l'environnement.
	 * @param componentSpaceConfigBuilder Builder de la configuration de l'environnement
	 */
	protected void appendOtherModules(final Properties properties, final AppConfigBuilder appConfigBuilder) {
		//Possibilit� d'ajouter d'autres modules � la conf.
	}

	private final void appendModuleAnalytica(final Properties properties, final AppConfigBuilder appConfigBuilder) {
		final ModuleConfigBuilder moduleConfigBuilder = appConfigBuilder.beginModule("analytica");
		if (properties.containsKey(REST_API_PORT) || properties.containsKey(REST_API_PATH)) {
			moduleConfigBuilder.beginComponent(RestServerManager.class, RestServerManagerImpl.class) //
					.addParam("apiPath", properties.getProperty(REST_API_PATH, "/rest/"))
					.addParam("httpPort", properties.getProperty(REST_API_PORT, "8080"))
					.endComponent();
		}
		final ComponentConfigBuilder serverConfigBuilder = moduleConfigBuilder.beginComponent(ServerManager.class, ServerManagerImpl.class);
		if (properties.containsKey(PROCESS_STORE_PATH)) {
			moduleConfigBuilder.beginPlugin(BerkeleyProcessStorePlugin.class)
					.addParam("dbPath", properties.getProperty(PROCESS_STORE_PATH))
					.endPlugin();
		}

		if (TYPE_API_REST.equals(properties.getProperty(PROCESS_API, TYPE_API_NONE))) {
			moduleConfigBuilder.addPlugin(RestProcessNetApiPlugin.class);
		}
		if (TYPE_API_REST.equals(properties.getProperty(QUERY_API, TYPE_API_NONE))) {
			moduleConfigBuilder.addPlugin(RestQueryNetApiPlugin.class);
		}

		moduleConfigBuilder.addComponent(HCubeManager.class, HCubeManagerImpl.class)
				.beginPlugin(LuceneHCubeStorePlugin.class)
				.addParam("path", properties.getProperty(CUBE_STORE_PATH))
				.endPlugin();
		if (properties.containsKey(SOCKET_IO_URL)) {
			moduleConfigBuilder.beginPlugin(SocketIoProcessStatsPlugin.class)
					.addParam("socketIoUrl", properties.getProperty(SOCKET_IO_URL))
					.endPlugin();
		}
		if (Boolean.parseBoolean(properties.getProperty(STACK_PROCESS_STATS, "false"))) {
			moduleConfigBuilder.addPlugin(MemoryStackProcessStatsPlugin.class);
		}
		moduleConfigBuilder.endModule();
	}

	/**
	 * Stop l'application.
	 */
	public final void stop() {
		if (started) {
			app.close();
			started = false;
		}
	}

	/**
	 * Charge le fichier properties.
	 * Par defaut vide, mais il peut-�tre surcharg�.
	 * @param relativeRootClass Racine du chemin relatif, le cas ech�ant
	 */
	private static final void appendFileProperties(final Properties properties, final String propertiesFileName, final Class<?> relativeRootClass) {
		//---------------------------------------------------------------------
		final String fileName = translateFileName(propertiesFileName, relativeRootClass);
		try {
			final InputStream in = createURL(fileName, relativeRootClass).openStream();
			try {
				properties.load(in);
			} finally {
				in.close();
			}
		} catch (final IOException e) {
			throw new IllegalArgumentException("Impossible de charger le fichier de configuration des tests : " + fileName, e);
		}
	}

	/**
	 * Transforme le chemin vers un fichier local au test en une URL absolue.
	 * @param fileName Path du fichier : soit en absolu (commence par /), soit en relatif � la racine
	 * @param relativeRootClass Racine du chemin relatif, le cas ech�ant
	 * @return URL du fichier
	 */
	private static final URL createURL(final String fileName, final Class<?> relativeRootClass) {
		Assertion.checkArgNotEmpty(fileName);
		//---------------------------------------------------------------------
		final String absoluteFileName = translateFileName(fileName, relativeRootClass);
		try {
			return new URL(absoluteFileName);
		} catch (final MalformedURLException e) {
			//Si fileName non trouv�, on recherche dans le classPath
			final URL url = relativeRootClass.getResource(absoluteFileName);
			Assertion.checkNotNull(url, "Impossible de r�cup�rer le fichier [" + absoluteFileName + "]");
			return url;
		}
	}

	private static final String translateFileName(final String fileName, final Class<?> relativeRootClass) {
		Assertion.checkArgNotEmpty(fileName);
		//---------------------------------------------------------------------
		System.out.println(relativeRootClass.getResource(relativeRootClass.getSimpleName() + ".class").getPath().toString());
		if (fileName.startsWith(".")) {
			//soit en relatif
			return "/" + getRelativePath(relativeRootClass) + "/" + fileName.replace("./", "");
		}

		//soit en absolu
		if (fileName.startsWith("/")) {
			return fileName;
		}
		return "/" + fileName;
	}

	private static final String getRelativePath(final Class<?> relativeRootClass) {

		return relativeRootClass.getPackage().getName().replace('.', '/');
	}
}
