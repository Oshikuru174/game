package org.fandyrev.game.http;

import java.util.HashSet;

public class ResourceMapper {
    private static HashSet<String> gameResourse;
    static {
        gameResourse = new HashSet<>();
        gameResourse.add("/authorisationError");
        gameResourse.add("/login");
        gameResourse.add("/logout");
        gameResourse.add("/combat");
        gameResourse.add("/duels");
        gameResourse.add("/main");
    }

    public static boolean isGameResourse(String uri){
        System.out.println(uri);
        return gameResourse.contains(uri);
    }
}
