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

@WebServlet("/viewall")
public class ViewAllServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(ViewAllServlet.class.getName());
    private ObjectMapper objectMapper = new ObjectMapper();

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

        String format = request.getParameter("format");
        String category = request.getParameter("category");

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            String sql = "SELECT item_id, item_number, name, color, description, price, " +
                    "available, stock_quantity, category FROM items";

            // Add category filter if specified
            if (category != null && !category.trim().isEmpty()) {
                sql += " WHERE category = ?";
            }

            sql += " ORDER BY item_number";

            stmt = conn.prepareStatement(sql);

            if (category != null && !category.trim().isEmpty()) {
                stmt.setString(1, category);
            }

            rs = stmt.executeQuery();

            List<Items> itemsList = new ArrayList<>();
            BigDecimal totalValue = BigDecimal.ZERO;

            while (rs.next()) {
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

                itemsList.add(item);
                totalValue = totalValue.add(item.getPrice());
            }

            if ("json".equals(format)) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("items", itemsList);
                responseData.put("totalItems", itemsList.size());
                responseData.put("totalValue", totalValue);
                responseData.put("message", "Items retrieved successfully");

                response.getWriter().write(objectMapper.writeValueAsString(responseData));
            } else {
                // Set items as request attribute and forward to JSP
                request.setAttribute("items", itemsList);
                request.setAttribute("totalItems", itemsList.size());
                request.setAttribute("totalValue", totalValue);
                request.getRequestDispatcher("viewall.jsp").forward(request, response);
            }

            logger.info("Retrieved " + itemsList.size() + " items for viewing");

        } catch (SQLException e) {
            logger.severe("Database error while retrieving items: " + e.getMessage());

            if ("json".equals(format)) {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.setContentType("application/json");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Error retrieving items");

                response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            } else {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving items");
            }

        } finally {
            DatabaseUtil.closeResultSet(rs);
            DatabaseUtil.closeStatement(stmt);
            DatabaseUtil.closeConnection(conn);
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

        if ("getCategories".equals(action)) {
            handleGetCategories(request, response);
        } else if ("getItemsByCategory".equals(action)) {
            handleGetItemsByCategory(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid action");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private void handleGetCategories(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();
            String sql = "SELECT DISTINCT category FROM items WHERE category IS NOT NULL ORDER BY category";

            stmt = conn.prepareStatement(sql);
            rs = stmt.executeQuery();

            List<String> categories = new ArrayList<>();
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("categories", categories);

            response.getWriter().write(objectMapper.writeValueAsString(responseData));

        } catch (SQLException e) {
            logger.severe("Database error while retrieving categories: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving categories");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } finally {
            DatabaseUtil.closeResultSet(rs);
            DatabaseUtil.closeStatement(stmt);
            DatabaseUtil.closeConnection(conn);
        }
    }

    private void handleGetItemsByCategory(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String category = request.getParameter("category");

        if (category == null || category.trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Category parameter is required");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
            return;
        }

        // Delegate to doGet with category parameter
        request.setAttribute("javax.servlet.forward.request_uri", request.getRequestURI());
        request.setAttribute("javax.servlet.forward.context_path", request.getContextPath());
        request.setAttribute("javax.servlet.forward.servlet_path", request.getServletPath());

        try {
            doGet(request, response);
        } catch (ServletException e) {
            logger.severe("Error delegating to doGet: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error retrieving items by category");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }
}