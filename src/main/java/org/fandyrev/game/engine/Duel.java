package org.fandyrev.game.engine;

import org.fandyrev.game.model.Duelist;
import org.fandyrev.game.model.Player;

import java.util.Date;
import java.util.HashMap;

public class Duel {
    public static final int WATING = 0;
    public static final int FIGHTING = 1;
    public static final int FINISH = 2;

    private Duelist duelist1, duelist2;
    private int state;
    private Date startTime;
    private String winer;

    public Duel(Player player1, Player player2, String sessionId1, String sessionId2) {
        duelist1 = new Duelist(player1, sessionId1);
        duelist2 = new Duelist(player2, sessionId2);
        startTime = new Date();
        state = WATING;
        winer = null;

        duelist1.addMessage("Началась дуэль между Вами и " + player2.getName());
        duelist2.addMessage("Началась дуэль между Вами и " + player1.getName());
    }

    public HashMap<String, String> hit(String sessionId){
        HashMap<String, String> param = new HashMap<>();
        Duelist attacking, defensible;
        if(duelist1.getSessionId().equals(sessionId)){
            attacking = duelist1;
            defensible = duelist2;
        } else {
            attacking = duelist2;
            defensible = duelist1;
        }

        int hit = attacking.getDamage();
        defensible.hitDuelist(hit);
        attacking.addMessage("Вы ударили " + defensible.getName() + " на " + hit + " урона");
        defensible.addMessage(attacking.getName() + " ударил вас на " + hit + " урона");
        checkWinner();

        return getState(sessionId);
    }

    public int getState(){
        return state;
    }

    public HashMap<String, String> getState(String sessionId){
        HashMap<String, String> param = new HashMap<>();
        long timer = getTimer();
        int curState;
        synchronized (this) {
            if (state == WATING && timer <= 0)
                state = FIGHTING;

            if (state == FIGHTING && (duelist1.getHealth() <= 0 || duelist2.getHealth() <= 0))
                state = FINISH;

            curState = state;
        }

        Duelist player, enemy;
        if(duelist1.getSessionId().equals(sessionId)){
            player = duelist1;
            enemy = duelist2;
        } else {
            player = duelist2;
            enemy = duelist1;
        }

        param.put(Command.PARAM_HEALTH, String.valueOf(player.getHealthPercent()));
        param.put(Command.PARAM_DAMAGE, String.valueOf(player.getDamage()));
        param.put(Command.PARAM_NAME, player.getName());
        param.put(Command.PARAM_ENEMY_HEALTH, String.valueOf(enemy.getHealthPercent()));
        param.put(Command.PARAM_ENEMY_DAMAGE, String.valueOf(enemy.getDamage()));
        param.put(Command.PARAM_ENEMY_NAME, enemy.getName());
        param.put(Command.PARAM_MESSAGE, player.getMessages());

        if(curState == WATING){
            param.put(Command.PARAM_TIMER, String.valueOf(timer));
            param.put(Command.PARAM_TIMER_NUM, generateTimer(timer));
        } else if(curState == FIGHTING){
            param.put(Command.PARAM_COMBAT, "true");
        } else {
            if(winer != null && winer.equals(player.getSessionId())) {
                param.put(Command.PARAM_WINNER_NAME, player.getName());
            } else if(winer != null && winer.equals(enemy.getSessionId())) {
                param.put(Command.PARAM_WINNER_NAME, enemy.getName());
            }
        }

        return param;
    }

    public long getTimer() {
        long timer = 30 - ((new Date().getTime() - startTime.getTime())/1000);

        return timer;
    }

    private String generateTimer(long timer) {
        StringBuilder sb = new StringBuilder();
        while(timer > 9){
            sb.append(timer);
            sb.append(" ");
            timer--;
        }
        while(timer > 0){
            sb.append(0);
            sb.append(timer);
            sb.append(" ");
            timer--;
        }
        sb.append("00 00 00 00 00 00 00 00 00 00");

        return sb.toString();
    }

    public String getWiner(){
        return winer;
    }

    private boolean checkWinner(){
        synchronized (this) {
            if (winer != null)
                return true;

            if (duelist1.getHealth() <= 0) {
                winer = duelist2.getSessionId();
                duelist1.addMessage("Вы проиграли!");
                duelist2.addMessage("Вы победили!");
                state = FINISH;
                return true;
            }

            if (duelist2.getHealth() <= 0) {
                winer = duelist1.getSessionId();
                duelist1.addMessage("Вы победили!");
                duelist2.addMessage("Вы проиграли!");
                state = FINISH;
                return true;
            }
        }

        return false;
    }
}
