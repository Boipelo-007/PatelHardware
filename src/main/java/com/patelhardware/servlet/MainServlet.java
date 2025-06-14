package com.patelhardware.servlet;

import com.patelhardware.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@WebServlet("/main")
public class MainServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(MainServlet.class.getName());
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

        User user = (User) session.getAttribute("user");
        String action = request.getParameter("action");

        if ("logout".equals(action)) {
            handleLogout(request, response);
            return;
        }

        // Return main page data as JSON for AJAX requests
        if ("json".equals(request.getParameter("format"))) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> mainData = new HashMap<>();
            mainData.put("success", true);
            mainData.put("user", user);
            mainData.put("message", "Welcome to Patel Hardware");

            Map<String, String> menuOptions = new HashMap<>();
            menuOptions.put("viewAll", "View All Items");
            menuOptions.put("buyItem", "Buy an Item");
            menuOptions.put("viewCart", "View Cart");
            menuOptions.put("search", "Search Items");
            menuOptions.put("logout", "Logout");

            mainData.put("menuOptions", menuOptions);

            response.getWriter().write(objectMapper.writeValueAsString(mainData));
        } else {
            // Redirect to main HTML page
            response.sendRedirect("main.html");
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

        switch (action != null ? action : "") {
            case "getUserInfo":
                handleGetUserInfo(request, response);
                break;
            case "updateProfile":
                handleUpdateProfile(request, response);
                break;
            case "logout":
                handleLogout(request, response);
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

    private void handleGetUserInfo(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("success", true);
        userInfo.put("user", user);
        userInfo.put("sessionId", session.getId());

        response.getWriter().write(objectMapper.writeValueAsString(userInfo));
    }

    private void handleUpdateProfile(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession();
        User user = (User) session.getAttribute("user");

        // Update user information from request parameters
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");

        if (fullName != null && !fullName.trim().isEmpty()) {
            user.setFullName(fullName);
        }
        if (phone != null && !phone.trim().isEmpty()) {
            user.setPhone(phone);
        }
        if (address != null && !address.trim().isEmpty()) {
            user.setAddress(address);
        }

        // Update session
        session.setAttribute("user", user);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> updateResponse = new HashMap<>();
        updateResponse.put("success", true);
        updateResponse.put("message", "Profile updated successfully");
        updateResponse.put("user", user);

        response.getWriter().write(objectMapper.writeValueAsString(updateResponse));

        logger.info("User profile updated: " + user.getUsername());
    }

    private void handleLogout(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        HttpSession session = request.getSession(false);
        if (session != null) {
            String username = (String) session.getAttribute("username");
            session.invalidate();
            logger.info("User logged out: " + username);
        }

        // Check if it's an AJAX request
        if ("json".equals(request.getParameter("format"))) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            Map<String, Object> logoutResponse = new HashMap<>();
            logoutResponse.put("success", true);
            logoutResponse.put("message", "Logout successful");

            response.getWriter().write(objectMapper.writeValueAsString(logoutResponse));
        } else {
            response.sendRedirect("login.html");
        }
    }
}