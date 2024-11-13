package com.github.jrhee17;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.linecorp.armeria.client.grpc.GrpcClients;
import com.linecorp.armeria.server.ServerBuilder;
import com.linecorp.armeria.server.grpc.AsyncServerInterceptor;
import com.linecorp.armeria.server.grpc.GrpcService;
import com.linecorp.armeria.testing.junit5.server.ServerExtension;

import io.grpc.Context;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCall.Listener;
import io.grpc.ServerCallHandler;
import io.grpc.stub.StreamObserver;
import testing.grpc.Proto3.Proto3Message;
import testing.grpc.Proto3ServiceGrpc.Proto3ServiceBlockingStub;
import testing.grpc.Proto3ServiceGrpc.Proto3ServiceImplBase;

class GrpcTest {

    @RegisterExtension
    static ServerExtension server = new ServerExtension() {
        @Override
        protected void configure(ServerBuilder sb) throws Exception {
            sb.service(GrpcService.builder()
                                  .intercept(new AsyncServerInterceptor() {
                                      @Override
                                      public <I, O> CompletableFuture<Listener<I>> asyncInterceptCall(
                                              ServerCall<I, O> call, Metadata headers,
                                              ServerCallHandler<I, O> next) {
                                          final Context context = Context.current();
                                          return CompletableFuture.supplyAsync(() -> {
                                              try {
                                                  return context.call(() -> next.startCall(call, headers));
                                              } catch (Exception e) {
                                                  throw new RuntimeException(e);
                                              }
                                          });
                                      }
                                  })
                                  .addService(new Proto3ServiceImplBase() {
                                      @Override
                                      public void echo(Proto3Message request,
                                                       StreamObserver<Proto3Message> responseObserver) {
                                          responseObserver.onNext(request);
                                          responseObserver.onCompleted();
                                      }
                                  })
                                  .build());
        }
    };

    @Test
    void testAdf() {
        final Proto3ServiceBlockingStub stub = GrpcClients.builder(server.httpUri())
                                                          .build(Proto3ServiceBlockingStub.class);
        final Proto3Message res = stub.echo(Proto3Message.newBuilder()
                                                         .setFooValue(3)
                                                         .build());
        System.out.println(res);
    }
}
