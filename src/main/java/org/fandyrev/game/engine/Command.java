package org.fandyrev.game.engine;

import java.util.HashMap;

public class Command {
    public static final int LOGIN = 0;
    public static final int AUTHORIZATION = 1;
    public static final int MAIN = 2;
    public static final int DUELS = 3;
    public static final int COMBAT = 4;
    public static final int START_MATCHING = 5;
    public static final int STOP_MATCHING = 11;
    public static final int HIT = 6;
    public static final int REDIRECT = 7;
    public static final int LOGOUT = 8;
    public static final int AUTHORIZATION_ERROR = 9;
    public static final int WAITING_DUEL = 10;

    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_LOGIN = "login";
    public static final String PARAM_RATING = "rating";
    public static final String PARAM_URL = "url";
    public static final String PARAM_TIMER = "timer";
    public static final String PARAM_TIMER_NUM = "timer_num";
    public static final String PARAM_MATCHING = "matching";
    public static final String PARAM_COMBAT = "combat";
    public static final String PARAM_MESSAGE = "message";
    public static final String PARAM_HEALTH = "health";
    public static final String PARAM_DAMAGE = "damage";
    public static final String PARAM_NAME = "name";
    public static final String PARAM_ENEMY_HEALTH = "enemy_health";
    public static final String PARAM_ENEMY_DAMAGE = "enemy_damage";
    public static final String PARAM_ENEMY_NAME = "enemy_name";
    public static final String PARAM_WINNER_NAME = "winner_name";
    public static final String PARAM_DB_REQ = "db_req";
    public static final String PARAM_PAGE_GEN = "page_gen";

    private int command;
    private String sessionId;
    private HashMap<String, String> param;
    private long startTime;

    public Command(int name) {
        this.command = name;
        this.param = new HashMap<String, String>();
        this.startTime = System.nanoTime();
    }

    public Command(int command, String sessionId) {
        this.command = command;
        this.sessionId = sessionId;
        this.param = new HashMap<String, String>();
        this.startTime = System.nanoTime();
    }

    public Command(int command, String sessionId, HashMap<String, String> param) {
        this.command = command;
        this.sessionId = sessionId;
        this.param = param;
        this.startTime = System.nanoTime();
    }

    public void putParam(String name, String value){
        param.put(name, value);
    }

    public int getCommand() {
        return command;
    }

    public String getParam(String key ) {
        return param.get(key);
    }

    public HashMap<String, String> getParam() {
        return param;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    @Override
    public String toString() {
        return "Command{" +
                "name=" + command +
                ", sessionId=" + sessionId +
                ", param=" + param +
                '}';
    }
}
