package com.github.jrhee17;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.linecorp.armeria.client.logging.LoggingClient;
import com.linecorp.armeria.client.retrofit2.ArmeriaRetrofit;
import com.linecorp.armeria.common.HttpResponse;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.logging.LoggingService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import retrofit2.Converter;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.http.GET;

class RetrofitTest {

    private static final Logger logger = LoggerFactory.getLogger(RetrofitTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Converter.Factory converterFactory = JacksonConverterFactory.create(objectMapper);

    @RegisterExtension
    static ServerExtension server1 = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.decorator(LoggingService.newDecorator());
            sb.service("/", (ctx, req) -> HttpResponse.ofJson(new Pojo("hello")));
        }
    };

    @RegisterExtension
    static ServerExtension server2 = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.decorator(LoggingService.newDecorator());
            sb.service("/", (ctx, req) -> server1.webClient().execute(req));
        }
    };

    @RegisterExtension
    static ServerExtension server3 = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.decorator(LoggingService.newDecorator());
            sb.service("/", (ctx, req) -> server2.webClient().execute(req));
        }
    };

    @Test
    void testHello() throws Exception {
        Service service = ArmeriaRetrofit.builder(server3.httpUri())
                                         .addConverterFactory(converterFactory)
                                         .decorator((delegate, ctx, req) -> {
                                             final Context current = Context.current();
                                             ctx.hook(current::makeCurrent);
                                             logger.info("traceId from custom decorator: {}", Span.current().getSpanContext().getTraceId());
                                             logger.info("traceparent from header: {}", ctx.additionalRequestHeaders().get("traceparent"));
                                             return delegate.execute(ctx, req);
                                         })
                                         .decorator(LoggingClient.builder()
                                                                 .newDecorator())
                                         .build()
                                         .create(Service.class);
        Pojo pojo = service.pojo().join();
        assertThat(pojo.name).isEqualTo("hello");
    }

    interface Service {

        @GET("/")
        CompletableFuture<Pojo> pojo();
    }

    public static class Pojo {
        @JsonCreator
        Pojo(@JsonProperty("name") String name) {
            this.name = name;
        }

        @JsonProperty("name")
        String name;
    }
}
