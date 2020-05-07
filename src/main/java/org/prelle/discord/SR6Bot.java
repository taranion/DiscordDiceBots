/**
 * 
 */
package org.prelle.discord;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.security.auth.login.LoginException;

import org.prelle.discord.SR6DiceGraphicGenerator.DiceRollResult;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

/**
 * @author prelle
 *
 */
public class SR6Bot extends ListenerAdapter  {
	
	private static JDABuilder jdaBuilder;
	
	static {
		jdaBuilder = JDABuilder.createLight("<token>", 
				GatewayIntent.DIRECT_MESSAGES, 
				GatewayIntent.DIRECT_MESSAGE_TYPING,
				GatewayIntent.DIRECT_MESSAGE_REACTIONS,
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.GUILD_MESSAGE_TYPING,
				GatewayIntent.GUILD_MESSAGE_REACTIONS
				);
		
	}
	    
	    public SR6Bot() {
	        //We construct a builder for a BOT account. If we wanted to use a CLIENT account
	        // we would use AccountType.CLIENT
	        try
	        {
	        	JDA jda = jdaBuilder
	        			.build();
	        	jda.addEventListener(this);
	            jda.awaitReady(); // Blocking guarantees that JDA will be completely loaded.
	            System.out.println("Finished Building JDA!");
	        }
	        catch (LoginException e)
	        {
	            //If anything goes wrong in terms of authentication, this is the exception that will represent it
	            e.printStackTrace();
	        }
	        catch (InterruptedException e)
	        {
	            //Due to the fact that awaitReady is a blocking method, one which waits until JDA is fully loaded,
	            // the waiting can be interrupted. This is the exception that would fire in that situation.
	            //As a note: in this extremely simplified example this will never occur. In fact, this will never occur unless
	            // you use awaitReady in a thread that has the possibility of being interrupted (async thread usage and interrupts)
	            e.printStackTrace();
	        }
	    }

	    /**
	     * NOTE THE @Override!
	     * This method is actually overriding a method in the ListenerAdapter class! We place an @Override annotation
	     *  right before any method that is overriding another to guarantee to ourselves that it is actually overriding
	     *  a method from a super class properly. You should do this every time you override a method!
	     *
	     * As stated above, this method is overriding a hook method in the
	     * {@link net.dv8tion.jda.api.hooks.ListenerAdapter ListenerAdapter} class. It has convenience methods for all JDA events!
	     * Consider looking through the events it offers if you plan to use the ListenerAdapter.
	     *
	     * In this example, when a message is received it is printed to the console.
	     *
	     * @param event
	     *          An event containing information about a {@link net.dv8tion.jda.api.entities.Message Message} that was
	     *          sent in a channel.
	     */
	    @Override
	    public void onMessageReceived(MessageReceivedEvent event) {
	        //These are provided with every event in JDA
	        JDA jda = event.getJDA();                       //JDA, the core of the api.
	        long responseNumber = event.getResponseNumber();//The amount of discord events that JDA has received since the last reconnect.

	        //Event specific information
	        User author = event.getAuthor();                //The user that sent the message
	        Message message = event.getMessage();           //The message that was received.
	        MessageChannel channel = event.getChannel();    //This is the MessageChannel that the message was sent to.
	                                                        //  This could be a TextChannel, PrivateChannel, or Group!

	        String msg = message.getContentDisplay();              //This returns a human readable version of the Message. Similar to
	                                                        // what you would see in the client.

	        boolean bot = author.isBot();                    //This boolean is useful to determine if the User that
	                                                        // sent the Message is a BOT or not!
	        if (bot)
	        	return;

	        System.out.println("Received: "+msg+" from "+author);
	        if (event.isFromType(ChannelType.TEXT))         //If this message was sent to a Guild TextChannel
	        {
	            //Because we now know that this message was sent in a Guild, we can do guild specific things
	            // Note, if you don't check the ChannelType before using these methods, they might return null due
	            // the message possibly not being from a Guild!

	            Guild guild = event.getGuild();             //The Guild that this message was sent in. (note, in the API, Guilds are Servers)
	            TextChannel textChannel = event.getTextChannel(); //The TextChannel that this message was sent to.
	            Member member = event.getMember();          //This Member that sent the message. Contains Guild specific information about the User!

	            String name;
	            if (message.isWebhookMessage())
	            {
	                name = author.getName();                //If this is a Webhook message, then there is no Member associated
	            }                                           // with the User, thus we default to the author for name.
	            else
	            {
	                name = member.getEffectiveName();       //This will either use the Member's nickname if they have one,
	            }                                           // otherwise it will default to their username. (User#getName())

	            System.out.printf("(%s)[%s]<%s>: %s\n", guild.getName(), textChannel.getName(), name, msg);
	        } else if (event.isFromType(ChannelType.PRIVATE)) {
	            //The message was sent in a PrivateChannel.
	            //In this example we don't directly use the privateChannel, however, be sure, there are uses for it!
	            PrivateChannel privateChannel = event.getPrivateChannel();

	            System.out.printf("[PRIV]<%s>: %s\n", author.getName(), msg);
	        }

	        if (msg.equals("!help")) {
	            channel.sendMessage("The following commands exist:\n"+
	            		"!roll X     - Roll a pool of X dice\n" + 
	            		"!roll X Y   - Roll a pool of X dice, of which Y are wild die\n" + 
	            		"!roll X Y Z - as above, but with the 4-Edge boost, which adds \n" + 
	            		"              Z dices and let 6 explode")
	            .queue();
	        } else if (msg.startsWith("!roll")) {
	            String[] args = msg.split(" ");
	            int pool = Integer.parseInt(args[1]);
	            int wild = 0;
	            if (args.length>2)
	            	wild = Integer.parseInt(args[2]);
	            int edge = 0;
	            if (args.length>3)
	            	edge = Integer.parseInt(args[3]);
	            
	            EmbedBuilder embed = new EmbedBuilder();
	            embed.setTitle(author.getName());
	            DiceRollResult ret = SR6DiceGraphicGenerator.generate(embed, pool, wild, edge);
	            embed.setImage("attachment://result.png") // we specify this in sendFile as "cat.png"
	                 .setDescription(ret.message)
	                 .setColor(255*65536 + 0*256 + 0xA0)
	                 ;
	            channel.sendFile(ret.image, "result.png")
	            	.embed(embed.build())
	            	.queue();

	        }
	    }
}
