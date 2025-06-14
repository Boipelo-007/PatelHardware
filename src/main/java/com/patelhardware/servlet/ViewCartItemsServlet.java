package com.patelhardware.servlet;

import com.patelhardware.util.DatabaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@WebServlet("/viewcart")
public class ViewCartItemsServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(ViewCartItemsServlet.class.getName());
    private ObjectMapper objectMapper = new ObjectMapper();

    // Inner class to represent cart item with details
    public static class CartItem {
        private int cartId;
        private int itemId;
        private String itemNumber;
        private String name;
        private String color;
        private String description;
        private BigDecimal price;
        private int quantity;
        private BigDecimal subtotal;
        private boolean available;

        // Constructors
        public CartItem() {}

        // Getters and setters
        public int getCartId() { return cartId; }
        public void setCartId(int cartId) { this.cartId = cartId; }

        public int getItemId() { return itemId; }
        public void setItemId(int itemId) { this.itemId = itemId; }

        public String getItemNumber() { return itemNumber; }
        public void setItemNumber(String itemNumber) { this.itemNumber = itemNumber; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }

        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }

        public BigDecimal getSubtotal() { return subtotal; }
        public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }

        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Check if user is logged in
        if (session == null || session.getAttribute("user") == null) {
            if ("json".equals(request.getParameter("format"))) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "User not logged in");

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            } else {
                response.sendRedirect("login.html");
            }
            return;
        }

        Integer userId = (Integer) session.getAttribute("userId");
        handleViewCart(request, response, userId);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Check if user is logged in
        if (session == null || session.getAttribute("user") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "User not logged in");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        Integer userId = (Integer) session.getAttribute("userId");
        String action = request.getParameter("action");

        switch (action != null ? action : "") {
            case "updateQuantity":
                handleUpdateQuantity(request, response, userId);
                break;
            case "removeItem":
                handleRemoveItem(request, response, userId);
                break;
            case "clearCart":
                handleClearCart(request, response, userId);
                break;
            case "checkout":
                handleCheckout(request, response, userId);
                break;
            default:
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Invalid action");

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private void handleViewCart(HttpServletRequest request, HttpServletResponse response, Integer userId)
            throws IOException {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            String sql = "SELECT c.cart_id, c.item_id, c.quantity, " +
                    "i.item_number, i.name, i.color, i.description, i.price, i.available " +
                    "FROM cart c " +
                    "JOIN items i ON c.item_id = i.item_id " +
                    "WHERE c.user_id = ? " +
                    "ORDER BY c.added_at DESC";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            List<CartItem> cartItems = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            int totalItems = 0;

            while (rs.next()) {
                CartItem cartItem = new CartItem();
                cartItem.setCartId(rs.getInt("cart_id"));
                cartItem.setItemId(rs.getInt("item_id"));
                cartItem.setItemNumber(rs.getString("item_number"));
                cartItem.setName(rs.getString("name"));
                cartItem.setColor(rs.getString("color"));
                cartItem.setDescription(rs.getString("description"));
                cartItem.setPrice(rs.getBigDecimal("price"));
                cartItem.setQuantity(rs.getInt("quantity"));
                cartItem.setAvailable(rs.getBoolean("available"));

                // Calculate subtotal
                BigDecimal subtotal = cartItem.getPrice().multiply(new BigDecimal(cartItem.getQuantity()));
                cartItem.setSubtotal(subtotal);

                cartItems.add(cartItem);
                totalAmount = totalAmount.add(subtotal);
                totalItems += cartItem.getQuantity();
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("cartItems", cartItems);
            responseData.put("totalAmount", totalAmount);
            responseData.put("totalItems", totalItems);
            responseData.put("itemCount", cartItems.size());
            responseData.put("message", "Cart retrieved successfully");

            response.getWriter().write(objectMapper.writeValueAsString(responseData));

            logger.info("Cart viewed for user: " + userId + ", Items: " + cartItems.size());

        } catch (SQLException e) {
            logger.severe("Database error while retrieving cart: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving cart");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } finally {
            DatabaseUtil.closeResultSet(rs);
            DatabaseUtil.closeStatement(stmt);
            DatabaseUtil.closeConnection(conn);
        }
    }

    private void handleUpdateQuantity(HttpServletRequest request, HttpServletResponse response, Integer userId)
            throws IOException {

        String cartIdStr = request.getParameter("cartId");
        String quantityStr = request.getParameter("quantity");

        if (cartIdStr == null || quantityStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Cart ID and quantity are required");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        try {
            int cartId = Integer.parseInt(cartIdStr);
            int quantity = Integer.parseInt(quantityStr);

            if (quantity <= 0) {
                // Remove item if quantity is 0 or negative
                handleRemoveItemById(response, userId, cartId);
                return;
            }

            Connection conn = null;
            PreparedStatement stmt = null;

            try {
                conn = DatabaseUtil.getConnection();

                // Verify the cart item belongs to the user and get item details
                String verifySql = "SELECT c.item_id, i.stock_quantity FROM cart c " +
                        "JOIN items i ON c.item_id = i.item_id " +
                        "WHERE c.cart_id = ? AND c.user_id = ?";
                PreparedStatement verifyStmt = conn.prepareStatement(verifySql);
                verifyStmt.setInt(1, cartId);
                verifyStmt.setInt(2, userId);
                ResultSet rs = verifyStmt.executeQuery();

                if (!rs.next()) {
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    response.setContentType("application/json");

                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Cart item not found");

                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    return;
                }

                int stockQuantity = rs.getInt("stock_quantity");

                if (quantity > stockQuantity) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");

                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Quantity exceeds available stock: " + stockQuantity);

                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    return;
                }

                // Update quantity
                String updateSql = "UPDATE cart SET quantity = ? WHERE cart_id = ? AND user_id = ?";
                stmt = conn.prepareStatement(updateSql);
                stmt.setInt(1, quantity);
                stmt.setInt(2, cartId);
                stmt.setInt(3, userId);

                int updated = stmt.executeUpdate();

                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                if (updated > 0) {
                    Map<String, Object> successResponse = new HashMap<>();
                    successResponse.put("success", true);
                    successResponse.put("message", "Quantity updated successfully");
                    successResponse.put("cartId", cartId);
                    successResponse.put("newQuantity", quantity);

                    response.getWriter().write(objectMapper.writeValueAsString(successResponse));

                    logger.info("Cart quantity updated - User: " + userId + ", Cart ID: " + cartId + ", Quantity: " + quantity);
                } else {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Failed to update quantity");

                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                }

            } finally {
                DatabaseUtil.closeStatement(stmt);
                DatabaseUtil.closeConnection(conn);
            }

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid cart ID or quantity format");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } catch (SQLException e) {
            logger.severe("Database error while updating cart quantity: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error updating quantity");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private void handleRemoveItem(HttpServletRequest request, HttpServletResponse response, Integer userId)
            throws IOException {

        String cartIdStr = request.getParameter("cartId");

        if (cartIdStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Cart ID is required");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        try {
            int cartId = Integer.parseInt(cartIdStr);
            handleRemoveItemById(response, userId, cartId);

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid cart ID format");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private void handleRemoveItemById(HttpServletResponse response, Integer userId, int cartId)
            throws IOException {

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseUtil.getConnection();
            String sql = "DELETE FROM cart WHERE cart_id = ? AND user_id = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, cartId);
            stmt.setInt(2, userId);

            int deleted = stmt.executeUpdate();

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            if (deleted > 0) {
                Map<String, Object> successResponse = new HashMap<>();
                successResponse.put("success", true);
                successResponse.put("message", "Item removed from cart successfully");
                successResponse.put("cartId", cartId);

                response.getWriter().write(objectMapper.writeValueAsString(successResponse));

                logger.info("Item removed from cart - User: " + userId + ", Cart ID: " + cartId);
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Cart item not found");

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            }

        } catch (SQLException e) {
            logger.severe("Database error while removing cart item: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error removing item from cart");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } finally {
            DatabaseUtil.closeStatement(stmt);
            DatabaseUtil.closeConnection(conn);
        }
    }

    private void handleClearCart(HttpServletRequest request, HttpServletResponse response, Integer userId)
            throws IOException {

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = DatabaseUtil.getConnection();
            String sql = "DELETE FROM cart WHERE user_id = ?";

            stmt = conn.prepareStatement(sql);
            stmt.setInt(1, userId);

            int deleted = stmt.executeUpdate();

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Cart cleared successfully");
            successResponse.put("itemsDeleted", deleted);

            response.getWriter().write(objectMapper.writeValueAsString(successResponse));

            logger.info("Cart cleared for user: " + userId + ", Items deleted: " + deleted);

        } catch (SQLException e) {
            logger.severe("Database error while clearing cart: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error clearing cart");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } finally {
            DatabaseUtil.closeStatement(stmt);
            DatabaseUtil.closeConnection(conn);
        }
    }

    private void handleCheckout(HttpServletRequest request, HttpServletResponse response, Integer userId)
            throws IOException {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Get cart items and calculate total
            String cartSql = "SELECT c.item_id, c.quantity, i.price, i.stock_quantity, i.available " +
                    "FROM cart c JOIN items i ON c.item_id = i.item_id " +
                    "WHERE c.user_id = ?";

            stmt = conn.prepareStatement(cartSql);
            stmt.setInt(1, userId);
            rs = stmt.executeQuery();

            List<Map<String, Object>> orderItems = new ArrayList<>();
            BigDecimal totalAmount = BigDecimal.ZERO;
            boolean hasErrors = false;
            String errorMessage = "";

            while (rs.next()) {
                int itemId = rs.getInt("item_id");
                int quantity = rs.getInt("quantity");
                BigDecimal price = rs.getBigDecimal("price");
                int stockQuantity = rs.getInt("stock_quantity");
                boolean available = rs.getBoolean("available");

                if (!available) {
                    hasErrors = true;
                    errorMessage = "Some items in your cart are no longer available";
                    break;
                }

                if (quantity > stockQuantity) {
                    hasErrors = true;
                    errorMessage = "Insufficient stock for some items in your cart";
                    break;
                }

                BigDecimal subtotal = price.multiply(new BigDecimal(quantity));
                totalAmount = totalAmount.add(subtotal);

                Map<String, Object> orderItem = new HashMap<>();
                orderItem.put("itemId", itemId);
                orderItem.put("quantity", quantity);
                orderItem.put("price", price);
                orderItem.put("subtotal", subtotal);
                orderItems.add(orderItem);
            }

            if (hasErrors) {
                conn.rollback();

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", errorMessage);

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }

            if (orderItems.isEmpty()) {
                conn.rollback();

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Cart is empty");

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }

            // Simulate order processing (in a real system, this would create orders table entries)
            // For now, we'll just clear the cart and return success

            // Clear the cart
            String clearCartSql = "DELETE FROM cart WHERE user_id = ?";
            PreparedStatement clearStmt = conn.prepareStatement(clearCartSql);
            clearStmt.setInt(1, userId);
            clearStmt.executeUpdate();

            conn.commit(); // Commit transaction

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Order placed successfully");
            successResponse.put("orderId", "PH" + System.currentTimeMillis()); // Generate simple order ID
            successResponse.put("totalAmount", totalAmount);
            successResponse.put("itemCount", orderItems.size());
            successResponse.put("orderItems", orderItems);

            response.getWriter().write(objectMapper.writeValueAsString(successResponse));

            logger.info("Order placed for user: " + userId + ", Total: " + totalAmount);

        } catch (SQLException e) {
            logger.severe("Database error during checkout: " + e.getMessage());

            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                logger.severe("Error rolling back checkout transaction: " + rollbackEx.getMessage());
            }

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error processing checkout");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                logger.warning("Error resetting auto-commit: " + e.getMessage());
            }

            DatabaseUtil.closeResultSet(rs);
            DatabaseUtil.closeStatement(stmt);
            DatabaseUtil.closeConnection(conn);
        }
    }
}