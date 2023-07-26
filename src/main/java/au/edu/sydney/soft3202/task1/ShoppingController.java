package au.edu.sydney.soft3202.task1;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URI;
import java.security.SecureRandom;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

@Controller
public class ShoppingController {
    private static int setUp = 1;
//    private static final String dbName = "fruitbasket.db";
//    private static final String dbURL = "jdbc:sqlite:" + dbName;
    private final SecureRandom randomNumberGenerator = new SecureRandom();
    private final HexFormat hexFormatter = HexFormat.of();

    private final AtomicLong counter = new AtomicLong();
    ShoppingBasket shoppingBasket = new ShoppingBasket();

    Map<String, String> sessions = new HashMap<>();

//    String[] users = {"A", "B", "C", "D", "E"};
    ArrayList<String> users = new ArrayList<>();
    private Admin admin;
    DatabaseHelper dbHelper = null;
    Map<String,Customer> customers = new HashMap<>();

    public ShoppingController(){
        users.add("Admin");
    }


//    public static void addDataFromQuestionableSource(String firstName, String lastName, double wam) {
//        String addSingleStudentWithParametersSQL =
//                """
//                INSERT INTO students(first_name, last_name, wam) VALUES
//                    (?, ?, ?)
//                """;
//
//        try (Connection conn = DriverManager.getConnection(dbURL);
//             PreparedStatement preparedStatement = conn.prepareStatement(addSingleStudentWithParametersSQL)) {
//            preparedStatement.setString(1, firstName);
//            preparedStatement.setString(2, lastName);
//            preparedStatement.setDouble(3, wam);
//            preparedStatement.executeUpdate();
//
//            System.out.println("Added questionable data");
//        } catch (SQLException e) {
//            System.out.println(e.getMessage());
//            System.exit(-1);
//        }
//    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam(value = "user", defaultValue = "") String user) {

