package org.fandyrev.game;

import org.apache.velocity.app.Velocity;
import org.fandyrev.game.http.Server;

/**
 * Hello world!
 *
 */
public class GameServer
{
    public static void main( String[] args ) throws Exception {
        System.out.println( "Start" );
        Server server = new Server(9999);
        Velocity.init();
        System.out.println( "Init ResourceMapper" );
        server.start();
    }
}
