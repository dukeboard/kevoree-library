
package platform.plugins.installables.network.routing;

import util.NetworkAddress;

/**
 * Messages used to store the reply for a finding route request.
 *
 * @author Dalmau
 */
public class ReplyForRouteMessage {
    
    /**
     * From which host is the route
     */
    protected String from;
    /**
     * To which host is the route
     */
    protected String to;
    /**
     * Via which host the route goes through
     */
    protected String via;
    /**
     * Is the route direct or by a relay
     */
    protected String direct;

    /**
     * Create a message
     * @param message the message
     */
    public ReplyForRouteMessage(String message) {
        int l1 = message.indexOf(' ');
        from = new NetworkAddress(message.substring(0, l1)).getNormalizedAddress();
        int l2 = message.indexOf(' ',l1+1);
        to = new NetworkAddress(message.substring(l1+1, l2)).getNormalizedAddress();
        int l3 = message.indexOf(' ',l2+1);
        via = new NetworkAddress(message.substring(l2+1,l3)).getNormalizedAddress();
        direct = message.substring(l3+1);
    }
    
    /**
     * Creates a message
     * @param from From which host is the route
     * @param to To which host is the route
     * @param via Via which host the route goes
     * @param direct Is the route direct or by a relay
     */
    public ReplyForRouteMessage(String from, String to, String via, String direct) {
        this (from+" "+to+" "+via+" "+direct);
    }

    /**
     * Convets the reply message into a string
     * @return reply message converted into the string
     */
    @Override
    public String toString() {
        return new String(from+" "+to+" "+via+" "+direct);
    }
    
    /**
     * Gets from which host is the route
     * @return From which host is the route
     */
    public String getFrom() { return from; }
    
    /**
     * Gets to which host is the route
     * @return To which host is the route
     */
    public String getTo() { return to; }
    
    /**
     * Gets via which host the route goes
     * @return Via which host the route goes
     */
    public String getVia() { return via; }

    /**
     * Gets if the route is direct or by a relay
     * @return the route is direct (true) or by a relay (false)
     */
    public boolean isDirect() {
        if (direct.equals("direct")) return true;
        else return false;
    }

}
