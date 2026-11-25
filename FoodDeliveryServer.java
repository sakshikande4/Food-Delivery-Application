
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;

public class FoodDeliveryServer {

    static class FoodItem {
        String id, name, category;
        double price;
        boolean available;

        FoodItem(String id, String name, String category, double price) {
            this.id = id;
            this.name = name;
            this.category = category;
            this.price = price;
            this.available = true;
        }
    }

    static class Restaurant {
        String id, name;
        ArrayList<FoodItem> menu = new ArrayList<>();

        Restaurant(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    static class CartItem {
        FoodItem food;
        int quantity;

        CartItem(FoodItem food, int quantity) {
            this.food = food;
            this.quantity = quantity;
        }

        double subtotal() {
            return food.price * quantity;
        }
    }

    static class Order {
        String id, customer, phone, address, status;
        ArrayList<CartItem> items;
        double total;

        Order(String id, String customer, String phone,
              String address, ArrayList<CartItem> items,
              double total) {
            this.id = id;
            this.customer = customer;
            this.phone = phone;
            this.address = address;
            this.items = items;
            this.total = total;
            this.status = "Order Placed";
        }
    }

    static HashMap<String, Restaurant> restaurants = new HashMap<>();
    static HashMap<String, Order> orders = new HashMap<>();
    static ArrayList<CartItem> cart = new ArrayList<>();

    static final double DELIVERY_CHARGE = 30;

    static void loadData() {

        Restaurant r1 = new Restaurant("R101", "Spice Garden");
        Restaurant r2 = new Restaurant("R102", "Pizza Point");
        Restaurant r3 = new Restaurant("R103", "Green Bowl");

        r1.menu.add(new FoodItem("F101", "Paneer Tikka",
                "Indian", 220));
        r1.menu.add(new FoodItem("F102", "Veg Biryani",
                "Indian", 180));
        r1.menu.add(new FoodItem("F103", "Butter Naan",
                "Indian", 40));

        r2.menu.add(new FoodItem("F201", "Margherita Pizza",
                "Pizza", 199));
        r2.menu.add(new FoodItem("F202", "Farmhouse Pizza",
                "Pizza", 279));
        r2.menu.add(new FoodItem("F203", "Garlic Bread",
                "Sides", 99));

        r3.menu.add(new FoodItem("F301", "Healthy Salad",
                "Healthy", 150));
        r3.menu.add(new FoodItem("F302", "Veg Bowl",
                "Healthy", 180));
        r3.menu.add(new FoodItem("F303", "Fruit Bowl",
                "Healthy", 120));

        restaurants.put(r1.id, r1);
        restaurants.put(r2.id, r2);
        restaurants.put(r3.id, r3);
    }

    static FoodItem findFood(String id) {
        for (Restaurant r : restaurants.values()) {
            for (FoodItem f : r.menu) {
                if (f.id.equals(id)) return f;
            }
        }
        return null;
    }

    static ArrayList<FoodItem> searchFood(String keyword) {
        ArrayList<FoodItem> result = new ArrayList<>();

        for (Restaurant r : restaurants.values()) {
            for (FoodItem f : r.menu) {
                if (f.name.toLowerCase()
                        .contains(keyword.toLowerCase())) {
                    result.add(f);
                }
            }
        }
        return result;
    }

    static void addToCart(String id, int quantity) {
        FoodItem food = findFood(id);

        if (food == null || !food.available || quantity <= 0)
            return;

        for (CartItem item : cart) {
            if (item.food.id.equals(id)) {
                item.quantity += quantity;
                return;
            }
        }

        cart.add(new CartItem(food, quantity));
    }

    static void updateCart(String id, int quantity) {
        Iterator<CartItem> iterator = cart.iterator();

        while (iterator.hasNext()) {
            CartItem item = iterator.next();

            if (item.food.id.equals(id)) {
                if (quantity <= 0) {
                    iterator.remove();
                } else {
                    item.quantity = quantity;
                }
                return;
            }
        }
    }

    static double calculateTotal() {
        double total = 0;

        for (CartItem item : cart) {
            total += item.subtotal();
        }

        return total;
    }

    static String cartJSON() {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < cart.size(); i++) {
            CartItem item = cart.get(i);
            if (i > 0) json.append(",");

            json.append("{\"id\":\"").append(item.food.id)
                .append("\",\"name\":\"").append(item.food.name)
                .append("\",\"price\":").append(item.food.price)
                .append(",\"quantity\":").append(item.quantity)
                .append(",\"subtotal\":").append(item.subtotal())
                .append("}");
        }
        return json.append("]").toString();
    }

