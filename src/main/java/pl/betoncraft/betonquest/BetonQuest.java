package pl.betoncraft.betonquest;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import pl.betoncraft.betonquest.compatibility.Compatibility;
import pl.betoncraft.betonquest.conditions.AlternativeCondition;
import pl.betoncraft.betonquest.conditions.ArmorCondition;
import pl.betoncraft.betonquest.conditions.ArmorRatingCondition;
import pl.betoncraft.betonquest.conditions.ConjunctionCondition;
import pl.betoncraft.betonquest.conditions.EffectCondition;
import pl.betoncraft.betonquest.conditions.ExperienceCondition;
import pl.betoncraft.betonquest.conditions.HandCondition;
import pl.betoncraft.betonquest.conditions.HealthCondition;
import pl.betoncraft.betonquest.conditions.HeightCondition;
import pl.betoncraft.betonquest.conditions.ItemCondition;
import pl.betoncraft.betonquest.conditions.LocationCondition;
import pl.betoncraft.betonquest.conditions.PermissionCondition;
import pl.betoncraft.betonquest.conditions.PointCondition;
import pl.betoncraft.betonquest.conditions.TagCondition;
import pl.betoncraft.betonquest.conditions.TimeCondition;
import pl.betoncraft.betonquest.conditions.WeatherCondition;
import pl.betoncraft.betonquest.core.Condition;
import pl.betoncraft.betonquest.core.Journal;
import pl.betoncraft.betonquest.core.JournalRes;
import pl.betoncraft.betonquest.core.Objective;
import pl.betoncraft.betonquest.core.ObjectiveRes;
import pl.betoncraft.betonquest.core.Point;
import pl.betoncraft.betonquest.core.PointRes;
import pl.betoncraft.betonquest.core.Pointer;
import pl.betoncraft.betonquest.core.QuestEvent;
import pl.betoncraft.betonquest.core.TagRes;
import pl.betoncraft.betonquest.database.ConfigUpdater;
import pl.betoncraft.betonquest.database.Database;
import pl.betoncraft.betonquest.database.Metrics;
import pl.betoncraft.betonquest.database.MySQL;
import pl.betoncraft.betonquest.database.QueryType;
import pl.betoncraft.betonquest.database.SQLite;
import pl.betoncraft.betonquest.database.UpdateType;
import pl.betoncraft.betonquest.database.Updater;
import pl.betoncraft.betonquest.database.Updater.UpdateResult;
import pl.betoncraft.betonquest.editor.Editor;
import pl.betoncraft.betonquest.events.CommandEvent;
import pl.betoncraft.betonquest.events.ConversationEvent;
import pl.betoncraft.betonquest.events.DeleteObjectiveEvent;
import pl.betoncraft.betonquest.events.EffectEvent;
import pl.betoncraft.betonquest.events.ExplosionEvent;
import pl.betoncraft.betonquest.events.FolderEvent;
import pl.betoncraft.betonquest.events.GiveEvent;
import pl.betoncraft.betonquest.events.JournalEvent;
import pl.betoncraft.betonquest.events.KillEvent;
import pl.betoncraft.betonquest.events.LightningEvent;
import pl.betoncraft.betonquest.events.MessageEvent;
import pl.betoncraft.betonquest.events.ObjectiveEvent;
import pl.betoncraft.betonquest.events.PointEvent;
import pl.betoncraft.betonquest.events.SetBlockEvent;
import pl.betoncraft.betonquest.events.SpawnMobEvent;
import pl.betoncraft.betonquest.events.TagEvent;
import pl.betoncraft.betonquest.events.TakeEvent;
import pl.betoncraft.betonquest.events.TeleportEvent;
import pl.betoncraft.betonquest.events.TimeEvent;
import pl.betoncraft.betonquest.events.WeatherEvent;
import pl.betoncraft.betonquest.inout.ConfigInput;
import pl.betoncraft.betonquest.inout.ConversationContainer;
import pl.betoncraft.betonquest.inout.CubeNPCListener;
import pl.betoncraft.betonquest.inout.GlobalLocations;
import pl.betoncraft.betonquest.inout.JoinQuitListener;
import pl.betoncraft.betonquest.inout.JournalBook;
import pl.betoncraft.betonquest.inout.JournalCommand;
import pl.betoncraft.betonquest.inout.ObjectiveSaving;
import pl.betoncraft.betonquest.inout.PlayerConverter;
import pl.betoncraft.betonquest.inout.QuestCommand;
import pl.betoncraft.betonquest.objectives.ActionObjective;
import pl.betoncraft.betonquest.objectives.BlockObjective;
import pl.betoncraft.betonquest.objectives.CraftingObjective;
import pl.betoncraft.betonquest.objectives.DelayObjective;
import pl.betoncraft.betonquest.objectives.DieObjective;
import pl.betoncraft.betonquest.objectives.LocationObjective;
import pl.betoncraft.betonquest.objectives.MobKillObjective;
import pl.betoncraft.betonquest.objectives.SmeltingObjective;
import pl.betoncraft.betonquest.objectives.TameObjective;

