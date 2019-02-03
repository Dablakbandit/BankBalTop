package me.dablakbandit.bankbaltop;

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
		BankBalTopConfiguration.setup(this);
		for(String s : BankBalTopConfiguration.COMMANDS.get()){
			new BalanceTopCommand(s);
		}
	}
}
