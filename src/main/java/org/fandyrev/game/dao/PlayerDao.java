package org.fandyrev.game.dao;

import org.fandyrev.game.model.Player;

import java.sql.SQLException;

public interface PlayerDao {
    public Player getPlayer(String name) throws SQLException;
    public Player createPlayer(String name, String password) throws SQLException;
    public void updatePlayer(long id, int level, int rating) throws SQLException;
}