/**
 * Represents BetonQuest plugin
 * 
 * @authors Co0sh, Dzejkop, BYK
 */
public final class BetonQuest extends JavaPlugin {

	private static BetonQuest instance;
	private Database database;
	private boolean isMySQLUsed;
	private Editor editor;

	private HashMap<String, Class<? extends Condition>> conditions = new HashMap<String, Class<? extends Condition>>();
	private HashMap<String, Class<? extends QuestEvent>> events = new HashMap<String, Class<? extends QuestEvent>>();
	private HashMap<String, Class<? extends Objective>> objectives = new HashMap<String, Class<? extends Objective>>();

	private ConcurrentHashMap<String, ObjectiveRes> objectiveRes = new ConcurrentHashMap<String, ObjectiveRes>();
	private ConcurrentHashMap<String, TagRes> stringsRes = new ConcurrentHashMap<String, TagRes>();
	private ConcurrentHashMap<String, JournalRes> journalRes = new ConcurrentHashMap<String, JournalRes>();
	private ConcurrentHashMap<String, PointRes> pointRes = new ConcurrentHashMap<String, PointRes>();

	private HashMap<String, List<String>> playerTags = new HashMap<String, List<String>>();
	private HashMap<String, Journal> journals = new HashMap<String, Journal>();
	private HashMap<String, List<Point>> points = new HashMap<String, List<Point>>();

	private List<ObjectiveSaving> saving = new ArrayList<ObjectiveSaving>();

