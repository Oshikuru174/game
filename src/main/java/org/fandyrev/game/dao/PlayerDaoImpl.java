package org.fandyrev.game.dao;

import org.fandyrev.game.model.Player;

import java.sql.*;

public class PlayerDaoImpl implements PlayerDao {
    @Override
    public Player getPlayer(String name) throws SQLException {
        Player player = null;
        String SQL_QUERY = "SELECT * FROM player WHERE name = '" +
                name +
                "'";

        Connection connection = DataSource.getConnection();

        if(connection != null){
            try {
                PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
                ResultSet rs = pst.executeQuery();
                if(rs.next()){
                    player = new Player();
                    player.setId(rs.getLong("id"));
                    player.setName(rs.getString("name"));
                    player.setPassword(rs.getString("password"));
                    player.setLevel(rs.getInt("level"));
                    player.setRating(rs.getInt("rating"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connection.close();
            }
        }

        return player;
    }

    @Override
    public Player createPlayer(String name, String password) throws SQLException {
        Player player = null;
        String SQL_QUERY = "INSERT INTO player(name, password, level, rating) VALUES ('" +
                name +
                "', '" +
                password +
                "', 0, 0)";

        Connection connection = DataSource.getConnection();

        if(connection != null){
            try {
                PreparedStatement pst = connection.prepareStatement(SQL_QUERY, Statement.RETURN_GENERATED_KEYS);
                pst.executeUpdate();
                ResultSet rs = pst.getGeneratedKeys();
                System.out.println(rs.getMetaData().getColumnCount());
                if(rs.next()){
                    player = new Player();
                    player.setId(rs.getLong(1));
                    player.setName(name);
                    player.setPassword(password);
                    player.setLevel(0);
                    player.setRating(0);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connection.close();
            }
        }

        return player;
    }

    @Override
    public void updatePlayer(long id, int level, int rating) throws SQLException {
        String SQL_QUERY = "UPDATE player SET level='" + level + "', rating='" + rating + "' WHERE id='" + id + "'";

        Connection connection = DataSource.getConnection();

        if(connection != null){
            try {
                PreparedStatement pst = connection.prepareStatement(SQL_QUERY);
                pst.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            } finally {
                connection.close();
            }
        }
    }
}
