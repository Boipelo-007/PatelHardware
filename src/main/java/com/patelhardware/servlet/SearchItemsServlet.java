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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@WebServlet("/search")
public class SearchItemsServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(SearchItemsServlet.class.getName());
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

        String query = request.getParameter("query");
        String searchType = request.getParameter("searchType");
        String category = request.getParameter("category");
        String minPrice = request.getParameter("minPrice");
        String maxPrice = request.getParameter("maxPrice");

        if (query == null || query.trim().isEmpty()) {
            if ("json".equals(request.getParameter("format"))) {
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");

                Map<String, Object> responseData = new HashMap<>();
                responseData.put("success", true);
                responseData.put("items", new ArrayList<>());
                responseData.put("totalItems", 0);
                responseData.put("message", "No search query provided");

                response.getWriter().write(objectMapper.writeValueAsString(responseData));
            } else {
                response.sendRedirect("search.html");
            }
            return;
        }

        handleSearch(request, response, query, searchType, category, minPrice, maxPrice);
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

        if ("search".equals(action)) {
            String query = request.getParameter("query");
            String searchType = request.getParameter("searchType");
            String category = request.getParameter("category");
            String minPrice = request.getParameter("minPrice");
            String maxPrice = request.getParameter("maxPrice");

            handleSearch(request, response, query, searchType, category, minPrice, maxPrice);
        } else if ("getSearchSuggestions".equals(action)) {
            handleSearchSuggestions(request, response);
        } else {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Invalid action");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
        }
    }

    private void handleSearch(HttpServletRequest request, HttpServletResponse response,
                              String query, String searchType, String category,
                              String minPrice, String maxPrice) throws IOException {

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();

            // Build dynamic SQL query
            StringBuilder sqlBuilder = new StringBuilder();
            sqlBuilder.append("SELECT item_id, item_number, name, color, description, price, ")
                    .append("available, stock_quantity, category FROM items WHERE ");

            List<Object> parameters = new ArrayList<>();
            List<String> conditions = new ArrayList<>();

            // Search conditions based on search type
            if ("name".equals(searchType)) {
                conditions.add("name LIKE ?");
                parameters.add("%" + query + "%");
            } else if ("description".equals(searchType)) {
                conditions.add("description LIKE ?");
                parameters.add("%" + query + "%");
            } else if ("itemNumber".equals(searchType)) {
                conditions.add("item_number LIKE ?");
                parameters.add("%" + query + "%");
            } else {
                // Default: search in all text fields
                conditions.add("(name LIKE ? OR description LIKE ? OR item_number LIKE ?)");
                parameters.add("%" + query + "%");
                parameters.add("%" + query + "%");
                parameters.add("%" + query + "%");
            }

            // Category filter
            if (category != null && !category.trim().isEmpty() && !"all".equals(category)) {
                conditions.add("category = ?");
                parameters.add(category);
            }

            // Price range filter
            if (minPrice != null && !minPrice.trim().isEmpty()) {
                try {
                    double min = Double.parseDouble(minPrice);
                    conditions.add("price >= ?");
                    parameters.add(min);
                } catch (NumberFormatException e) {
                    logger.warning("Invalid minimum price format: " + minPrice);
                }
            }

            if (maxPrice != null && !maxPrice.trim().isEmpty()) {
                try {
                    double max = Double.parseDouble(maxPrice);
                    conditions.add("price <= ?");
                    parameters.add(max);
                } catch (NumberFormatException e) {
                    logger.warning("Invalid maximum price format: " + maxPrice);
                }
            }

            // Join conditions
            sqlBuilder.append(String.join(" AND ", conditions));
            sqlBuilder.append(" ORDER BY name, item_number");

            stmt = conn.prepareStatement(sqlBuilder.toString());

            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                Object param = parameters.get(i);
                if (param instanceof String) {
                    stmt.setString(i + 1, (String) param);
                } else if (param instanceof Double) {
                    stmt.setDouble(i + 1, (Double) param);
                }
            }

            rs = stmt.executeQuery();

            List<Items> searchResults = new ArrayList<>();

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

                searchResults.add(item);
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("items", searchResults);
            responseData.put("totalItems", searchResults.size());
            responseData.put("query", query);
            responseData.put("searchType", searchType);
            responseData.put("message", "Search completed successfully");

            // Add search statistics
            Map<String, Object> searchStats = new HashMap<>();
            searchStats.put("totalResults", searchResults.size());
            searchStats.put("availableItems", searchResults.stream().mapToInt(item -> item.isAvailable() ? 1 : 0).sum());
            searchStats.put("categories", searchResults.stream().map(Items::getCategory).distinct().count());
            responseData.put("searchStats", searchStats);

            response.getWriter().write(objectMapper.writeValueAsString(responseData));

            logger.info("Search performed - Query: '" + query + "', Results: " + searchResults.size());

        } catch (SQLException e) {
            logger.severe("Database error during search: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error performing search");
            errorResponse.put("query", query);

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } finally {
            DatabaseUtil.closeResultSet(rs);
            DatabaseUtil.closeStatement(stmt);
            DatabaseUtil.closeConnection(conn);
        }
    }

    private void handleSearchSuggestions(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String partialQuery = request.getParameter("partialQuery");

        if (partialQuery == null || partialQuery.trim().length() < 2) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("suggestions", new ArrayList<>());

            response.getWriter().write(objectMapper.writeValueAsString(responseData));
            return;
        }

        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();

            String sql = "SELECT DISTINCT name FROM items WHERE name LIKE ? " +
                    "UNION " +
                    "SELECT DISTINCT item_number FROM items WHERE item_number LIKE ? " +
                    "LIMIT 10";

            stmt = conn.prepareStatement(sql);
            String searchPattern = partialQuery + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);

            rs = stmt.executeQuery();

            List<String> suggestions = new ArrayList<>();
            while (rs.next()) {
                suggestions.add(rs.getString(1));
            }

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("success", true);
            responseData.put("suggestions", suggestions);
            responseData.put("partialQuery", partialQuery);

            response.getWriter().write(objectMapper.writeValueAsString(responseData));

        } catch (SQLException e) {
            logger.severe("Database error while getting search suggestions: " + e.getMessage());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.setContentType("application/json");

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Error getting search suggestions");

            response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

        } finally {
            DatabaseUtil.closeResultSet(rs);
            DatabaseUtil.closeStatement(stmt);
            DatabaseUtil.closeConnection(conn);
        }
    }
}