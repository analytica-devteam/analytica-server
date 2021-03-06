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
package kasperimpl.spaces.spaces.kraft;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import kasper.kernel.Home;
import kasper.kernel.di.injector.Injector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.analytica.hcube.HCubeManager;
import io.analytica.hcube.query.HQuery;
import io.analytica.hcube.result.HResult;
import io.analytica.server.ServerManager;

@Path("/home")
public class HomeServices {
	private final String dTimeTo = "NOW+6h";
	private final String dTimeFrom = "NOW-8h";
	private final String dTimeDim = "Minute";
	private final String dDatas = "duration:mean";
	private final String dDatasMult = "duration:count;duration:mean";

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	@Inject
	private ServerManager serverManager;
	@Inject
	private HCubeManager cubeManager;

	public HomeServices() {
		final Injector injector = new Injector();
		injector.injectMembers(this, Home.getContainer().getRootContainer());
	}

	@GET
	@Produces(MediaType.TEXT_HTML)
	public String hello() {
		// @formatter:off
		return new StringBuilder()
			.append("<html><body>")
			.append("<a href=\"\\/datas\">datas</a>")
			.append("<a href=\"\\/categories\">categories</a>")
			.append("</body></html>")
			.toString();
		// @formatter:on
	}

	@GET
	@Path("/timeLine/{category}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getMonoSerieTimeLine(@QueryParam("timeFrom") @DefaultValue(dTimeFrom) final String timeFrom, @QueryParam("timeTo") @DefaultValue(dTimeTo) final String timeTo, @DefaultValue(dTimeDim) @QueryParam("timeDim") final String timeDim, @PathParam("category") final String category, @DefaultValue(dDatas) @QueryParam("datas") final String datas) {
		final HQuery query = Utils.createQuery(timeFrom, timeTo, timeDim, category, false);
		final HResult result = serverManager.execute(query);

		final List<DataPoint> points = Utils.loadDataPointsMonoSerie(result, datas);

		return gson.toJson(points);
	}

	@GET
	@Path("/multitimeLine/{category}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getMultiSerieTimeLine(@QueryParam("timeFrom") @DefaultValue(dTimeFrom) final String timeFrom, @QueryParam("timeTo") @DefaultValue(dTimeTo) final String timeTo, @DefaultValue(dTimeDim) @QueryParam("timeDim") final String timeDim, @PathParam("category") final String category, @DefaultValue(dDatasMult) @QueryParam("datas") final String datas) {
		final HQuery query = Utils.createQuery(timeFrom, timeTo, timeDim, category, false);
		final HResult result = serverManager.execute(query);

		final Map<String, List<DataPoint>> pointsMap = Utils.loadDataPointsMuliSerie(result, datas);

		return gson.toJson(pointsMap);
	}

	@GET
	@Path("/agregatedDatasByCategory/{category}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getAggregatedDataByCategory(@QueryParam("timeFrom") @DefaultValue("NOW-12h") final String timeFrom, @QueryParam("timeTo") @DefaultValue("NOW+2h") final String timeTo, @DefaultValue("Hour") @QueryParam("timeDim") final String timeDim, @PathParam("category") final String category, @DefaultValue("duration:count") @QueryParam("datas") final String datas) {
		final HQuery query = Utils.createQuery(timeFrom, timeTo, timeDim, category, true);
		final HResult result = serverManager.execute(query);

		return gson.toJson(Utils.getAggregatedValuesByCategory(result, datas));
	}

	@GET
	@Path("/categories")
	@Produces(MediaType.APPLICATION_JSON)
	public String getCategories() {
		return gson.toJson(cubeManager.getCategoryDictionary().getAllRootCategories());
	}

	//	@GET
	//	@Path("/bollinger/{category}")
	//	@Produces(MediaType.APPLICATION_JSON)
	//	public String getBollingerBands(@QueryParam("timeFrom") @DefaultValue(dTimeFrom) final String timeFrom, @QueryParam("timeTo") @DefaultValue(dTimeTo) final String timeTo, @DefaultValue(dTimeDim) @QueryParam("timeDim") final String timeDim, @PathParam("category") final String category, @DefaultValue(dDatasMult) @QueryParam("datas") final String datas) {
	//		final HResult result = Utils.resolveQuery(timeFrom, timeTo, timeDim, category, false);
	//		final Map<String, List<DataPoint>> pointsMap = Utils.loadBollingerBands(result, datas);
	//
	//		return gson.toJson(pointsMap);
	//	}

	@GET
	@Path("/stackedDatas/{category}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getAllCategoriesToStack(@QueryParam("timeFrom") @DefaultValue("NOW-12h") final String timeFrom, @QueryParam("timeTo") @DefaultValue("NOW+2h") final String timeTo, @DefaultValue("Hour") @QueryParam("timeDim") final String timeDim, @PathParam("category") final String category, @DefaultValue("duration:count") @QueryParam("datas") final String datas) {
		final HQuery query = Utils.createQuery(timeFrom, timeTo, timeDim, category, true);
		final HResult result = serverManager.execute(query);

		return gson.toJson(Utils.loadDataPointsStackedByCategory(result, datas));
	}

	@GET
	@Path("/tableSparkline/{category}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getTableSparklineDatas(@QueryParam("timeFrom") @DefaultValue("NOW-12h") final String timeFrom, @QueryParam("timeTo") @DefaultValue("NOW+2h") final String timeTo, @DefaultValue("Hour") @QueryParam("timeDim") final String timeDim, @PathParam("category") final String category, @DefaultValue("duration:count") @QueryParam("datas") final String datas) {
		final HQuery query = Utils.createQuery(timeFrom, timeTo, timeDim, category, true);
		final HResult result = serverManager.execute(query);

		return gson.toJson(Utils.getSparklinesTableDatas(result, datas));
	}

	@GET
	@Path("/tablePunchcard/{category}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getPunchCardDatas(@QueryParam("timeFrom") @DefaultValue("NOW-240h") final String timeFrom, @QueryParam("timeTo") @DefaultValue("NOW") final String timeTo, @DefaultValue("Hour") @QueryParam("timeDim") final String timeDim, @PathParam("category") final String category, @DefaultValue("duration:count") @QueryParam("datas") final String datas) {
		final HQuery query = Utils.createQuery(timeFrom, timeTo, timeDim, category, false);
		final HResult result = serverManager.execute(query);
		return gson.toJson(Utils.getPunchCardDatas(result, datas));
	}

	@GET
	@Path("/faketablePunchcard/{category}")
	@Produces(MediaType.APPLICATION_JSON)
	public String getPunchCardFakeDatas(@QueryParam("timeFrom") @DefaultValue("NOW-240h") final String timeFrom, @QueryParam("timeTo") @DefaultValue("NOW") final String timeTo, @DefaultValue("Hour") @QueryParam("timeDim") final String timeDim, @PathParam("category") final String category, @DefaultValue("duration:count") @QueryParam("datas") final String datas) {
		final HQuery query = Utils.createQuery(timeFrom, timeTo, timeDim, category, false);
		final HResult result = serverManager.execute(query);
		return gson.toJson(Utils.getPunchCardFakeDatas(result, datas));
	}

}
