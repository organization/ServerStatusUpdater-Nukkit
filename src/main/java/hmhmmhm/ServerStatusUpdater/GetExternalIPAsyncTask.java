package hmhmmhm.ServerStatusUpdater;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import cn.nukkit.Server;
import cn.nukkit.scheduler.AsyncTask;

public class GetExternalIPAsyncTask extends AsyncTask {
	String ip;
	String pluginName;

	public GetExternalIPAsyncTask(String pluginName) {
		this.pluginName = pluginName;
	}

	@Override
	public void onRun() {
		try {
			this.ip = this.getIp();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onCompletion(Server server) {
		ServerStatusUpdater plugin = (ServerStatusUpdater) server.getPluginManager().getPlugin(this.pluginName);
		plugin.reportExternalIp(this.ip);
	}

	public String getIp() throws Exception {
		URL whatismyip = new URL("http://checkip.amazonaws.com");
		BufferedReader in = null;
		try {
			in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			String ip = in.readLine();
			return ip;
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}
