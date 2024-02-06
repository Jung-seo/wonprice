package main.wonprice.auth.exception;

import com.google.gson.Gson;
import main.wonprice.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class ErrorResponder {
    public static void sendErrorResponse(HttpServletResponse response, HttpStatus status, String exceptionName) throws IOException {

        Gson gson = new Gson();
        ErrorResponse errorResponse = ErrorResponse.of(status, exceptionName);

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        response.getWriter().write(gson.toJson(errorResponse, ErrorResponse.class));
    }
}
