package me.dablakbandit.bankbaltop;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.bukkit.plugin.Plugin;

import me.dablakbandit.core.configuration.PluginConfiguration;

public class BankBalTopConfiguration extends PluginConfiguration{
	
	public static BankBalTopConfiguration	configuration;
	
	public static StringListPath			COMMANDS	= new StringListPath("Commands", Arrays.asList("balancetop", "baltop"));
	
	private BankBalTopConfiguration(Plugin plugin){
		super(plugin);
	}
	
	public static void setup(Plugin plugin){
		configuration = new BankBalTopConfiguration(plugin);
		load();
	}
	
	public static void load(){
		configuration.plugin.reloadConfig();
		try{
			boolean save = false;
			for(Field f : configuration.getClass().getDeclaredFields()){
				if(Path.class.isAssignableFrom(f.getType())){
					Path p = (Path)f.get(null);
					if(!save){
						save = p.retrieve(configuration.plugin.getConfig());
					}else{
						p.retrieve(configuration.plugin.getConfig());
					}
				}
			}
			if(save){
				configuration.plugin.saveConfig();
			}
		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static void reload(){
		load();
	}
}
