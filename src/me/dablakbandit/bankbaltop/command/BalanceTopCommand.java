package me.dablakbandit.bankbaltop.command;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import com.earth2me.essentials.CommandSource;
import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.I18n;
import com.earth2me.essentials.User;
import com.earth2me.essentials.textreader.SimpleTextInput;
import com.earth2me.essentials.textreader.TextPager;
import com.earth2me.essentials.utils.NumberUtil;

import me.dablakbandit.bank.api.BankAPI;
import me.dablakbandit.dabcore.command.AbstractCommand;
import net.ess3.api.IEssentials;

public class BalanceTopCommand extends AbstractCommand{
	
	public BalanceTopCommand(String command){
		super(command);
		register();
	}
	
	private static final int					CACHETIME	= 2 * 60 * 1000;
	public static final int						MINUSERS	= 50;
	private static final SimpleTextInput		cache		= new SimpleTextInput();
	private static long							cacheage	= 0;
	private static final ReentrantReadWriteLock	lock		= new ReentrantReadWriteLock();
	protected transient IEssentials				ess			= (Essentials)Bukkit.getPluginManager().getPlugin("Essentials");
	protected static final Logger				logger		= Logger.getLogger("Essentials");
	
	@Override
	public boolean onCommand(CommandSender s, Command cmd, String commandLabel, String[] args){
		CommandSource sender = new CommandSource(s);
		int page = 0;
		boolean force = false;
		if(args.length > 0){
			try{
				page = Integer.parseInt(args[0]);
			}catch(NumberFormatException ex){
				if(args[0].equalsIgnoreCase("force") && (!sender.isPlayer() || ess.getUser(sender.getPlayer()).isAuthorized("essentials.balancetop.force"))){
					force = true;
				}
			}
		}
		
		if(!force && lock.readLock().tryLock()){
			try{
				if(cacheage > System.currentTimeMillis() - CACHETIME){
					outputCache(sender, commandLabel, page);
					return true;
				}
				if(ess.getUserMap().getUniqueUsers() > MINUSERS){
					sender.sendMessage(I18n.tl("orderBalances", ess.getUserMap().getUniqueUsers()));
				}
			}finally{
				lock.readLock().unlock();
			}
			ess.runTaskAsynchronously(new Viewer(sender, commandLabel, page, force));
		}else{
			if(ess.getUserMap().getUniqueUsers() > MINUSERS){
				sender.sendMessage(I18n.tl("orderBalances", ess.getUserMap().getUniqueUsers()));
			}
			ess.runTaskAsynchronously(new Viewer(sender, commandLabel, page, force));
		}
		return true;
	}
	
	private static void outputCache(final CommandSource sender, String command, int page){
		final Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(cacheage);
		final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
		sender.sendMessage(I18n.tl("balanceTop", format.format(cal.getTime())));
		new TextPager(cache).showPage(Integer.toString(page), null, "balancetop", sender);
	}
	
	private class Calculator implements Runnable{
		private final transient Viewer	viewer;
		private final boolean			force;
		
		public Calculator(final Viewer viewer, final boolean force){
			this.viewer = viewer;
			this.force = force;
		}
		
		@Override
		public void run(){
			lock.writeLock().lock();
			try{
				if(force || cacheage <= System.currentTimeMillis() - CACHETIME){
					cache.getLines().clear();
					final Map<String, BigDecimal> balances = new HashMap<String, BigDecimal>();
					BigDecimal totalMoney = BigDecimal.ZERO;
					if(ess.getSettings().isEcoDisabled()){
						if(ess.getSettings().isDebug()){
							ess.getLogger().info("Internal economy functions disabled, aborting baltop.");
						}
					}else{
						for(UUID u : ess.getUserMap().getAllUniqueUsers()){
							final User user = ess.getUserMap().getUser(u);
							if(user != null){
								if(!ess.getSettings().isNpcsInBalanceRanking() && user.isNPC()){
									// Don't list NPCs in output
									continue;
								}
								BigDecimal userMoney = user.getMoney();
								user.updateMoneyCache(userMoney);
								userMoney = userMoney.add(new BigDecimal(BankAPI.getInstance().getMoney(u.toString())));
								totalMoney = totalMoney.add(userMoney);
								final String name = user.isHidden() ? user.getName() : user.getDisplayName();
								balances.put(name, userMoney);
							}
						}
					}
					
					final List<Map.Entry<String, BigDecimal>> sortedEntries = new ArrayList<Map.Entry<String, BigDecimal>>(balances.entrySet());
					Collections.sort(sortedEntries, new Comparator<Map.Entry<String, BigDecimal>>(){
						@Override
						public int compare(final Entry<String, BigDecimal> entry1, final Entry<String, BigDecimal> entry2){
							return entry2.getValue().compareTo(entry1.getValue());
						}
					});
					
					cache.getLines().add(I18n.tl("serverTotal", NumberUtil.displayCurrency(totalMoney, ess)));
					int pos = 1;
					for(Map.Entry<String, BigDecimal> entry : sortedEntries){
						cache.getLines().add(pos + ". " + entry.getKey() + ": " + NumberUtil.displayCurrency(entry.getValue(), ess));
						pos++;
					}
					cacheage = System.currentTimeMillis();
				}
			}finally{
				lock.writeLock().unlock();
			}
			ess.runTaskAsynchronously(viewer);
		}
	}
	
	private class Viewer implements Runnable{
		private final transient CommandSource	sender;
		private final transient int				page;
		private final transient boolean			force;
		private final transient String			commandLabel;
		
		public Viewer(final CommandSource sender, final String commandLabel, final int page, final boolean force){
			this.sender = sender;
			this.page = page;
			this.force = force;
			this.commandLabel = commandLabel;
		}
		
		@Override
		public void run(){
			lock.readLock().lock();
			try{
				if(!force && cacheage > System.currentTimeMillis() - CACHETIME){
					outputCache(sender, commandLabel, page);
					return;
				}
			}finally{
				lock.readLock().unlock();
			}
			ess.runTaskAsynchronously(new Calculator(new Viewer(sender, commandLabel, page, false), force));
		}
	}
}
