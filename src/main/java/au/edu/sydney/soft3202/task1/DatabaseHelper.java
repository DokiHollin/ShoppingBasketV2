package au.edu.sydney.soft3202.task1;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

public class DatabaseHelper {
  private static final String dbName = "fruitbasket.db";
  private static final String dbURL = "jdbc:sqlite:" + dbName;
  private Connection connection;
  public static int setUp = 1;

  public static void createDB() {
    File dbFile = new File(dbName);
    if (dbFile.exists()) {
      System.out.println("Database already created");
      return;
    }
    try (Connection ignored = DriverManager.getConnection(dbURL)) {
      // If we get here that means no exception raised from getConnection - meaning it worked
      System.out.println("A new database has been created.");
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }

  public static void setupDB() {
    String users =
            """
            CREATE TABLE IF NOT EXISTS users (
                name text PRIMARY KEY
            );
            """;

    String shoppingcart =
            """
            CREATE TABLE IF NOT EXISTS shoppingcart (
                user text,
                item text,
                count integer NOT NULL,
                cost double NOT NULL,
                PRIMARY KEY (user, item),
                FOREIGN KEY (user)
                    REFERENCES users (name)
                        ON DELETE CASCADE
            );
            """;

    try (Connection conn = DriverManager.getConnection(dbURL);
         Statement statement = conn.createStatement()) {
      statement.execute(users);
      statement.execute(shoppingcart);
//            statement.execute(createStudentUnitTableSQL);

      System.out.println("Created tables");
    } catch (SQLException e) {
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }
  public void readyDB() {
    if(setUp == 0){
      return;
    }
    setUp = 0;
    File dbFile = new File(dbName);
    if (dbFile.exists()) {
      boolean result = dbFile.delete();
      if (!result) {
        System.out.println("Couldn't delete existing db file");
        System.exit(-1);
      } else {
        System.out.println("Removed existing DB file.");
      }
    } else {
      System.out.println("No existing DB file.");
    }
    createDB();
    setupDB();
    this.insertUser("Admin");
  }



  public String getUser(String name) throws SQLException {
    String sql = "SELECT name FROM users WHERE name = ?";
    try (Connection conn = DriverManager.getConnection(dbURL);
         PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      ResultSet resultSet = preparedStatement.executeQuery();

      while (resultSet.next()) {
        String user = resultSet.getString("name");
        return user;
      }
    }
    return null;
  }

  public static ArrayList<String> queryUsers(){
    String sql =
            """
            SELECT name
            FROM users
            WHERE name != 'Admin'
            """;
    ArrayList<String> temp = new ArrayList<>();
    try (Connection conn = DriverManager.getConnection(dbURL);
         PreparedStatement preparedStatement = conn.prepareStatement(sql);
         Statement statement = conn.createStatement()) {

      ResultSet results = preparedStatement.executeQuery();

      while (results.next()) {
        System.out.println(
                results.getString("name"));
        temp.add(results.getString("name"));
      }

    } catch (SQLException e) {
      System.out.println(e.getMessage());

    }
    return temp;
  }

  public void insertUser(String name){
    String sql = "INSERT INTO users (name) VALUES (?)";
    if (!isAlphanumeric(name)) {
      System.out.println("input name must be alphanumeric");
      return;
    }
    try (Connection conn = DriverManager.getConnection(dbURL);
         PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      preparedStatement.executeUpdate();
//      this.values.put("apple", 2.5);
//      this.values.put("orange", 1.25);
//      this.values.put("pear", 3.00);
//      this.values.put("banana", 4.95);
      this.addItem(name,"apple",0,2.5);
      this.addItem(name,"orange",0,1.25);
      this.addItem(name,"pear",0,3.00);
      this.addItem(name,"banana",0,4.95);
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public static boolean isAlphanumeric(String input) {
    String pattern = "^[a-zA-Z0-9]+$";
    return input.matches(pattern);
  }


    public void addItem(String userName, String item, int count, double cost){
    String sql = "INSERT INTO shoppingcart (user,item,count,cost) VALUES (?,?,?,?)";
    try (Connection conn = DriverManager.getConnection(dbURL);
         PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
      preparedStatement.setString(1, userName);
      preparedStatement.setString(2, item);
      preparedStatement.setDouble(3, count);
      preparedStatement.setDouble(4, cost);
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public ArrayList<String[]> queryBasket(String user){
    String sql =
            """
            SELECT user,item,count,cost
            FROM shoppingcart
            WHERE user = (?)
            """;
//    ArrayList<String> userLs = new ArrayList<>();
//    ArrayList<String> itemLs = new ArrayList<>();
//    ArrayList<String> countLs = new ArrayList<>();
//    ArrayList<String> costLs = new ArrayList<>();
    ArrayList<String[]> ls = new ArrayList<>();
//    String[] x = {"apple", "orange", "pear", "banana"};
//    ls.add(x);
    try (Connection conn = DriverManager.getConnection(dbURL);
         PreparedStatement preparedStatement = conn.prepareStatement(sql);
         Statement statement = conn.createStatement()) {
      preparedStatement.setString(1, user);
      ResultSet results = preparedStatement.executeQuery();

      while (results.next()) {
        System.out.println(
                results.getString("user"));
        String[] temp = {results.getString("user"), results.getString("item"), results.getString("cost"), results.getString("count")};
        ls.add(temp);
      }

    } catch (SQLException e) {
      System.out.println(e.getMessage());

    }
    return ls;
  }

  public void updateTable(String user, String fruit, String newName, int count, double cost){
    String sql =
                """
         
                UPDATE shoppingcart
                SET count = (?) , cost = (?) , item = (?)
                WHERE user = (?) AND item = (?)
                """;
    try (Connection conn = DriverManager.getConnection(dbURL);
         PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
      preparedStatement.setInt(1, count);
      preparedStatement.setDouble(2, cost);
      preparedStatement.setString(3, newName);
      preparedStatement.setString(4, user);
      preparedStatement.setString(5, fruit);
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public void removeItem(String user, String fruit){
    String sql =
            """
            DELETE FROM shoppingcart
            WHERE user = (?) AND item = (?)
            """;
    try (Connection conn = DriverManager.getConnection(dbURL);
         PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
      preparedStatement.setString(1, user);
      preparedStatement.setString(2, fruit);
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }

  public void deleteUser(String name) throws SQLException {
    String sql =
            """
            DELETE FROM users
            WHERE name = (?)
            """;
    try (Connection conn = DriverManager.getConnection(dbURL);
         PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
      preparedStatement.setString(1, name);
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }

    String sql2 =
            """
            DELETE FROM shoppingcart
            WHERE user = (?)
            """;
    try (Connection conn = DriverManager.getConnection(dbURL);
         PreparedStatement preparedStatement = conn.prepareStatement(sql2)) {
      preparedStatement.setString(1, name);
      preparedStatement.executeUpdate();
    } catch (SQLException e) {
      System.out.println(e.getMessage());
    }
  }


}

