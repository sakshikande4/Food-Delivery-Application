
const API = "http://localhost:8080/api";

let currentRestaurant = null;

// Load restaurants
async function loadRestaurants() {
    try {
        const response = await fetch(`${API}/restaurants`);
        const restaurants = await response.json();

        const container = document.getElementById("restaurantList");
        container.innerHTML = "";

        restaurants.forEach(restaurant => {
            const card = document.createElement("div");
            card.className = "card";

            const heading = document.createElement("h3");
            heading.textContent = "🍽️ " + restaurant.name;

            const id = document.createElement("p");
            id.textContent = "Restaurant ID: " + restaurant.id;

            const button = document.createElement("button");
            button.textContent = "View Menu";
            button.addEventListener("click", () => {
                loadMenu(restaurant.id, restaurant.name);
            });

            card.append(heading, id, button);
            container.appendChild(card);
        });

    } catch (error) {
        document.getElementById("restaurantList").textContent =
            "Cannot connect to Java backend. Start the server first.";
    }
}

// Load selected restaurant menu
async function loadMenu(id, name) {
    currentRestaurant = id;

    document.getElementById("menuTitle").textContent =
        name + " - Food Menu";

    try {
        const response = await fetch(
            `${API}/menu?restaurantId=${encodeURIComponent(id)}`
        );

        const foods = await response.json();
        displayFoods(foods);

        document.getElementById("menu").scrollIntoView();
    } catch (error) {
        alert("Unable to load menu.");
    }
}

// Display food cards
function displayFoods(foods) {
    const container = document.getElementById("foodList");
    container.innerHTML = "";

    if (foods.length === 0) {
        container.textContent = "No food items found.";
        return;
    }

    foods.forEach(food => {
        const card = document.createElement("div");
        card.className = "card";

        const heading = document.createElement("h3");
        heading.textContent = "🍕 " + food.name;

        const price = document.createElement("p");
        price.textContent = "Price: ₹" + food.price;

        const button = document.createElement("button");
        button.textContent = "Add to Cart";
        button.addEventListener("click", () => addToCart(food.id));

        card.append(heading, price, button);
        container.appendChild(card);
    });
}

// Search food
async function searchFood() {
    const keyword = document.getElementById("searchInput").value.trim();

    if (!keyword) {
        alert("Please enter a food name.");
        return;
    }

    try {
        const response = await fetch(
            `${API}/search?q=${encodeURIComponent(keyword)}`
        );

        const foods = await response.json();

        document.getElementById("menuTitle").textContent =
            "Search Results";

        displayFoods(foods);
    } catch (error) {
        alert("Search failed.");
    }
}

// Sort food by price
async function sortFood() {
    if (!currentRestaurant) {
        alert("Please select a restaurant first.");
        return;
    }

    try {
        const response = await fetch(
            `${API}/sort?restaurantId=${encodeURIComponent(currentRestaurant)}`
        );

        const foods = await response.json();
        displayFoods(foods);
    } catch (error) {
        alert("Unable to sort food.");
    }
}

// Add food to cart
async function addToCart(id) {
    try {
        const response = await fetch(
            `${API}/cart/add?itemId=${encodeURIComponent(id)}&quantity=1`
        );

        const result = await response.json();

        if (result.error) {
            alert(result.error);
            return;
        }

        alert("Food added to cart!");
        loadCart();
    } catch (error) {
        alert("Unable to add item.");
    }
}

