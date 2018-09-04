package org.fandyrev.game.model;

public class Duelist {
    private String name;
    private String sessionId;
    private int health;
    private int maxHealth;
    private int damage;
    private StringBuilder log;

    public Duelist(Player player, String sessionId) {
        this.sessionId = sessionId;
        this.damage = 10 + player.getLevel();
        this.health = 100 + player.getLevel();
        this.maxHealth = health;
        this.name = player.getName();
        this.log = new StringBuilder();
    }

    public int hitDuelist(int damage){
        return health -= damage;
    }

    public int getDamage() {
        return damage;
    }

    public String getName() {
        return name;
    }

    public String getSessionId() {
        return sessionId;
    }

    public int getHealth() {
        return health;
    }

    public float getHealthPercent() {
        return ((float)health * 100)/maxHealth;
    }

    public void addMessage(String message){
        log.append(message);
        log.append("</br>");
    }

    public String getMessages(){
        return log.toString();
    }
}
