import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;
import javax.security.auth.login.LoginException;
import java.sql.*;

public class Main extends ListenerAdapter {
    public static void main(String[] args) throws LoginException {
        JDABuilder builder = new JDABuilder(AccountType.BOT);
        String token = "NDk2MDA3MDM5OTY1OTg2ODI3.DpKWGw.qKC_6jhqJIVBLg-iNOEc21Q2CgM";
        builder.setToken(token);
        builder.addEventListener(new Main());
        builder.buildAsync();
    }

    public boolean findinDatabase(String hash) {
        boolean found = false;
        try {
            String myUrl = "jdbc:mysql://localhost:3306/rpi?autoReconnect=true&useSSL=false";
            String myDriver = "org.gjt.mm.mysql.Driver";
            String username = "oaklea";
            String password = "Kakacarrotcake12";
            //establish connection with database with given credentials
            Connection conn = DriverManager.getConnection(myUrl, username, password);
            String queryCheck = "SELECT * from rpi.emails WHERE email_id = '" + hash + "'";
            PreparedStatement st = conn.prepareStatement(queryCheck);
            ResultSet rs = st.executeQuery(queryCheck);
            //if there is one row in the resultset, then the email exists
            if(rs.absolute(1)){
                conn.close();
                found = true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Cannot connect to the database!");
        }
        return found;
    }

    public void addtoDatabase(int hash, String email) {
        try {
            String myUrl = "jdbc:mysql://localhost:3306/rpi?autoReconnect=true&useSSL=false";
            String myDriver = "org.gjt.mm.mysql.Driver";
            String username = "oaklea";
            String password = "Kakacarrotcake12";
            //establish connection with database using given credentials
            Connection conn = DriverManager.getConnection(myUrl, username, password);
            String query = "insert into rpi.emails (email_id, email_String) values(?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);
            //prepare the hash and email to be inserted into the table
            preparedStmt.setInt(1, hash);
            preparedStmt.setString(2, email);
            //insert
            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new IllegalStateException("Cannot connect to the database!");
        }
    }


    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot()) {
            return;
        }
            System.out.println("We received a message from " +
                event.getAuthor().getName() + ": " +
                event.getMessage().getContentDisplay()
        );
        if(event.getMessage().getContentRaw().equals("!register")) {
            //send the author of "!register" a pm
            event.getAuthor().openPrivateChannel().queue((channel) -> {
                channel.sendMessage("What is your RPI e-mail?").queue();
                if(event.getAuthor().isBot()) {
                    return;
                }
            });
        }
    }
    public void onPrivateMessageReceived(PrivateMessageReceivedEvent event) {
        //to prevent the bot from talking to itself:
        if(event.getAuthor().isBot()) {
            return;
        }
        String event_return = event.getMessage().getContentRaw();
        if(event_return.contains("rpi.edu")) {//if they send an email
            int rand = event_return.hashCode();
            //add this rand to the database with the email
            addtoDatabase(rand, event_return);
            event.getChannel().sendMessage("Sent verification check to " + event_return + ". Check your e-mail!").queue();
            Mailer.send("trpi.auth@gmail.com", "goRPI2018!!", event_return, "teamRPI Verification", "Send this code back to the bot:\n" + rand);
            event.getChannel().sendMessage("Please enter the verification code.").queue();
            return;
        }
        if(!event_return.contains("rpi.edu"))  {//if they give the code
            boolean found = findinDatabase(event_return);
            if(found) {
                event.getChannel().sendMessage("Verified! Welcome to teamRPI!").queue();
                Guild g = event.getJDA().getGuildById("433828818982010880");
                GuildController gc = new GuildController(g);
                gc.addRolesToMember(g.getMember(event.getAuthor()), event.getJDA().getRolesByName("peon", true)).complete();
                return;
            } else {
                event.getChannel().sendMessage("Something went wrong. If you entered a code, make sure it matches the code sent in the e-mail exactly. If you entered an email, then this email is not valid. Try again.").queue();
                return;
            }
        }


    }

}
