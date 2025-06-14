package com.patelhardware.servlet;

import com.patelhardware.model.User;
import com.patelhardware.model.RegistrationResponse;
import com.patelhardware.util.DatabaseUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.regex.Pattern;

@WebServlet("/register")
public class RegistrationServlet extends HttpServlet {
    private static final Logger logger = Logger.getLogger(RegistrationServlet.class.getName());
    private ObjectMapper objectMapper = new ObjectMapper();

    // Email validation pattern
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Redirect to registration page
        response.sendRedirect("register.html");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String username = request.getParameter("username");
        String email = request.getParameter("email");
        String password = request.getParameter("password");
        String fullName = request.getParameter("fullName");
        String phone = request.getParameter("phone");
        String address = request.getParameter("address");

        // Validate input
        RegistrationResponse regResponse = validateInput(username, email, password);
        if (!regResponse.isSuccess()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(objectMapper.writeValueAsString(regResponse));
            return;
        }

        Connection conn = null;
        PreparedStatement checkStmt = null;
        PreparedStatement insertStmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseUtil.getConnection();

            // Check if username or email already exists
            String checkSql = "SELECT COUNT(*) FROM users WHERE username = ? OR email = ?";
            checkStmt = conn.prepareStatement(checkSql);
            checkStmt.setString(1, username);
            checkStmt.setString(2, email);

            rs = checkStmt.executeQuery();
            if (rs.next() && rs.getInt(1) > 0) {
                regResponse = new RegistrationResponse(false, "Username or email already exists");
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                response.getWriter().write(objectMapper.writeValueAsString(regResponse));
                return;
            }

            // Hash password
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            // Insert new user
            String insertSql = "INSERT INTO users (username, email, password, full_name, phone, address) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";

            insertStmt = conn.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS);
            insertStmt.setString(1, username);
            insertStmt.setString(2, email);
            insertStmt.setString(3, hashedPassword);
            insertStmt.setString(4, fullName);
            insertStmt.setString(5, phone);
            insertStmt.setString(6, address);

            int affectedRows = insertStmt.executeUpdate();

            if (affectedRows > 0) {
                // Get generated user ID
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    int userId = generatedKeys.getInt(1);

                    User newUser = new User();
                    newUser.setUserId(userId);
                    newUser.setUsername(username);
                    newUser.setEmail(email);
                    newUser.setFullName(fullName);
                    newUser.setPhone(phone);
                    newUser.setAddress(address);

                    regResponse = new RegistrationResponse(true, "Registration successful", newUser);
                    response.setStatus(HttpServletResponse.SC_CREATED);
                    response.getWriter().write(objectMapper.writeValueAsString(regResponse));

                    logger.info("New user registered successfully: " + username);
                } else {
                    regResponse = new RegistrationResponse(false, "Registration failed");
                    response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    response.getWriter().write(objectMapper.writeValueAsString(regResponse));
                }
            } else {
                regResponse = new RegistrationResponse(false, "Registration failed");
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write(objectMapper.writeValueAsString(regResponse));
            }

        } catch (SQLException e) {
            logger.severe("Database error during registration: " + e.getMessage());
            regResponse = new RegistrationResponse(false, "Internal server error");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(objectMapper.writeValueAsString(regResponse));

        } finally {
            DatabaseUtil.closeResultSet(rs);
            DatabaseUtil.closeStatement(checkStmt);
            DatabaseUtil.closeStatement(insertStmt);
            DatabaseUtil.closeConnection(conn);
        }
    }

    private RegistrationResponse validateInput(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            return new RegistrationResponse(false, "Username is required");
        }

        if (email == null || email.trim().isEmpty()) {
            return new RegistrationResponse(false, "Email is required");
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return new RegistrationResponse(false, "Invalid email format");
        }

        if (password == null || password.length() < 6) {
            return new RegistrationResponse(false, "Password must be at least 6 characters long");
        }

        if (username.length() < 3) {
            return new RegistrationResponse(false, "Username must be at least 3 characters long");
        }

        return new RegistrationResponse(true, "Validation passed");
    }
}