        // We are just checking the username, in the real world you would also check their password here
        // or authenticate the user some other way.
        if (!users.contains(user)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user.\n");
        }
        try {
            dbHelper = new DatabaseHelper();
            if (!user.equals("Admin")) {
                user = dbHelper.getUser(user);
            }
        } catch (SQLException sqle) {
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body("Unable to connect: " + sqle.getMessage()+ ".\n");
        }
//        dbHelper = new DatabaseHelper();

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user.\n");
        }


        // Generate the session token.
        byte[] sessionTokenBytes = new byte[16];
        randomNumberGenerator.nextBytes(sessionTokenBytes);
        String sessionToken = hexFormatter.formatHex(sessionTokenBytes);

        // Store the association of the session token with the user.
        sessions.put(sessionToken, user);

        // Create HTTP headers including the instruction for the browser to store the session token in a cookie.
        String setCookieHeaderValue = String.format("session=%s; Path=/; HttpOnly; SameSite=Strict;", sessionToken);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Set-Cookie", setCookieHeaderValue);
        if(user.equals("Admin")){
            admin = new Admin();
            dbHelper.readyDB();
            return ResponseEntity.status(HttpStatus.FOUND).headers(headers).location(URI.create("/admin")).build();
        }
        // Redirect to the cart page, with the session-cookie-setting headers.
        return ResponseEntity.status(HttpStatus.FOUND).headers(headers).location(URI.create("/cart")).build();

    }


    @GetMapping("/admin")
    public String admin(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                       Model model
    ) {
//        if (!sessions.containsKey(sessionToken)) {
//            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.\n");
//            return "InvalidPage";
//        }

        ArrayList<String> temp = dbHelper.queryUsers();
        model.addAttribute("admin", admin);
        model.addAttribute("token", sessionToken);
        model.addAttribute("users", temp);

        return "admin";
    }

    @PostMapping("/adminAddNew")
    public String adminAddNew(
            @CookieValue(value = "session", defaultValue = "") String sessionToken,
            @RequestParam(value = "newName",defaultValue = "") String name,
            Model model
    ) {

        dbHelper.insertUser(name);


        ArrayList<String> temp = dbHelper.queryUsers();
        model.addAttribute("admin", admin);
        model.addAttribute("token", sessionToken);
        model.addAttribute("users", temp);
        users.add(name);
        customers.put(name,new Customer(name));
//        dbHelper.updateTable("a","apple",2,20.0);
        return "admin";
    }
    @GetMapping("/adminAddNew")
    public String directAdd(){
        return "adminAddNew";
    }

    @PostMapping("/adminDelete")
    public String adminDelete(
            @CookieValue(value = "session", defaultValue = "") String sessionToken,
            @RequestParam(value = "names",defaultValue = "") String name,
            Model model
    ) throws SQLException {

//        dbHelper.insertUser(name);
//
//      s
        System.out.println("Deleting: "+name);
        String[] arrOfStr = name.split(",");
        for (String s : arrOfStr) {
            dbHelper.deleteUser(s);
            users.remove(s);
            customers.remove(s);
        }

        ArrayList<String> temp = dbHelper.queryUsers();
        model.addAttribute("admin", admin);
        model.addAttribute("token", sessionToken);
        model.addAttribute("users", temp);


        return "admin";
    }

    @GetMapping("/cart")
    public String cart(@CookieValue(value = "session", defaultValue = "") String sessionToken,
        Model model
    ) {
//        dbHelper.updateTable("a","apple",2,20.0);
        if (!sessions.containsKey(sessionToken)) {
            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.\n");
            return "InvalidPage";
        }
        Customer customer = customers.get(sessions.get(sessionToken));
        String customerName = customer.name;
        ArrayList<String[]> basket = dbHelper.queryBasket(customerName);
        customer.resetBasket(basket);
        model.addAttribute("customer", customer);
        model.addAttribute("basket", customer.getBasket());
        int[] array = new int[customer.getBasket().items.size()];
        model.addAttribute("emptyList", array);
        model.addAttribute("cost",customer.getBasket().values);
        model.addAttribute("token", sessionToken);
        return "cart";
    }

    @PostMapping("/updateCart")
    public String updateCart(
                             @RequestParam(value = "emptyList",defaultValue = "") String list,
                             @RequestParam(value = "customerName",defaultValue = "") String name,
                             @CookieValue(value = "session", defaultValue = "") String sessionToken,
                             Model model ) {

        if(Objects.equals(list, "") || Objects.equals(name, "")){
            return "redirect:/cart";
        }
        String actualName = name.split(",")[0];
        String[] arrOfStr = list.split(",", customers.get(actualName).getBasket().items.size());
        int i = 0;
        for(Map.Entry<String,Integer> each: customers.get(actualName).getBasket().items.entrySet()){
            try{
                each.setValue(0);
                if(Integer.parseInt(arrOfStr[i]) > 0){
                    customers.get(actualName).getBasket().addItem(each.getKey(), Integer.parseInt(arrOfStr[i]));
                    dbHelper.updateTable(actualName,each.getKey(),each.getKey(),Integer.parseInt(arrOfStr[i]),customers.get(actualName).getBasket().values.get(each.getKey()));

                }

//                each.setValue(Integer.valueOf(arrOfStr[i]));
//                Integer a = Integer.valueOf(arrOfStr[i]);
                i++;
            }catch(Exception e){
                System.out.println(e);
                System.out.println("currently processing" + each.getKey());
                System.out.println(customers.get(actualName).getBasket().items.toString());
                System.out.println(customers.get(actualName).getBasket().values.toString());
                System.out.println(Arrays.toString(customers.get(actualName).getBasket().names));
                return "InvalidInput";
            }
        }

       //return ResponseEntity.status(HttpStatus.FOUND).body(name);
       return "redirect:/cart";
    }

