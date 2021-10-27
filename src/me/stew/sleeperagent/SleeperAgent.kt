package me.stew.sleeperagent

/* Imports */
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import java.io.File

/* Vars */
val agent = Agent()

lateinit var killer: Killer
lateinit var countdown: Countdown
lateinit var plugin: SleeperAgent

var stopDelay: Long = 60

/* Functions */
fun printf(string: String): Unit = Bukkit.getServer().consoleSender.sendMessage(string)

/* Classes */
//Tasks
class Agent: BukkitRunnable()
{
    private var enabled = true
    private var stopping = false

    override fun run()
    {
        if(enabled) if(Bukkit.getServer().onlinePlayers.isEmpty() && !stopping)
        {
            printf("${ChatColor.RED} No players, server stopping in $stopDelay seconds.")
            killer = Killer()
            killer.runTaskLater(plugin, 20 * stopDelay) //20 ticks in 1 second for 15 seconds
            countdown = Countdown()
            countdown.runTaskTimer(plugin, (20 * stopDelay) - 100, 20) //5 secs * 20 = 100 ticks
            stopping = true
        }
        else
        {
            if(!Bukkit.getServer().onlinePlayers.isEmpty() && stopping) {
                printf("${ChatColor.GREEN} Player(s) online, canceling server stop.")
                killer.cancel()
                countdown.cancel()
                stopping = false
            }
        }
    }

    fun disable()
    {
        var extra = "."

        if(stopping)
        {
            killer.cancel()
            countdown.cancel()
            stopping = false
            extra = ", countdown stopped."
        }

        enabled = false

        printf("${ChatColor.YELLOW} Sleeper disabled$extra")
    }

    fun enable()
    {
        printf("${ChatColor.YELLOW} Sleeper enabled.")
        enabled = true
    }
}

class Killer: BukkitRunnable() {override fun run(): Unit = Bukkit.getServer().shutdown()}

class Countdown: BukkitRunnable() { //delay for total ticks - 5, interval 20 ticks
    private var count = 5 //might make configurable, probably log colors too
    override fun run(): Unit = printf("${ChatColor.YELLOW} Server stopping in ${count--} seconds.")
}

//Commands
class CmdHandler: CommandExecutor
{
    override fun onCommand(snd: CommandSender, cmd: Command, lab: String, arg: Array<out String>?): Boolean
    {
        return if(snd is ConsoleCommandSender)
        {
            when(cmd.name)
            {
                "sleeperon"  -> agent.enable()
                "sleeperoff" -> agent.disable()
            }
            true
        } else false
    }
}

//Main
class SleeperAgent: JavaPlugin()
{
    override fun onEnable()
    {
        plugin = this

        val fp = File("${this.dataFolder}config.yml")
        if(!fp.exists())
        {
            this.saveDefaultConfig()
            this.reloadConfig()
        }
        stopDelay = this.config["stopDelay"].toString().toLong()

        val handler = CmdHandler()

        this.getCommand("sleeperon")?.setExecutor(handler)
        this.getCommand("sleeperoff")?.setExecutor(handler)

        agent.runTaskTimer(this, 0, 0)
    }

    override fun onDisable(): Unit = agent.cancel()
}
