package com.github.jrhee17;

import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.Server;

public class Main {
    static int SERVER1_PORT = 8080;
    static int SERVER2_PORT = 8081;
    static int SERVER3_PORT = 8082;

    static Endpoint ENDPOINT1 = Endpoint.of("127.0.0.1", SERVER1_PORT);
    static Endpoint ENDPOINT2 = Endpoint.of("127.0.0.1", SERVER2_PORT);
    static Endpoint ENDPOINT3 = Endpoint.of("127.0.0.1", SERVER3_PORT);

    public static void main(String[] args) {
//        Server server1 = Server.builder()
//                               .port(SERVER1_PORT)
//                               .service("/", (ctx, req) -> )
//                               .build();
//        Server server2 = Server.builder()
//                               .port(SERVER2_PORT)
//                               .service("/", (ctx, req) -> HttpResponse.of(200))
//                               .build();
    }
}