//    @PostMapping("/newname")
//    public ResponseEntity<String> directAccessnewName(@RequestParam(value = "user", defaultValue = "") String user) {
//        if (!Arrays.asList(users).contains(user)) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid user.\n");
//        }
//        return null;
//    }

    @PostMapping("/newname")
    public String newName(
            @CookieValue(value = "session", defaultValue = "") String sessionToken,
            @RequestParam(value = "name",defaultValue = "") String name,
            Model model
    ) {
        if (!sessions.containsKey(sessionToken)) {
            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.\n");
            return "InvalidPage";
        }
        model.addAttribute("customer", name);

        return "newname";
    }

    @PostMapping("/delname")
    public String del(@RequestParam(value = "name",defaultValue = "") String name,
                      @CookieValue(value = "session", defaultValue = "") String sessionToken,
                      Model model
    ) {
        if (!sessions.containsKey(sessionToken) || name.isEmpty()) {
            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.\n");
            return "InvalidPage";
        }

        model.addAttribute("customer", customers.get(name));
        return "delname";
    }

    @PostMapping("/deleteItems")
    public String deleteItems(
            @RequestParam(value = "items",defaultValue = "") String items,
            @RequestParam(value = "userName",defaultValue = "") String userName) {

        String actualName = userName.split(",")[0];
        try{
            if(Objects.equals(items, "") && customers.get(actualName).basket.items.size()==0){
                customers.get(actualName).basket.items = new HashMap<>();
                customers.get(actualName).basket.values = new HashMap<>();
                return "redirect:/cart";
            }
        }catch (Exception e){
            return "InvalidInput";
        }

        // The arrOfstr contains the item which shouldn't delete
        String[] arrOfStr = items.split(",", customers.get(actualName).getBasket().items.size());
        ArrayList<String> shoudDelete = new ArrayList<>();
        for(Map.Entry<String,Integer> each: customers.get(actualName).basket.items.entrySet()){
            int found = 0;
            for(String eachName:arrOfStr){
                if(eachName.equals(each.getKey())){
                    found = 1;
                    break;
                }
            }
            if(found == 0){
                shoudDelete.add(each.getKey());
            }
        }
        System.out.println(shoudDelete.size());
        for(String each: shoudDelete){
            customers.get(actualName).basket.addItem(each,1);
            customers.get(actualName).basket.removeItem(each,1);
            customers.get(actualName).basket.removeItem(each,1);
            customers.get(actualName).basket.items.remove(each);
            customers.get(actualName).basket.values.remove(each);
            System.out.println("deleting item is:"+each);
            dbHelper.removeItem(actualName,each);
        }
        customers.get(actualName).basket.names = arrOfStr;


        //return ResponseEntity.status(HttpStatus.FOUND).body(Arrays.toString(arrOfStr));

        return "redirect:/cart";

    }

    @PostMapping("/adding")
    public String adding(
            @RequestParam(value = "userName") String userName,
            @RequestParam(value = "newName") String name,
            @RequestParam(value = "cost") Object cost
    ){
        try{
            cost = Double.parseDouble(cost.toString());
        }catch(Exception e){
            return "InvalidInput";
        }
        name = name.toLowerCase();
        String actualName = userName.split(",")[0];
        ShoppingBasket basket = customers.get(actualName).basket;
        if(basket.items.containsKey(name)){
            return "redirect:/cart";
        }
        basket.addNewItem(name,(double) cost);
        basket.addNewItem(name,(double) cost);
        dbHelper.addItem(actualName,name,0, (double) cost);
//        basket.items.put(name,0);
//        basket.values.put(name, (double) cost);
//        basket.names = new String [basket.items.size()];
//        int i = 0;
//        for(String each: basket.items.keySet()){
//            basket.names[i] = each;
//            i++;
//        }

        return "redirect:/cart";
    }

    @PostMapping("/updatename")
    public String updateName(
            @CookieValue(value = "session", defaultValue = "") String sessionToken,
            @RequestParam(value = "name",defaultValue = "") String name,
            Model model
    ) {
        if (!sessions.containsKey(sessionToken) || name.isEmpty()) {
            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.\n");
            return "InvalidPage";
        }
        model.addAttribute("customer", customers.get(name));

        return "updatename";
    }

    @PostMapping("/updateProcess")
    public String adding(
            @RequestParam(value = "itemName",defaultValue = "") String itemName,
            @RequestParam(value = "itemCost",defaultValue = "") String itemCost,
            @RequestParam(value = "userName",defaultValue = "") String userName,
            @RequestParam(value = "originalName",defaultValue = "") String originalName
    ){

        String actualName = userName.split(",")[0];
        if(Objects.equals(itemName, "") || Objects.equals(itemCost, "")){
            customers.get(actualName).basket.items = new HashMap<>();
            customers.get(actualName).basket.values = new HashMap<>();
            return "redirect:/cart";
        }
        ShoppingBasket basket = customers.get(actualName).basket;
        String[] nameArray = itemName.split(",", customers.get(actualName).getBasket().items.size());
        String[] costArray = itemCost.split(",", customers.get(actualName).getBasket().items.size());
        ArrayList<Integer> amount = new ArrayList<>();


        for(Map.Entry<String,Integer> each: basket.items.entrySet()){
            amount.add(each.getValue());
        }


        basket.clear();
        basket.items.clear();
        basket.values.clear();
        basket.getValue();
        int i = 0;
        for(String each: nameArray){
            basket.items.put(each,amount.get(i));
            i++;
        }
        int j = 0;
        for(String each: costArray){
            try{
                basket.values.put(nameArray[j], Double.valueOf(each));
                j++;
            }catch(Exception e){
                return "InvalidInput";
            }

        }


        System.out.println(basket.items.toString());
//        basket.addItem(nameArray[0],1);
        System.out.println(Arrays.toString(basket.names));
        System.out.println(basket.values.toString());
        basket.names = nameArray;
        basket.getValue();

//        System.out.println(itemName);
//        System.out.println(itemCost);
//        System.out.println(originalName);

        String[] originalNameArray = originalName.split(",", customers.get(actualName).getBasket().items.size());

        for(int k = 0; k < originalNameArray.length; k++){
            dbHelper.updateTable(actualName,originalNameArray[k],nameArray[k],basket.items.get(nameArray[k]), Double.parseDouble(costArray[k]));
        }


        return "redirect:/cart";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "redirect:/";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session,
                         @RequestParam(value = "token",defaultValue = "") String token) {
        session.invalidate();
        sessions.remove(token);
        return "redirect:/login";
    }


    @GetMapping("/newname")
    public String redirectNewName(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                                  @RequestParam(value = "name",defaultValue = "") String name,
                                  Model model){
        if (sessions.size() == 0 || name.isEmpty()) {
            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.\n");
            return "InvalidPage";
        }else{
            for(String each: sessions.keySet()){
                name = each;
            }
            name = sessions.get(name);
            model.addAttribute("customer", customers.get(name));

            return "newname";
        }
    }

