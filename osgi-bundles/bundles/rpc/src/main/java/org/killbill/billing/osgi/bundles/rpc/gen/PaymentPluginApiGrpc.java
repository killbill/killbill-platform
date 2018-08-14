package org.killbill.billing.osgi.bundles.rpc.gen;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler (version 1.14.0)",
    comments = "Source: payment/payment_plugin_api.proto")
public final class PaymentPluginApiGrpc {

  private PaymentPluginApiGrpc() {}

  public static final String SERVICE_NAME = "payment.PaymentPluginApi";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest,
      org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin> getPurchasePaymentMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "PurchasePayment",
      requestType = org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest.class,
      responseType = org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest,
      org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin> getPurchasePaymentMethod() {
    io.grpc.MethodDescriptor<org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest, org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin> getPurchasePaymentMethod;
    if ((getPurchasePaymentMethod = PaymentPluginApiGrpc.getPurchasePaymentMethod) == null) {
      synchronized (PaymentPluginApiGrpc.class) {
        if ((getPurchasePaymentMethod = PaymentPluginApiGrpc.getPurchasePaymentMethod) == null) {
          PaymentPluginApiGrpc.getPurchasePaymentMethod = getPurchasePaymentMethod = 
              io.grpc.MethodDescriptor.<org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest, org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(
                  "payment.PaymentPluginApi", "PurchasePayment"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin.getDefaultInstance()))
                  .setSchemaDescriptor(new PaymentPluginApiMethodDescriptorSupplier("PurchasePayment"))
                  .build();
          }
        }
     }
     return getPurchasePaymentMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PaymentPluginApiStub newStub(io.grpc.Channel channel) {
    return new PaymentPluginApiStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PaymentPluginApiBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    return new PaymentPluginApiBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PaymentPluginApiFutureStub newFutureStub(
      io.grpc.Channel channel) {
    return new PaymentPluginApiFutureStub(channel);
  }

  /**
   */
  public static abstract class PaymentPluginApiImplBase implements io.grpc.BindableService {

    /**
     */
    public void purchasePayment(org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest request,
        io.grpc.stub.StreamObserver<org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin> responseObserver) {
      asyncUnimplementedUnaryCall(getPurchasePaymentMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getPurchasePaymentMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest,
                org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin>(
                  this, METHODID_PURCHASE_PAYMENT)))
          .build();
    }
  }

  /**
   */
  public static final class PaymentPluginApiStub extends io.grpc.stub.AbstractStub<PaymentPluginApiStub> {
    private PaymentPluginApiStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PaymentPluginApiStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PaymentPluginApiStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PaymentPluginApiStub(channel, callOptions);
    }

    /**
     */
    public void purchasePayment(org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest request,
        io.grpc.stub.StreamObserver<org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPurchasePaymentMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PaymentPluginApiBlockingStub extends io.grpc.stub.AbstractStub<PaymentPluginApiBlockingStub> {
    private PaymentPluginApiBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PaymentPluginApiBlockingStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PaymentPluginApiBlockingStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PaymentPluginApiBlockingStub(channel, callOptions);
    }

    /**
     */
    public org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin purchasePayment(org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest request) {
      return blockingUnaryCall(
          getChannel(), getPurchasePaymentMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PaymentPluginApiFutureStub extends io.grpc.stub.AbstractStub<PaymentPluginApiFutureStub> {
    private PaymentPluginApiFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PaymentPluginApiFutureStub(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PaymentPluginApiFutureStub build(io.grpc.Channel channel,
        io.grpc.CallOptions callOptions) {
      return new PaymentPluginApiFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin> purchasePayment(
        org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getPurchasePaymentMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PURCHASE_PAYMENT = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PaymentPluginApiImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PaymentPluginApiImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PURCHASE_PAYMENT:
          serviceImpl.purchasePayment((org.killbill.billing.osgi.bundles.rpc.gen.PaymentRequest) request,
              (io.grpc.stub.StreamObserver<org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPlugin>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class PaymentPluginApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PaymentPluginApiBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return org.killbill.billing.osgi.bundles.rpc.gen.PaymentPluginApiOuterClass.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PaymentPluginApi");
    }
  }

  private static final class PaymentPluginApiFileDescriptorSupplier
      extends PaymentPluginApiBaseDescriptorSupplier {
    PaymentPluginApiFileDescriptorSupplier() {}
  }

  private static final class PaymentPluginApiMethodDescriptorSupplier
      extends PaymentPluginApiBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PaymentPluginApiMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PaymentPluginApiGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new PaymentPluginApiFileDescriptorSupplier())
              .addMethod(getPurchasePaymentMethod())
              .build();
        }
      }
    }
    return result;
  }
}
