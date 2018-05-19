package me.dablakbandit.bankbaltop;

import java.util.Arrays;
import java.util.List;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import me.dablakbandit.bankbaltop.command.BalanceTopCommand;

public class BankBalTopPlugin extends JavaPlugin{
	
	private static BankBalTopPlugin main;
	
	public static BankBalTopPlugin getInstance(){
		return main;
	}
	
	public void onLoad(){
		main = this;
	}
	
	public void onEnable(){
		FileConfiguration config = getConfig();
		reloadConfig();
		List<String> list;
		if(config.isSet("Commands")){
			list = config.getStringList("Commands");
		}else{
			list = Arrays.asList(new String[]{ "balancetop", "baltop" });
			config.set("Commands", list);
			saveConfig();
		}
		for(String s : list)
			new BalanceTopCommand(s);
	}
}