	@Override
	public void onEnable() {

		instance = this;

		new ConfigInput();

		String autoIncrement;
		// try to connect to database
		this.database = new MySQL(this, getConfig().getString("mysql.host"), getConfig().getString("mysql.port"), getConfig()
				.getString("mysql.base"), getConfig().getString("mysql.user"), getConfig().getString("mysql.pass"));

		// try to connect to MySQL
		if (database.openConnection() != null) {
			BetonQuest.getInstance().getLogger().info("Using MySQL for storing data!");
			isMySQLUsed = true;
			autoIncrement = "AUTO_INCREMENT";
			database.closeConnection();
			// if it fails use SQLite
		} else {
			this.database = new SQLite(this, "database.db");
			BetonQuest.getInstance().getLogger().info("Using SQLite for storing data!");
			isMySQLUsed = false;
			autoIncrement = "AUTOINCREMENT";
		}

		// create tables if they don't exist
		Connection connection = database.openConnection();
		try {
			connection
					.createStatement()
					.executeUpdate(
							"CREATE TABLE IF NOT EXISTS objectives (id INTEGER PRIMARY KEY "
									+ autoIncrement
									+ ", playerID VARCHAR(256) NOT NULL, instructions VARCHAR(2048) NOT NULL, isused BOOLEAN NOT NULL DEFAULT 0);");
			connection.createStatement().executeUpdate(
					"CREATE TABLE IF NOT EXISTS tags (id INTEGER PRIMARY KEY " + autoIncrement
							+ ", playerID VARCHAR(256) NOT NULL, tag TEXT NOT NULL, isused BOOLEAN NOT NULL DEFAULT 0);");
			connection.createStatement().executeUpdate(
					"CREATE TABLE IF NOT EXISTS points (id INTEGER PRIMARY KEY " + autoIncrement
							+ ", playerID VARCHAR(256) NOT NULL, category VARCHAR(256) NOT NULL, count INT NOT NULL);");
			connection.createStatement().executeUpdate(
					"CREATE TABLE IF NOT EXISTS journal (id INTEGER PRIMARY KEY " + autoIncrement
							+ ", playerID VARCHAR(256) NOT NULL, pointer VARCHAR(256) NOT NULL, date TIMESTAMP NOT NULL);");
		} catch (SQLException e) {
			e.printStackTrace();
		}
		database.closeConnection();

		// update configs
		new ConfigUpdater();
		
		// register ConversationContainer
		new ConversationContainer();

		// instantiating of these important things
		new JoinQuitListener();

		// instantiate default conversation start listener
		new CubeNPCListener();

		new JournalBook();
		new GlobalLocations().runTaskTimer(this, 0, 20);

		getCommand("q").setExecutor(new QuestCommand());
		getCommand("j").setExecutor(new JournalCommand());

		// register conditions
		registerConditions("health", HealthCondition.class);
		registerConditions("permission", PermissionCondition.class);
		registerConditions("experience", ExperienceCondition.class);
		registerConditions("tag", TagCondition.class);
		registerConditions("point", PointCondition.class);
		registerConditions("and", ConjunctionCondition.class);
		registerConditions("or", AlternativeCondition.class);
		registerConditions("time", TimeCondition.class);
		registerConditions("weather", WeatherCondition.class);
		registerConditions("height", HeightCondition.class);
		registerConditions("item", ItemCondition.class);
		registerConditions("hand", HandCondition.class);
		registerConditions("location", LocationCondition.class);
		registerConditions("armor", ArmorCondition.class);
		registerConditions("effect", EffectCondition.class);
		registerConditions("rating", ArmorRatingCondition.class);

		// register events
		registerEvents("message", MessageEvent.class);
		registerEvents("objective", ObjectiveEvent.class);
		registerEvents("command", CommandEvent.class);
		registerEvents("tag", TagEvent.class);
		registerEvents("journal", JournalEvent.class);
		registerEvents("teleport", TeleportEvent.class);
		registerEvents("explosion", ExplosionEvent.class);
		registerEvents("lightning", LightningEvent.class);
		registerEvents("point", PointEvent.class);
		registerEvents("delete", DeleteObjectiveEvent.class);
		registerEvents("give", GiveEvent.class);
		registerEvents("take", TakeEvent.class);
		registerEvents("conversation", ConversationEvent.class);
		registerEvents("kill", KillEvent.class);
		registerEvents("effect", EffectEvent.class);
		registerEvents("spawn", SpawnMobEvent.class);
		registerEvents("time", TimeEvent.class);
		registerEvents("weather", WeatherEvent.class);
		registerEvents("folder", FolderEvent.class);
		registerEvents("setblock", SetBlockEvent.class);

		// register objectives
		registerObjectives("location", LocationObjective.class);
		registerObjectives("block", BlockObjective.class);
		registerObjectives("mobkill", MobKillObjective.class);
		registerObjectives("action", ActionObjective.class);
		registerObjectives("die", DieObjective.class);
		registerObjectives("craft", CraftingObjective.class);
		registerObjectives("smelt", SmeltingObjective.class);
		registerObjectives("tame", TameObjective.class);
		registerObjectives("delay", DelayObjective.class);

		new Compatibility();
		
		// initialize PlayerConverter
		PlayerConverter.getType();
		
		// load objectives for all online players (in case of reload)
		for (Player player : Bukkit.getOnlinePlayers()) {
			database.openConnection();
			loadAllPlayerData(PlayerConverter.getID(player));
			loadObjectives(PlayerConverter.getID(player));
			loadPlayerTags(PlayerConverter.getID(player));
			loadJournal(PlayerConverter.getID(player));
			loadPlayerPoints(PlayerConverter.getID(player));
			database.closeConnection();
		}

		// metrics!
		if (getConfig().getString("metrics").equalsIgnoreCase("true")) {
			try {
				Metrics metrics = new Metrics(this);
				metrics.start();
				getLogger().info("Metrics enabled!");
			} catch (IOException e) {
				getLogger().info("Metrics faild to enable!");
			}
		} else {
			getLogger().info("Metrics are not used!");
		}
		
		// updater!
		if (getConfig().getString("autoupdate").equalsIgnoreCase("true")) {
			getLogger().info("AutoUpdater enabled!");
		} else {
			getLogger().info("AutoUpdater disabled!");
		}
		
		// editor
		if (getConfig().getString("editor.enabled").equalsIgnoreCase("true")) {
			try {
		        editor = new Editor();
		        editor.start();
				getLogger().info("Editor enabled on port " + editor.getPort());
			} catch (Exception e) {
				getLogger().info("Could not enable the Editor:");
				e.printStackTrace();
			}
			
		}

		// done
		getLogger().log(Level.INFO, "BetonQuest succesfully enabled!");
	}

