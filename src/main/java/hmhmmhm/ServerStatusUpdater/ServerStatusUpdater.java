package hmhmmhm.ServerStatusUpdater;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.scheduler.PluginTask;
import cn.nukkit.utils.Config;

public class ServerStatusUpdater extends PluginBase implements Listener {
	private Queue<String> chatlist = new LinkedList<String>();
	private String externalIp = "0.0.0.0";

	public void onEnable() {
		this.getDataFolder().mkdirs();

		LinkedHashMap<String, Object> defaultMap = new LinkedHashMap<String, Object>();
		defaultMap.put("statusLocate", this.getDataFolder().getAbsolutePath() + "/status.json");
		defaultMap.put("updateFrequency", 5);

		Map<String, Object> defaultConfig = (new Config(this.getDataFolder().getAbsolutePath() + "/locate.json",
				Config.JSON, defaultMap)).getAll();

		this.getServer().getScheduler().scheduleAsyncTask(new GetExternalIPAsyncTask("ServerStatusUpdater"));
		
		int updateFrequency = 5;
		if(defaultConfig.get("updateFrequency") instanceof Double){
			updateFrequency = ((Double) defaultConfig.get("updateFrequency")).intValue();
		}else{
			updateFrequency = (Integer) defaultConfig.get("updateFrequency");
		}
		
		this.getServer().getScheduler().scheduleRepeatingTask(new StatusUpdateTask(this),
				updateFrequency * 20);
		this.getServer().getPluginManager().registerEvents(this, this);

		this.getLogger().info("Activated");
	}

	@EventHandler
	public void onChat(PlayerChatEvent $event) {
		this.getServer().getScheduler().scheduleDelayedTask(new PlayerChatCollector(this, $event), 1);
	}

	public void addChat(String string) {
		this.chatlist.offer(string);
		if (this.chatlist.size() > 9)
			this.chatlist.poll();
	}

	public void reportExternalIp(String ip) {
		this.externalIp = ip;
	}

	public void update() {
		LinkedHashMap<String, Object> status = new LinkedHashMap<String, Object>();
		status.put("motd", this.getServer().getMotd());
		status.put("gamemode", Server.getGamemodeString(this.getServer().getGamemode()).split("%gameMode.")[1]);
		status.put("maxplayer", this.getServer().getMaxPlayers());
		status.put("nowplayer", this.getServer().getOnlinePlayers().size());
		status.put("version", this.getServer().getVersion());
		status.put("whitelist", this.getServer().getWhitelist().getAll().size() == 0 ? false : true);
		status.put("default-level-name", this.getServer().getDefaultLevel().getName());
		status.put("tps", this.getServer().getTicksPerSecond());
		status.put("ip", this.externalIp);
		status.put("port", this.getServer().getPort());
		status.put("in-game-time", this.getMinecraftTime(this.getServer().getDefaultLevel().getTime()));
		status.put("server-engine", this.getServer().getName() + " " + this.getServer().getNukkitVersion());

		ArrayList<String> players = new ArrayList<String>();
		for (Entry<String, Player> player : this.getServer().getOnlinePlayers().entrySet()) {
			if (player.getValue().isOnline())
				players.add(player.getValue().getName());
		}

		status.put("player-list", players.toArray());
		status.put("chat-list", this.chatlist.toArray());

		Map<String, Object> defaultConfig = (new Config(this.getDataFolder().getAbsolutePath() + "/locate.json",
				Config.JSON)).getAll();
		Config statusConfig = new Config((String) defaultConfig.get("statusLocate"), Config.JSON);

		statusConfig.setAll(status);
		statusConfig.save(true);
	}

	public String getMinecraftTime(int tick) {
		double totalhour = (tick / 1000.0) + 6.0;
		double totalday = Math.floor(totalhour) / 24.0;

		double nowhour = totalhour - totalday * 24.0;
		double nowmin = (nowhour - Math.floor(nowhour)) * 60.0;
		double nowsec = (nowmin - Math.floor(nowmin)) * 60.0;

		int hour = (int) nowhour;
		int min = (int) nowmin;
		int sec = (int) nowsec;

		String meridiem = "";
		if (hour <= 12) {
			meridiem = "AM";
		} else {
			hour -= 12;
			meridiem = "PM";
		}

		return meridiem + ":" + hour + ":" + min + ":" + sec;
	}
}

class PlayerChatCollector extends PluginTask<ServerStatusUpdater> {
	private PlayerChatEvent event;

	public PlayerChatCollector(ServerStatusUpdater owner, PlayerChatEvent event) {
		super(owner);
		this.event = event;
	}

	@Override
	public void onRun(int currentTick) {
		if (!this.event.isCancelled()) {
			String chat = Server.getInstance().getLanguage().translateString(this.event.getFormat(),
					new String[] { this.event.getPlayer().getDisplayName(), this.event.getMessage() });
			if (chat != null)
				this.getOwner().addChat(chat);
		}
	}
}

class StatusUpdateTask extends PluginTask<ServerStatusUpdater> {
	public StatusUpdateTask(ServerStatusUpdater owner) {
		super(owner);
	}

	public void onRun(int currentTick) {
		this.getOwner().update();
	}
}
