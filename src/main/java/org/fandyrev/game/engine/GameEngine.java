package org.fandyrev.game.engine;

import org.fandyrev.game.dao.PlayerDao;
import org.fandyrev.game.dao.PlayerDaoImpl;
import org.fandyrev.game.model.Player;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GameEngine {
    private static volatile GameEngine instance;
    private ConcurrentHashMap<String, Player> players;
    private ConcurrentHashMap<String, Duel> duels;
    private PlayerDao playerService;
    private String waitingDuel;

    private GameEngine() {
        players = new ConcurrentHashMap<>();
        playerService = new PlayerDaoImpl();
        duels = new ConcurrentHashMap<>();
    }

    public static GameEngine getInstance() {
        if (instance == null) {
            synchronized (GameEngine.class) {
                if (instance == null) {
                    instance = new GameEngine();
                }
            }
        }
        return instance;
    }

    public Command executeCommand(Command command){
        Command responseCommand = null;

        switch (command.getCommand()){
            case Command.LOGIN:
                responseCommand = login(command);
                break;
            case Command.AUTHORIZATION:
                responseCommand = authorization(command);
                break;
            case Command.MAIN:
                responseCommand = main(command);
                break;
            case Command.DUELS:
                responseCommand = duels(command);
                break;
            case Command.START_MATCHING:
                responseCommand = startMatching(command);
                break;
            case Command.STOP_MATCHING:
                responseCommand = stopMatching(command);
                break;
            case Command.COMBAT:
                responseCommand = combat(command);
                break;
            case Command.HIT:
                responseCommand = hit(command);
                break;
            case Command.LOGOUT:
                responseCommand = logout(command);
                break;
        }

        responseCommand.setStartTime(command.getStartTime());
        return responseCommand;
    }

    public Command hit(Command command){
        Command responseCommand = checkLogin(command.getSessionId());
        if(responseCommand != null) {
            responseCommand.setStartTime(command.getStartTime());
        }

        Duel duel = duels.get(command.getSessionId());
        if(duel == null){
            responseCommand = new Command(Command.REDIRECT);
            responseCommand.putParam(Command.PARAM_URL, "/duels");
            return responseCommand;
        }

        HashMap<String, String> param;
        if (duel.getState() == Duel.FINISH){
            param = duel.getState(command.getSessionId());
        } else {
            param = duel.hit(command.getSessionId());
        }

        responseCommand = new Command(Command.COMBAT, command.getSessionId(), param);
        if(duel.getState() == Duel.FINISH) {
            long dbReq = finishDuel(command.getSessionId());
            responseCommand.putParam(Command.PARAM_DB_REQ, "db: 1req (" + dbReq + "ms)");
        }

        return responseCommand;
    }

    public Command combat(Command command){
        Command responseCommand = checkLogin(command.getSessionId());
        if(responseCommand != null) {
            return responseCommand;
        }

        Duel duel = duels.get(command.getSessionId());
        if(duel == null){
            responseCommand = new Command(Command.DUELS);
        } else {
            HashMap<String, String> param = duel.getState(command.getSessionId());
            responseCommand = new Command(Command.COMBAT, command.getSessionId(), param);
            if(duel.getState() == Duel.FINISH) {
                long dbReq = finishDuel(command.getSessionId());
                responseCommand.putParam(Command.PARAM_DB_REQ, "db: 1req (" + dbReq + "ms)");
            }
        }

        return responseCommand;
    }

    public Command stopMatching(Command command){
        Command responseCommand = checkLogin(command.getSessionId());
        if(responseCommand != null)
            return responseCommand;

        synchronized (this) {
            if (waitingDuel != null && waitingDuel.equals(command.getSessionId())) {
                waitingDuel = null;
            }
        }

        responseCommand = new Command(Command.DUELS);
        Player player = players.get(command.getSessionId());
        responseCommand.putParam(Command.PARAM_RATING, String.valueOf(player.getRating()));

        return responseCommand;
    }

    public Command startMatching(Command command){
        Command responseCommand = checkLogin(command.getSessionId());
        if(responseCommand != null)
            return responseCommand;

        synchronized (this) {
            if (waitingDuel == null || waitingDuel.equals(command.getSessionId())) {
                waitingDuel = command.getSessionId();
                responseCommand = new Command(Command.WAITING_DUEL);
                responseCommand.putParam(Command.PARAM_TIMER, "5");
            } else {
                Duel duel = new Duel(players.get(command.getSessionId()), players.get(waitingDuel),
                        command.getSessionId(), waitingDuel);
                duels.putIfAbsent(waitingDuel, duel);
                duels.putIfAbsent(command.getSessionId(), duel);
                waitingDuel = null;
                responseCommand = new Command(Command.REDIRECT);
                responseCommand.putParam(Command.PARAM_URL, "/combat");
            }
        }

        return responseCommand;
    }

    public Command duels(Command command){
        Command responseCommand = checkLogin(command.getSessionId());
        if(responseCommand != null)
            return responseCommand;

        if (duels.get(command.getSessionId()) == null) {
            Player player = players.get(command.getSessionId());
            responseCommand = new Command(Command.DUELS);
            synchronized (this) {
                if (waitingDuel == null || !waitingDuel.equals(command.getSessionId())) {
                    responseCommand.putParam(Command.PARAM_RATING, String.valueOf(player.getRating()));
                } else {
                    responseCommand.putParam(Command.PARAM_TIMER, "5");
                }
            }
        } else {
            responseCommand = new Command(Command.REDIRECT);
            responseCommand.putParam(Command.PARAM_URL, "/combat");
        }

        return responseCommand;
    }

    public Command main(Command command){
        Command responseCommand = checkLogin(command.getSessionId());
        if(responseCommand != null)
            return responseCommand;

        responseCommand = new Command(Command.MAIN);

        return responseCommand;
    }

    public Command login(Command command){
        Command responseCommand = null;
        if(command.getSessionId() == null || !players.containsKey(command.getSessionId())){
            responseCommand = new Command(Command.LOGIN);
        } else {
            responseCommand = new Command(Command.REDIRECT);
            responseCommand.putParam(Command.PARAM_URL, "/main");
        }

        return responseCommand;
    }

    public Command authorization(Command command){
        Command responseCommand = null;
        String name = command.getParam(Command.PARAM_LOGIN);
        String password = command.getParam(Command.PARAM_PASSWORD);
        long dbReqTime, startTime, endTime;
        int dbReq = 1;
        if(name == null || password == null) {
            responseCommand = new Command(Command.AUTHORIZATION_ERROR);
            return responseCommand;
        }

        Player player = null;
        startTime = System.nanoTime();
        try {
            player = playerService.getPlayer(name);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        endTime = System.nanoTime();
        dbReqTime = endTime - startTime;

        if(player != null && (players.containsValue(player) || !password.equals(player.getPassword()))) {
            responseCommand = new Command(Command.AUTHORIZATION_ERROR);
            responseCommand.putParam(Command.PARAM_DB_REQ, "db: " + dbReq + "req (" + dbReqTime / 1000000 + "ms)");
            return responseCommand;
        }

        if(player == null) {
            startTime = System.nanoTime();
            try {
                player = playerService.createPlayer(name, password);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            endTime = System.nanoTime();
            dbReqTime += endTime - startTime;
            dbReq++;
        }

        String sessionId = UUID.randomUUID().toString();
        players.putIfAbsent(sessionId,player);

        responseCommand = new Command(Command.AUTHORIZATION, sessionId);
        responseCommand.putParam(Command.PARAM_DB_REQ, "db: " + dbReq + "req (" + dbReqTime / 1000000 + "ms)");
        return responseCommand;
    }

    public Command logout(Command command){
        Command responseCommand = null;
        if(command.getSessionId() != null && players.containsKey(command.getSessionId())){
            players.remove(command.getSessionId());
        }

        responseCommand = new Command(Command.REDIRECT);
        responseCommand.putParam(Command.PARAM_URL, "/main");

        return responseCommand;
    }

    private long finishDuel(String sessionId){
        Duel duel = duels.remove(sessionId);
        long startTime, endTime;

        Player player = players.get(sessionId);
        if(sessionId.equals(duel.getWiner())){
            player.setLevel(player.getLevel() + 1);
            player.setRating(player.getRating() + 1);

        } else {
            player.setLevel(player.getLevel() + 1);
            player.setRating(player.getRating() - 1);
        }

        startTime = System.nanoTime();
        try {
            playerService.updatePlayer(player.getId(), player.getLevel(), player.getRating());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        endTime = System.nanoTime();
        return (endTime - startTime) / 1000000;
    }

    private Command checkLogin(String sessionId){
        Command responseCommand = null;
        if(sessionId == null || !players.containsKey(sessionId)){
            responseCommand = new Command(Command.REDIRECT);
            responseCommand.putParam(Command.PARAM_URL, "/login");
        }

        return responseCommand;
    }
}
