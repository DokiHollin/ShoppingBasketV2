package au.edu.sydney.soft3202.task1;

import java.util.ArrayList;
import java.util.HashMap;

public class Customer {
    String name;
    ShoppingBasket basket;

    public Customer(String name){
        this.name = name;
        this.basket = new ShoppingBasket();
    }

    public void resetBasket(ArrayList<String[]> basket){
        HashMap<String, Integer> newItems = new HashMap<>();
        HashMap<String, Double> newValues = new HashMap<>();
        String[] newNames = new String[basket.size()];
        int i = 0;
        for(String[] each: basket){
            newItems.put(each[1], Integer.valueOf(each[3]));
            newValues.put(each[1], Double.valueOf(each[2]));
            newNames[i] = each[1];
            i++;
        }
        this.basket.items = newItems;
        this.basket.values = newValues;
        this.basket.names = newNames;
    }
    public ShoppingBasket getBasket(){
        return basket;
    }

    public String getName(){
        return name;
    }

}
