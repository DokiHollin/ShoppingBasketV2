package au.edu.sydney.soft3202.task1;

public class Admin {
    String name = "Admin";
    ShoppingBasket basket;

    public Admin(){
        this.basket = new ShoppingBasket();
    }

    public ShoppingBasket getBasket(){
        return basket;
    }

    public String getName(){
        return name;
    }
}
