package com.patelhardware.servlet;

import com.patelhardware.model.Items;
import com.patelhardware.util.DatabaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@WebServlet("/buyitem")
public class BuyItemsServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(BuyItemsServlet.class.getName());
    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        HttpSession session = request.getSession(false);

        // Check if user is logged in
        if (session == null || session.getAttribute("user") == null) {
            response.sendRedirect("login.html");
            return;
        }

        String itemId = request.getParameter("itemId");
        String itemNumber = request.getParameter("itemNumber");

        if (itemId != null || itemNumber != null) {
            // Get specific item details
            handleGetItemDetails(request, response, itemId, itemNumber);
        } else {
            // Redirect to buy item page
            response.sendRedirect("buyitem.html");
        }
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

        String action = request.getParameter("action");

        if ("addToCart".equals(action)) {
            handleAddToCart(request, response);
        } else if ("getItemDetails".equals(action)) {
            String itemId = request.getParameter("itemId");
            String itemNumber = request.getParameter("itemNumber");
            handleGetItemDetails(request, response, itemId, itemNumber);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid action");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private void handleGetItemDetails(HttpServletRequest request, HttpServletResponse response,
                                      String itemId, String itemNumber) throws IOException {

        if ((itemId == null || itemId.trim().isEmpty()) &&
                (itemNumber == null || itemNumber.trim().isEmpty())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Item ID or Item Number is required");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            String sql = "SELECT item_id, item_number, name, color, description, price, " +
                    "available, stock_quantity, category FROM items WHERE ";

            if (itemId != null && !itemId.trim().isEmpty()) {
                sql += "item_id = ?";
            } else {
                sql += "item_number = ?";
            }

            stmt = conn.prepareStatement(sql);

            if (itemId != null && !itemId.trim().isEmpty()) {
                stmt.setInt(1, Integer.parseInt(itemId));
            } else {
                stmt.setString(1, itemNumber);
            }

            rs = stmt.executeQuery();

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            if (rs.next()) {
                Items item = new Items();
                item.setItemId(rs.getInt("item_id"));
                item.setItemNumber(rs.getString("item_number"));
                item.setName(rs.getString("name"));
                item.setColor(rs.getString("color"));
                item.setDescription(rs.getString("description"));
                item.setPrice(rs.getBigDecimal("price"));
                item.setAvailable(rs.getBoolean("available"));
                item.setStockQuantity(rs.getInt("stock_quantity"));
                item.setCategory(rs.getString("category"));

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("item", item);
                responseData.put("message", "Item details retrieved successfully");

                response.getWriter().write(objectMapper.writeValueAsString(responseData));
            } else {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Item not found");

                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            }

        } catch (SQLException e) {
            logger.severe("Database error while retrieving item details: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving item details");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid item ID format");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } finally {
            DatabaseUtil.closeResultSet(rs);
            DatabaseUtil.closeStatement(stmt);
            DatabaseUtil.closeConnection(conn);
        }
    }

    private void handleAddToCart(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession();
        Integer userId = (Integer) session.getAttribute("userId");

        String itemIdStr = request.getParameter("itemId");
        String quantityStr = request.getParameter("quantity");

        if (itemIdStr == null || itemIdStr.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Item ID is required");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        int itemId;
        int quantity = 1; // Default quantity

        try {
            itemId = Integer.parseInt(itemIdStr);
            if (quantityStr != null && !quantityStr.trim().isEmpty()) {
                quantity = Integer.parseInt(quantityStr);
                if (quantity <= 0) {
                    quantity = 1;
                }
            }
        } catch (NumberFormatException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid item ID or quantity format");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        PreparedStatement updateStmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            conn.setAutoCommit(false); // Start transaction

            // Check if item exists and is available
            String checkItemSql = "SELECT available, stock_quantity FROM items WHERE item_id = ?";
            checkStmt = conn.prepareStatement(checkItemSql);
            checkStmt.setInt(1, itemId);
            rs = checkStmt.executeQuery();

            if (!rs.next()) {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Item not found");

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }

            boolean available = rs.getBoolean("available");
            int stockQuantity = rs.getInt("stock_quantity");

            if (!available) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Item is not available");

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }

            if (stockQuantity < quantity) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Insufficient stock. Available: " + stockQuantity);

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                return;
            }

            // Check if item already exists in cart
            String checkCartSql = "SELECT quantity FROM cart WHERE user_id = ? AND item_id = ?";
            PreparedStatement checkCartStmt = conn.prepareStatement(checkCartSql);
            checkCartStmt.setInt(1, userId);
            checkCartStmt.setInt(2, itemId);
            ResultSet cartRs = checkCartStmt.executeQuery();

            if (cartRs.next()) {
                // Update existing cart item
                int existingQuantity = cartRs.getInt("quantity");
                int newQuantity = existingQuantity + quantity;

                if (newQuantity > stockQuantity) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.setContentType("application/json");

                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("message", "Total quantity exceeds stock. Available: " + stockQuantity +
                            ", Current in cart: " + existingQuantity);

                    response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
                    return;
                }

                String updateCartSql = "UPDATE cart SET quantity = ? WHERE user_id = ? AND item_id = ?";
                updateStmt = conn.prepareStatement(updateCartSql);
                updateStmt.setInt(1, newQuantity);
                updateStmt.setInt(2, userId);
                updateStmt.setInt(3, itemId);
                updateStmt.executeUpdate();

            } else {
                // Insert new cart item
                String insertCartSql = "INSERT INTO cart (user_id, item_id, quantity) VALUES (?, ?, ?)";
                insertStmt = conn.prepareStatement(insertCartSql);
                insertStmt.setInt(1, userId);
                insertStmt.setInt(2, itemId);
                insertStmt.setInt(3, quantity);
                insertStmt.executeUpdate();
            }

            conn.commit(); // Commit transaction

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> successResponse = new HashMap<>();
            successResponse.put("success", true);
            successResponse.put("message", "Item added to cart successfully");
            successResponse.put("itemId", itemId);
            successResponse.put("quantity", quantity);

            response.getWriter().write(objectMapper.writeValueAsString(successResponse));

            logger.info("Item added to cart - User: " + userId + ", Item: " + itemId + ", Quantity: " + quantity);

        } catch (SQLException e) {
            logger.severe("Database error while adding item to cart: " + e.getMessage());

            try {
                if (conn != null) {
                    conn.rollback();
                }
            } catch (SQLException rollbackEx) {
                logger.severe("Error rolling back transaction: " + rollbackEx.getMessage());
            }

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error adding item to cart");

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
            DatabaseUtil.closeStatement(checkStmt);
            DatabaseUtil.closeStatement(insertStmt);
            DatabaseUtil.closeStatement(updateStmt);
            DatabaseUtil.closeConnection(conn);
        }
    }

}