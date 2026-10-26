package com.lino.skills;

import com.lino.skills.command.AdminCommandController;
import com.lino.skills.command.PlayerCommandController;
import com.lino.skills.command.StatsCommandController;
import com.lino.skills.events.AnglingHandler;
import com.lino.skills.events.BattleHandler;
import com.lino.skills.events.BreweryHandler;
import com.lino.skills.events.DurabilityHandler;
import com.lino.skills.events.EnchantmentHandler;
import com.lino.skills.events.FloatingTagHandler;
import com.lino.skills.events.HarvestHandler;
import com.lino.skills.events.RoamHandler;
import com.lino.skills.events.SessionHandler;
import com.lino.skills.events.SlayerHandler;
import com.lino.skills.integration.PapiExpansion;
import com.lino.skills.persistence.ProfileRepository;
import com.lino.skills.registry.ConfigRegistry;
import com.lino.skills.service.AttributeService;
import com.lino.skills.service.HudService;
import com.lino.skills.service.ManaService;
import com.lino.skills.service.ProgressionService;
import com.lino.skills.service.RankingService;
import com.lino.skills.ui.GuiRouter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class SkillsPlugin extends JavaPlugin {

    private ConfigRegistry configRegistry;
    private ProfileRepository profileRepository;
    private AttributeService attributeService;
    private ProgressionService progressionService;
    private ManaService manaService;
    private RankingService rankingService;
    private HudService hudService;

    @Override
    public void onEnable() {
        this.configRegistry = new ConfigRegistry(this);
        this.configRegistry.reloadAll();

        this.attributeService = new AttributeService(this);
        this.progressionService = new ProgressionService(this);
        this.manaService = new ManaService(this);
        this.profileRepository = new ProfileRepository(this);
        this.rankingService = new RankingService(this);
        this.hudService = new HudService(this);

        bindEventHandlers();
        bindCommands();
        bindIntegrations();

        for (Player online : Bukkit.getOnlinePlayers()) {
            profileRepository.loadProfileAsync(online);
        }

        getLogger().info("Skills v" + getDescription().getVersion() + " by Lino initialized.");
    }

    @Override
    public void onDisable() {
        if (progressionService != null) {
            progressionService.dismissAllBanners();
        }
        if (profileRepository != null) {
            profileRepository.shutdown();
        }
    }

    private void bindEventHandlers() {
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new SessionHandler(this), this);
        pm.registerEvents(new HarvestHandler(this), this);
        pm.registerEvents(new BattleHandler(this), this);
        pm.registerEvents(new SlayerHandler(this), this);
        pm.registerEvents(new AnglingHandler(this), this);
        pm.registerEvents(new EnchantmentHandler(this), this);
        pm.registerEvents(new BreweryHandler(this), this);
        pm.registerEvents(new RoamHandler(this), this);
        pm.registerEvents(new DurabilityHandler(this), this);
        pm.registerEvents(new FloatingTagHandler(this), this);
        pm.registerEvents(new GuiRouter(), this);
    }

    private void bindCommands() {
        AdminCommandController adminCtrl = new AdminCommandController(this);
        PlayerCommandController playerCtrl = new PlayerCommandController(this, adminCtrl);
        StatsCommandController statsCtrl = new StatsCommandController(this);

        Objects.requireNonNull(getCommand("skills")).setExecutor(playerCtrl);
        Objects.requireNonNull(getCommand("skills")).setTabCompleter(playerCtrl);

        Objects.requireNonNull(getCommand("stats")).setExecutor(statsCtrl);
        Objects.requireNonNull(getCommand("stats")).setTabCompleter(statsCtrl);

        Objects.requireNonNull(getCommand("skillsadmin")).setExecutor(adminCtrl);
        Objects.requireNonNull(getCommand("skillsadmin")).setTabCompleter(adminCtrl);
    }

    private void bindIntegrations() {
        if (getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new PapiExpansion(this).register();
        }
    }

    public ConfigRegistry getConfigRegistry() {
        return configRegistry;
    }

    public ProfileRepository getProfileRepository() {
        return profileRepository;
    }

    public AttributeService getAttributeService() {
        return attributeService;
    }

    public ProgressionService getProgressionService() {
        return progressionService;
    }

    public ManaService getManaService() {
        return manaService;
    }

    public RankingService getRankingService() {
        return rankingService;
    }

    public HudService getHudService() {
        return hudService;
    }
}