// Load cart
async function loadCart() {
    try {
        const response = await fetch(`${API}/cart`);
        const items = await response.json();

        const container = document.getElementById("cartItems");
        container.innerHTML = "";

        if (items.length === 0) {
            container.textContent =
                "Your cart is empty. Please add food items before placing an order.";
        }

        items.forEach(item => {
            const row = document.createElement("div");
            row.className = "cart-item";

            const details = document.createElement("span");
            details.textContent =
                `${item.name} | ₹${item.price} | Subtotal: ₹${item.subtotal}`;

            const controls = document.createElement("div");
            controls.className = "quantity";

            const minus = document.createElement("button");
            minus.textContent = "-";
            minus.addEventListener("click", () =>
                updateQuantity(item.id, item.quantity - 1)
            );

            const quantity = document.createElement("strong");
            quantity.textContent = " " + item.quantity + " ";

            const plus = document.createElement("button");
            plus.textContent = "+";
            plus.addEventListener("click", () =>
                updateQuantity(item.id, item.quantity + 1)
            );

            const remove = document.createElement("button");
            remove.textContent = "Remove";
            remove.className = "remove-btn";
            remove.addEventListener("click", () =>
                updateQuantity(item.id, 0)
            );

            controls.append(minus, quantity, plus, remove);
            row.append(details, controls);
            container.appendChild(row);
        });

        loadBill();

    } catch (error) {
        console.error("Cart loading failed:", error);
    }
}

// Update cart quantity
async function updateQuantity(id, quantity) {
    try {
        await fetch(
            `${API}/cart/update?itemId=${encodeURIComponent(id)}&quantity=${quantity}`
        );

        loadCart();
    } catch (error) {
        alert("Unable to update cart.");
    }
}

// Calculate bill
async function loadBill() {
    try {
        const response = await fetch(`${API}/cart/total`);
        const bill = await response.json();

        document.getElementById("subtotal").textContent =
            "₹" + bill.subtotal;

        document.getElementById("total").textContent =
            "₹" + (bill.subtotal === 0 ? 0 : bill.total);
    } catch (error) {
        console.error("Bill calculation failed:", error);
    }
}

// Place order
async function placeOrder() {
    const name = document.getElementById("customerName").value.trim();
    const phone = document.getElementById("phone").value.trim();
    const address = document.getElementById("address").value.trim();

    const confirmation = document.getElementById("orderConfirmation");

    if (!name || !phone || !address) {
        alert("Please fill in all delivery details.");
        return;
    }

    if (!/^[0-9]{10}$/.test(phone)) {
        alert("Please enter a valid 10-digit phone number.");
        return;
    }

    try {
        const params = new URLSearchParams({ name, phone, address });

        const response = await fetch(
            `${API}/orders?${params.toString()}`,
            { method: "POST" }
        );

        const result = await response.json();

        if (result.error) {
            confirmation.className = "error";
            confirmation.textContent = result.error;
            return;
        }

        confirmation.className = "success";
        confirmation.textContent =
            `🎉 Order Confirmed! Order ID: ${result.orderId}. ` +
            `Total Amount: ₹${result.total}. Status: ${result.status}`;

        document.getElementById("trackingId").value = result.orderId;

        document.getElementById("customerName").value = "";
        document.getElementById("phone").value = "";
        document.getElementById("address").value = "";

        loadCart();

    } catch (error) {
        alert("Unable to place order. Check whether Java backend is running.");
    }
}

// Track order
async function trackOrder() {
    const id = document.getElementById("trackingId").value.trim();
    const result = document.getElementById("trackingResult");

    if (!id) {
        alert("Please enter an Order ID.");
        return;
    }

    try {
        const response = await fetch(
            `${API}/order?id=${encodeURIComponent(id)}`
        );

        const order = await response.json();

        if (order.error) {
            result.textContent = order.error;
            return;
        }

        result.innerHTML = "";

        const heading = document.createElement("h3");
        heading.textContent = "Order: " + order.orderId;

        const customer = document.createElement("p");
        customer.textContent = "Customer: " + order.customer;

        const address = document.createElement("p");
        address.textContent = "Address: " + order.address;

        const total = document.createElement("p");
        total.textContent = "Total: ₹" + order.total;

        const status = document.createElement("p");
        status.textContent = "Current Status: " + order.status;

        result.append(heading, customer, address, total, status);

    } catch (error) {
        result.textContent = "Unable to track order.";
    }
}

// Button events
document.getElementById("searchBtn").addEventListener("click", searchFood);
document.getElementById("sortBtn").addEventListener("click", sortFood);
document.getElementById("placeOrderBtn").addEventListener("click", placeOrder);
document.getElementById("trackBtn").addEventListener("click", trackOrder);

// Initial loading
loadRestaurants();
loadCart();
