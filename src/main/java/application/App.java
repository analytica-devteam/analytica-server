package application;

import java.util.Scanner;

import kasper.kernel.Home;

import com.kleegroup.analytica.core.KProcess;
import com.kleegroup.analytica.server.ServerManager;
import com.kleegroup.museum.Museum;
import com.kleegroup.museum.PageListener;

/**
 * @author statchum
 */
public final class App {

	public static void main(String[] args) {
		Home.start(AppConfig.createHomeConfig());

		loadDatas();

		try {
			System.out.println("Taper sur entr�e pour sortir");
			final Scanner sc = new Scanner(System.in);
			sc.nextLine();
			sc.close();
		} finally {
			Home.stop();
		}
	}

	private static void loadDatas() {
		final ServerManager serverManager = Home.getContainer().getManager(ServerManager.class);
		new Museum(new PageListener() {
			@Override
			public void onPage(KProcess process) {
				serverManager.push(process);

			}
		}).load();
	}
}