//    @GetMapping("/cart")
//    public String redirectCart(@CookieValue(value = "session", defaultValue = "") String sessionToken,
//                                  @RequestParam(value = "name",defaultValue = "") String name,
//                                  Model model){
//        if (!sessions.containsKey(sessionToken) && name.isEmpty()) {
//            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.\n");
//            return "InvalidPage";
//        }else{
//            model.addAttribute("customer", name);
//
//            return "cart";
//        }
//    }

    @GetMapping("/delname")
    public String redirectDelName(@CookieValue(value = "session", defaultValue = "") String sessionToken,
                                  @RequestParam(value = "name",defaultValue = "") String name,
                                  Model model){
        if (sessions.size() == 0 || name.isEmpty()) {
            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.\n");
            return "InvalidPage";
        }else{
            for(String each: sessions.keySet()){
                name = each;
            }
            name = sessions.get(name);
            model.addAttribute("customer", customers.get(name));

            return "delname";
        }
    }

    @GetMapping("/updatename")
    public String redirectUpdateName(

                                  Model model){
        if (sessions.size() == 0) {
            //return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorised.\n");
            return "InvalidPage";
        }else{
            String name = "";
            for(String each: sessions.keySet()){
                name = each;
            }
            name = sessions.get(name);
            System.out.println(name);
            model.addAttribute("customer", customers.get(name));

            return "updatename";
        }
    }


//    @GetMapping("/counter")
//    public ResponseEntity<String> counter() {
//        counter.incrementAndGet();
//        return ResponseEntity.status(HttpStatus.OK).body("[" + counter + "]");
//    }
//
//    @GetMapping("/cost")
//    public ResponseEntity<String> cost() {
//        return ResponseEntity.status(HttpStatus.OK).body(
//            shoppingBasket.getValue() == null ? "0" : shoppingBasket.getValue().toString()
//        );
//    }
//
//    @GetMapping("/greeting")
//    public String greeting(
//        @RequestParam(name="name", required=false, defaultValue="World") String name,
//        Model model
//    ) {
//        model.addAttribute("name", name);
//        return "greeting";
//    }

}