	@Override
	public void onDisable() {
		editor.stop();
		database.openConnection();
		// create array and put there objectives (to avoid concurrent
		// modification exception)
		List<ObjectiveSaving> list = new ArrayList<ObjectiveSaving>();
		// save all active objectives to database
		for (ObjectiveSaving objective : saving) {
			list.add(objective);
		}
		for (ObjectiveSaving objective : list) {
			objective.saveObjective();
		}
		for (Player player : Bukkit.getOnlinePlayers()) {
			JournalBook.removeJournal(PlayerConverter.getID(player));
			saveJournal(PlayerConverter.getID(player));
			savePlayerTags(PlayerConverter.getID(player));
			savePlayerPoints(PlayerConverter.getID(player));
			BetonQuest.getInstance().getDB()
					.updateSQL(UpdateType.DELETE_USED_OBJECTIVES, new String[] { PlayerConverter.getID(player) });
		}
		database.closeConnection();
		
		if (getConfig().getString("autoupdate").equalsIgnoreCase("true")) {
			Updater updater = new Updater(this, 86448, this.getFile(), Updater.UpdateType.DEFAULT, false);
			if (updater.getResult().equals(UpdateResult.SUCCESS)) {
				getLogger().info("Found " + updater.getLatestName() + " update on DBO and downloaded it! Plugin will be automatically updated on next restart.");
			}
		}
		
		getLogger().log(Level.INFO, "BetonQuest succesfully disabled!");
	}

	/**
	 * @return the plugin instance
	 */
	public static BetonQuest getInstance() {
		return instance;
	}

	public Database getDB() {
		return database;
	}

	/**
	 * Registers new condition classes by their names
	 * 
	 * @param name
	 * @param conditionClass
	 */
	public void registerConditions(String name, Class<? extends Condition> conditionClass) {
		conditions.put(name, conditionClass);
	}

	/**
	 * Registers new event classes by their names
	 * 
	 * @param name
	 * @param eventClass
	 */
	public void registerEvents(String name, Class<? extends QuestEvent> eventClass) {
		events.put(name, eventClass);
	}

	/**
	 * Registers new objective classes by their names
	 * 
	 * @param name
	 * @param objectiveClass
	 */
	public void registerObjectives(String name, Class<? extends Objective> objectiveClass) {
		objectives.put(name, objectiveClass);
	}

	/**
	 * returns Class object of condition with given name
	 * 
	 * @param name
	 * @return
	 */
	public Class<? extends Condition> getCondition(String name) {
		return conditions.get(name);
	}

	/**
	 * returns Class object of event with given name
	 * 
	 * @param name
	 * @return
	 */
	public Class<? extends QuestEvent> getEvent(String name) {
		return events.get(name);
	}

	/**
	 * returns Class object of objective with given name
	 * 
	 * @param name
	 * @return
	 */
	public Class<? extends Objective> getObjective(String name) {
		return objectives.get(name);
	}

	/**
	 * stores pointer to ObjectiveSaving instance in order to store it on
	 * disable
	 * 
	 * @param object
	 */
	public void putObjectiveSaving(ObjectiveSaving object) {
		saving.add(object);
	}

	/**
	 * deletes pointer to ObjectiveSaving instance in case the objective was
	 * completed and needs to be deleted
	 * 
	 * @param object
	 */
	public void deleteObjectiveSaving(ObjectiveSaving object) {
		saving.remove(object);
	}

	/**
	 * loads from database all objectives of given player
	 * 
	 * @param playerID
	 */
	public void loadObjectives(String playerID) {
		ObjectiveRes res = objectiveRes.get(playerID);
		while (res.next()) {
			BetonQuest.objective(playerID, res.getInstruction());
		}
		objectiveRes.remove(playerID);
	}

