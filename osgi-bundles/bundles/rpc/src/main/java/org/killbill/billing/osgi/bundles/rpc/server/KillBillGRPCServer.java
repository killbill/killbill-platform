/*
 * Copyright 2014-2018 Groupon, Inc
 * Copyright 2014-2018 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.billing.osgi.bundles.rpc.server;

import java.io.IOException;
import java.util.Hashtable;

import org.killbill.billing.osgi.api.OSGIKillbillRegistrar;
import org.killbill.billing.osgi.api.OSGIPluginProperties;
import org.killbill.billing.osgi.bundles.rpc.RPCPaymentPluginApiClient;
import org.killbill.billing.payment.plugin.api.PaymentPluginApi;
import org.killbill.billing.rpc.killbill.registration.gen.PluginRegistrationApiGrpc;
import org.killbill.billing.rpc.killbill.registration.gen.RegistrationRequest;
import org.killbill.billing.rpc.killbill.registration.gen.RegistrationRequest.PluginType;
import org.killbill.billing.rpc.killbill.registration.gen.RegistrationResponse;
import org.killbill.billing.rpc.plugin.lifecycle.gen.LifecyclePluginApiGrpc;
import org.killbill.billing.rpc.plugin.lifecycle.gen.LifecyclePluginApiGrpc.LifecyclePluginApiBlockingStub;
import org.killbill.billing.rpc.plugin.lifecycle.gen.LifecycleRequest;
import org.killbill.billing.rpc.plugin.lifecycle.gen.LifecycleResponse;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;

public class KillBillGRPCServer {


    public static final int DEFAULT_SERVER_PORT = 21345;

    private static final Logger logger = LoggerFactory.getLogger(KillBillGRPCServer.class);

    private final int    port;
    private final Server server;


    public KillBillGRPCServer(final int port, final BundleContext context, final OSGIKillbillRegistrar registrar) {
        this.port = port;
        server = ServerBuilder.forPort(port).addService(new PluginRegistrationService(registrar, context))
                              .build();
    }

    private static class PluginRegistrationService extends PluginRegistrationApiGrpc.PluginRegistrationApiImplBase {


        private final OSGIKillbillRegistrar registrar;
        private final BundleContext context;

        public PluginRegistrationService(final OSGIKillbillRegistrar registrar, final BundleContext context) {
            this.registrar = registrar;
            this.context = context;
        }

        public void register(RegistrationRequest request, StreamObserver<RegistrationResponse> responseObserver) {

            logger.info("Got request " + request);

            final ManagedChannel channel = createChannel(request.getKey(), request.getEndpoint());
            for (final PluginType type : request.getTypeList()) {
                if (type == PluginType.PAYMENT) {
                    final RPCPaymentPluginApiClient paymentPluginApiClient = new RPCPaymentPluginApiClient(channel);
                    registerPaymentPluginApi(context, paymentPluginApiClient, request.getKey());
                }
            }
            responseObserver.onNext(RegistrationResponse.newBuilder().setSuccess(true).build());
            responseObserver.onCompleted();
        }

        private void registerPaymentPluginApi(final BundleContext context, final PaymentPluginApi api, final String pluginName) {
            final Hashtable<String, String> props = new Hashtable<String, String>();
            props.put(OSGIPluginProperties.PLUGIN_NAME_PROP, pluginName);
            registrar.registerService(context, PaymentPluginApi.class, api, props);
        }


        private ManagedChannel createChannel(final String key, final String endpoint) {
            final ManagedChannelBuilder managedChannelBuilder = ManagedChannelBuilder.forTarget(endpoint)
                                                                                     .usePlaintext(true);

            final ManagedChannel channel = managedChannelBuilder.build();
            // Registration triggers start() -- we could consider a registration mode to make that explicit instead
            final LifecyclePluginApiBlockingStub stub = LifecyclePluginApiGrpc.newBlockingStub(channel);
            final LifecycleResponse response = stub.start(LifecycleRequest.newBuilder().build());
            if (!response.getSuccess()) {
                logger.warn("Failed to start remote plugin '%s'", key);
                channel.shutdownNow();
            }
            return channel;
        }
    }



    public void start() throws IOException {
        server.start();
        logger.info("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may has been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                KillBillGRPCServer.this.stop();
                System.err.println("*** server shut down");
            }
        });
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

}
