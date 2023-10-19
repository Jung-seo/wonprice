package main.wonprice.server;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ServerController {

    @Value("${serverName}")
    private String serverName;

    @GetMapping("/getServerInfo")
    public ResponseEntity getServerInfo() {
        return new ResponseEntity(serverName, HttpStatus.OK);
    }
}