    static String getParam(String query, String key) {
        if (query == null) return "";

        for (String part : query.split("&")) {
            String[] pair = part.split("=", 2);
            if (pair.length == 2 && pair[0].equals(key)) {
                return URLDecoder.decode(
                    pair[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    static void send(HttpExchange exchange, String response)
            throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    static String escape(String value) {
        return value.replace("\\", "\\\\")
                    .replace("\"", "\\\"");
    }

    static void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String query = exchange.getRequestURI().getRawQuery();
        String response = "{}";

        try {
            if (path.equals("/api/restaurants")) {
                StringBuilder json = new StringBuilder("[");
                int i = 0;

                for (Restaurant r : restaurants.values()) {
                    if (i++ > 0) json.append(",");
                    json.append("{\"id\":\"").append(r.id)
                        .append("\",\"name\":\"")
                        .append(escape(r.name)).append("\"}");
                }
                response = json.append("]").toString();
            }

            else if (path.equals("/api/menu")) {
                String id = getParam(query, "restaurantId");
                Restaurant r = restaurants.get(id);

                if (r != null) {
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < r.menu.size(); i++) {
                        FoodItem f = r.menu.get(i);
                        if (i > 0) json.append(",");
                        json.append("{\"id\":\"").append(f.id)
                            .append("\",\"name\":\"").append(f.name)
                            .append("\",\"price\":").append(f.price)
                            .append(",\"category\":\"").append(f.category)
                            .append("\"}");
                    }
                    response = json.append("]").toString();
                }
            }

            else if (path.equals("/api/search")) {
                String keyword = getParam(query, "q");
                ArrayList<FoodItem> result = searchFood(keyword);
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < result.size(); i++) {
                    FoodItem f = result.get(i);
                    if (i > 0) json.append(",");
                    json.append("{\"id\":\"").append(f.id)
                        .append("\",\"name\":\"").append(f.name)
                        .append("\",\"price\":").append(f.price).append("}");
                }
                response = json.append("]").toString();
            }

            else if (path.equals("/api/sort")) {
                String id = getParam(query, "restaurantId");
                Restaurant r = restaurants.get(id);

                if (r != null) {
                    r.menu.sort(Comparator.comparingDouble(f -> f.price));
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < r.menu.size(); i++) {
                        FoodItem f = r.menu.get(i);
                        if (i > 0) json.append(",");
                        json.append("{\"id\":\"").append(f.id)
                            .append("\",\"name\":\"").append(f.name)
                            .append("\",\"price\":").append(f.price).append("}");
                    }
                    response = json.append("]").toString();
                }
            }

            else if (path.equals("/api/cart/add")) {
                addToCart(getParam(query, "itemId"),
                    Integer.parseInt(getParam(query, "quantity")));
                response = cartJSON();
            }

            else if (path.equals("/api/cart/update")) {
                updateCart(getParam(query, "itemId"),
                    Integer.parseInt(getParam(query, "quantity")));
                response = cartJSON();
            }

            else if (path.equals("/api/cart")) {
                response = cartJSON();
            }

            else if (path.equals("/api/cart/total")) {
                double subtotal = calculateTotal();
                response = "{\"subtotal\":" + subtotal
                    + ",\"deliveryCharge\":" + DELIVERY_CHARGE
                    + ",\"total\":" + (subtotal + DELIVERY_CHARGE) + "}";
            }

            else if (path.equals("/api/orders")
                    && exchange.getRequestMethod().equals("POST")) {

                if (cart.isEmpty()) {
                    response = "{\"error\":\"Your cart is empty. Please add food items before placing an order.\"}";
                } else {
                    String name = getParam(query, "name");
                    String phone = getParam(query, "phone");
                    String address = getParam(query, "address");

                    if (name.isBlank() || phone.isBlank() || address.isBlank()) {
                        response = "{\"error\":\"Please enter all delivery details.\"}";
                    } else {
                        String orderId = "ORD" + (1001 + orders.size());
                        ArrayList<CartItem> orderedItems = new ArrayList<>();

                        for (CartItem item : cart) {
                            orderedItems.add(new CartItem(
                                item.food, item.quantity));
                        }

                        double total = calculateTotal() + DELIVERY_CHARGE;

                        Order order = new Order(orderId, name,
                            phone, address, orderedItems, total);

                        orders.put(orderId, order);
                        cart.clear();

                        response = "{\"orderId\":\"" + orderId
                            + "\",\"total\":" + total
                            + ",\"status\":\"Order Placed\"}";
                    }
                }
            }

            else if (path.equals("/api/order")) {
                String id = getParam(query, "id");
                Order order = orders.get(id);

                if (order != null) {
                    response = "{\"orderId\":\"" + order.id
                        + "\",\"customer\":\"" + escape(order.customer)
                        + "\",\"address\":\"" + escape(order.address)
                        + "\",\"total\":" + order.total
                        + ",\"status\":\"" + order.status + "\"}";
                } else {
                    response = "{\"error\":\"Order not found\"}";
                }
            }

            else if (path.equals("/api/order/status")) {
                String id = getParam(query, "id");
                String status = getParam(query, "status");
                Order order = orders.get(id);

                List<String> statuses = Arrays.asList(
                    "Order Placed", "Preparing",
                    "Out for Delivery", "Delivered");

                if (order != null && statuses.contains(status)) {
                    order.status = status;
                    response = "{\"success\":true,\"status\":\""
                        + status + "\"}";
                } else {
                    response = "{\"error\":\"Invalid order or status\"}";
                }
            }

        } catch (Exception e) {
            response = "{\"error\":\"" + escape(e.getMessage()) + "\"}";
        }

        send(exchange, response);
    }

    public static void main(String[] args) throws Exception {
        loadData();

        HttpServer server = HttpServer.create(
            new InetSocketAddress(8080), 0);

        server.createContext("/api", FoodDeliveryServer::handle);
        server.start();

        System.out.println("Food Delivery Server is running!");
        System.out.println("Open http://localhost:8080/api/restaurants");
    }
}