	/**
	 * returns if the condition described by conditionID is met
	 * 
	 * @param conditionID
	 * @return
	 */
	public static boolean condition(String playerID, String conditionID) {
		String conditionInstruction = ConfigInput.getString("conditions." + conditionID);
		if (conditionInstruction == null) {
			BetonQuest.getInstance().getLogger().severe("Error while fetching condition: " + conditionID);
			return false;
		}
		boolean inverted = conditionInstruction.contains("--inverted");
		String[] parts = conditionInstruction.split(" ");
		Class<? extends Condition> condition = BetonQuest.getInstance().getCondition(parts[0]);
		Condition instance = null;
		if (condition == null) {
			BetonQuest.getInstance().getLogger().severe("Condition type not defined, in: " + conditionID);
			return false;
		}
		try {
			instance = condition.getConstructor(String.class, String.class).newInstance(playerID, conditionInstruction);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
			// return false for safety
			return false;
		}
		return (instance.isMet() && !inverted) || (!instance.isMet() && inverted);
	}

	/**
	 * fires the event described by eventID
	 * 
	 * @param eventID
	 */
	public static void event(String playerID, String eventID) {
		String eventInstruction = ConfigInput.getString("events." + eventID);
		if (eventInstruction == null) {
			BetonQuest.getInstance().getLogger().severe("Error while fetching event: " + eventID);
			return;
		}
		String[] parts = eventInstruction.split(" ");
		for (String part : parts) {
			if (part.startsWith("event_conditions:")) {
				String[] conditions = part.substring(17).split(",");
				for (String condition : conditions) {
					if (!condition(playerID, condition)) {
						return;
					}
				}
				break;
			}
		}
		Class<? extends QuestEvent> event = BetonQuest.getInstance().getEvent(parts[0]);
		if (event == null) {
			BetonQuest.getInstance().getLogger().severe("Event type not defined, in: " + eventID);
			return;
		}
		try {
			event.getConstructor(String.class, String.class).newInstance(playerID, eventInstruction);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}

	/**
	 * creates new objective for given player
	 * 
	 * @param playerID
	 * @param instruction
	 */
	public static void objective(String playerID, String instruction) {
		if (instruction == null) {
			BetonQuest.getInstance().getLogger().severe("Error while creating objective.");
			return;
		}
		String[] parts = instruction.split(" ");
		String tag = null;
		for (String part : parts) {
			if (part.contains("tag:")) {
				tag = part.substring(4);
				break;
			}
		}
		if (tag == null) {
			BetonQuest.getInstance().getLogger().severe("Tag not found in: " + instruction);
			return;
		}
		Class<? extends Objective> objective = BetonQuest.getInstance().getObjective(parts[0]);
		if (objective == null) {
			BetonQuest.getInstance().getLogger().severe("Objective type not defined, in: " + tag);
			return;
		}
		try {
			objective.getConstructor(String.class, String.class).newInstance(playerID, instruction);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Loads point objects for specified player
	 * 
	 * @param playerID
	 */
	public void loadPlayerPoints(String playerID) {
		PointRes res = pointRes.get(playerID);
		while (res.next()) {
			putPlayerPoints(playerID, res.getPoint());
		}
		pointRes.remove(playerID);
	}

	/**
	 * puts points in player's list
	 * 
	 * @param playerID
	 * @param points
	 */
	public void putPlayerPoints(String playerID, Point points) {
		if (!this.points.containsKey(playerID)) {
			this.points.put(playerID, new ArrayList<Point>());
		}
		this.points.get(playerID).add(points);
	}

	/**
	 * Saves player's points to database
	 * 
	 * @param playerID
	 */
	public void savePlayerPoints(String playerID) {
		List<Point> points = this.points.remove(playerID);
		if (points == null) {
			return;
		}
		database.updateSQL(UpdateType.DELETE_POINTS, new String[] { playerID });
		for (Point point : points) {
			database.updateSQL(UpdateType.ADD_POINTS, new String[] { playerID, point.getCategory(), point.getCount() + "" });
		}
	}

	/**
	 * returns how many points from given category the player has
	 * 
	 * @param playerID
	 * @param category
	 * @return
	 */
	public int getPlayerPoints(String playerID, String category) {
		List<Point> points = this.points.get(playerID);
		if (points == null) {
			return 0;
		}
		for (Point point : points) {
			if (point.getCategory().equalsIgnoreCase(category)) {
				return point.getCount();
			}
		}
		return 0;
	}

	/**
	 * adds points to specified category
	 * 
	 * @param playerID
	 * @param category
	 * @param count
	 */
	public void addPlayerPoints(String playerID, String category, int count) {
		List<Point> points = this.points.get(playerID);
		if (points == null) {
			this.points.put(playerID, new ArrayList<Point>());
			this.points.get(playerID).add(new Point(category, count));
			return;
		}
		for (Point point : points) {
			if (point.getCategory().equalsIgnoreCase(category)) {
				point.addPoints(count);
				return;
			}
		}
		this.points.get(playerID).add(new Point(category, count));
	}

	/**
	 * loads tags (tags) for specified player
	 * 
	 * @param playerID
	 */
	public void loadPlayerTags(String playerID) {
		TagRes res = stringsRes.get(playerID);
		while (res.next()) {
			putPlayerTag(playerID, res.getTag());
		}
		stringsRes.remove(playerID);
	}

	/**
	 * Puts a tag in player's list
	 * 
	 * @param playerID
	 * @param tag
	 */
	public void putPlayerTag(String playerID, String tag) {
		if (!playerTags.containsKey(playerID)) {
			playerTags.put(playerID, new ArrayList<String>());
		}
		playerTags.get(playerID).add(tag);
	}

	/**
	 * @return the playerTags
	 */
	public HashMap<String, List<String>> getPlayerTags() {
		return playerTags;
	}

	/**
	 * Checks if player has specified tag in his list
	 * 
	 * @param playerID
	 * @param tag
	 * @return
	 */
	public boolean havePlayerTag(String playerID, String tag) {
		if (!playerTags.containsKey(playerID)) {
			return false;
		}
		return playerTags.get(playerID).contains(tag);
	}

	/**
	 * Removes specified tag from player's list
	 * 
	 * @param playerID
	 * @param tag
	 */
	public void removePlayerTag(String playerID, String tag) {
		if (playerTags.containsKey(playerID)) {
			playerTags.get(playerID).remove(tag);
		}
	}

	/**
	 * Removes player's list from HashMap and returns it (eg. for storing in
	 * database)
	 * 
	 * @param playerID
	 */
	public void savePlayerTags(final String playerID) {
		List<String> tags = playerTags.remove(playerID);
		if (tags == null) {
			return;
		}
		database.updateSQL(UpdateType.DELETE_TAGS, new String[] { playerID });
		for (String tag : tags) {
			database.updateSQL(UpdateType.ADD_TAGS, new String[] { playerID, tag });
		}
	}

	/**
	 * loads journal of specified player
	 * 
	 * @param playerID
	 */
	public void loadJournal(String playerID) {
		journals.put(playerID, new Journal(playerID));
	}

	/**
	 * returns journal of specified player
	 * 
	 * @param playerID
	 * @return
	 */
	public Journal getJournal(String playerID) {
		return journals.get(playerID);
	}

	/**
	 * saves player's journal
	 * 
	 * @param playerID
	 */
	public void saveJournal(final String playerID) {
		database.updateSQL(UpdateType.DELETE_JOURNAL, new String[] { playerID });
		Journal journal = journals.remove(playerID);
		if (journal == null) {
			return;
		}
		List<Pointer> pointers = journal.getPointers();
		for (Pointer pointer : pointers) {
			database.updateSQL(UpdateType.ADD_JOURNAL, new String[] { playerID, pointer.getPointer(),
					pointer.getTimestamp().toString() });
		}
	}

	/**
	 * @return the objectiveRes
	 */
	public ConcurrentHashMap<String, ObjectiveRes> getObjectiveRes() {
		return objectiveRes;
	}

	/**
	 * @return the stringsRes
	 */
	public ConcurrentHashMap<String, TagRes> getTagsRes() {
		return stringsRes;
	}

	/**
	 * @return the journalRes
	 */
	public ConcurrentHashMap<String, JournalRes> getJournalRes() {
		return journalRes;
	}

	/**
	 * @return the pointRes
	 */
	public ConcurrentHashMap<String, PointRes> getPointRes() {
		return pointRes;
	}

	/**
	 * loads all player data from database and puts it to concurrent HashMap, so
	 * it's safe to call it in async thread
	 * 
	 * @param playerID
	 */
	public void loadAllPlayerData(String playerID) {
		try {
			// load objectives
			ResultSet res1 = database.querySQL(QueryType.SELECT_USED_OBJECTIVES, new String[] { playerID });
			if (!res1.isBeforeFirst()) {
				res1 = database.querySQL(QueryType.SELECT_UNUSED_OBJECTIVES, new String[] { playerID });
			}
			getObjectiveRes().put(playerID, new ObjectiveRes(res1));
			database.updateSQL(UpdateType.UPDATE_OBJECTIVES, new String[] { playerID });
			// load tags
			ResultSet res2 = database.querySQL(QueryType.SELECT_USED_TAGS, new String[] { playerID });
			if (!res2.isBeforeFirst()) {
				res2 = database.querySQL(QueryType.SELECT_UNUSED_TAGS, new String[] { playerID });
			}
			getTagsRes().put(playerID, new TagRes(res2));
			database.updateSQL(UpdateType.UPDATE_TAGS, new String[] { playerID });
			// load journals
			ResultSet res3 = database.querySQL(QueryType.SELECT_JOURNAL, new String[] { playerID });
			getJournalRes().put(playerID, new JournalRes(res3));
			// load points
			ResultSet res4 = database.querySQL(QueryType.SELECT_POINTS, new String[] { playerID });
			getPointRes().put(playerID, new PointRes(res4));
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void deleteObjective(String playerID, String tag) {
		List<ObjectiveSaving> list = new ArrayList<>();
		for (ObjectiveSaving objective : saving) {
			if (objective.getPlayerID().equals(playerID) && objective.getTag() != null
					&& objective.getTag().equalsIgnoreCase(tag)) {
				list.add(objective);
			}
		}
		for (ObjectiveSaving objective : list) {
			objective.deleteThis();
		}
	}

	public List<ObjectiveSaving> getObjectives(String playerID) {
		List<ObjectiveSaving> list = new ArrayList<ObjectiveSaving>();
		for (ObjectiveSaving objective : saving) {
			if (objective.getPlayerID().equals(playerID) && objective.getTag() != null) {
				list.add(objective);
			}
		}
		return list;
	}

	/**
	 * Purges player's objectives. Player MUST be online!
	 * 
	 * @param playerID
	 */
	public void purgePlayer(final String playerID) {
		if (playerTags.get(playerID) != null) {
			playerTags.get(playerID).clear();
		}
		if (journals.get(playerID) != null) {
			journals.get(playerID).clear();
		}
		if (points.get(playerID) != null) {
			points.get(playerID).clear();
		}
		List<ObjectiveSaving> list = new ArrayList<ObjectiveSaving>();
		Iterator<ObjectiveSaving> iterator = saving.iterator();
		while (iterator.hasNext()) {
			ObjectiveSaving objective = (ObjectiveSaving) iterator.next();
			if (objective.getPlayerID().equals(playerID)) {
				list.add(objective);
			}
		}
		database.openConnection();
		for (ObjectiveSaving objective : list) {
			objective.saveObjective();
		}
		database.closeConnection();
		if (PlayerConverter.getPlayer(playerID) != null) {
			JournalBook.updateJournal(playerID);
		}
		if (isMySQLUsed) {
			new BukkitRunnable() {
				@Override
				public void run() {
					database.openConnection();
					database.updateSQL(UpdateType.DELETE_ALL_OBJECTIVES, new String[] { playerID });
					database.updateSQL(UpdateType.DELETE_JOURNAL, new String[] { playerID });
					database.updateSQL(UpdateType.DELETE_POINTS, new String[] { playerID });
					database.updateSQL(UpdateType.DELETE_TAGS, new String[] { playerID });
				}
			}.runTaskAsynchronously(BetonQuest.getInstance());
		} else {
			database.openConnection();
			database.updateSQL(UpdateType.DELETE_ALL_OBJECTIVES, new String[] { playerID });
			database.updateSQL(UpdateType.DELETE_JOURNAL, new String[] { playerID });
			database.updateSQL(UpdateType.DELETE_POINTS, new String[] { playerID });
			database.updateSQL(UpdateType.DELETE_TAGS, new String[] { playerID });
			database.closeConnection();
		}
	}

	/**
	 * @return if MySQL is uset (false means SQLite)
	 */
	public boolean isMySQLUsed() {
		return isMySQLUsed;
	}
}
