// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: payment/payment_plugin_api.proto

package org.killbill.billing.osgi.bundles.rpc.gen;

public final class PaymentPluginApiOuterClass {
  private PaymentPluginApiOuterClass() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistryLite registry) {
  }

  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
    registerAllExtensions(
        (com.google.protobuf.ExtensionRegistryLite) registry);
  }
  static final com.google.protobuf.Descriptors.Descriptor
    internal_static_payment_PaymentRequest_descriptor;
  static final 
    com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
      internal_static_payment_PaymentRequest_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n payment/payment_plugin_api.proto\022\007paym" +
      "ent\032\032payment/call_context.proto\032-payment" +
      "/payment_transaction_info_plugin.proto\032\035" +
      "payment/plugin_property.proto\"\355\001\n\016Paymen" +
      "tRequest\022\025\n\rkb_account_id\030\001 \002(\t\022\025\n\rkb_pa" +
      "yment_id\030\002 \002(\t\022\031\n\021kb_transaction_id\030\003 \002(" +
      "\t\022\034\n\024kb_payment_method_id\030\004 \002(\t\022\016\n\006amoun" +
      "t\030\005 \002(\t\022\020\n\010currency\030\006 \002(\t\022+\n\nproperties\030" +
      "\007 \003(\0132\027.payment.PluginProperty\022%\n\007contex" +
      "t\030\010 \002(\0132\024.payment.CallContext2e\n\020Payment" +
      "PluginApi\022Q\n\017PurchasePayment\022\027.payment.P" +
      "aymentRequest\032%.payment.PaymentTransacti" +
      "onInfoPluginB-\n)org.killbill.billing.osg" +
      "i.bundles.rpc.genP\001"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
          org.killbill.billing.osgi.bundles.rpc.gen.CallContextOuterClass.getDescriptor(),
          org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPluginOuterClass.getDescriptor(),
          org.killbill.billing.osgi.bundles.rpc.gen.PluginPropertyOuterClass.getDescriptor(),
        }, assigner);
    internal_static_payment_PaymentRequest_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_payment_PaymentRequest_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessageV3.FieldAccessorTable(
        internal_static_payment_PaymentRequest_descriptor,
        new java.lang.String[] { "KbAccountId", "KbPaymentId", "KbTransactionId", "KbPaymentMethodId", "Amount", "Currency", "Properties", "Context", });
    org.killbill.billing.osgi.bundles.rpc.gen.CallContextOuterClass.getDescriptor();
    org.killbill.billing.osgi.bundles.rpc.gen.PaymentTransactionInfoPluginOuterClass.getDescriptor();
    org.killbill.billing.osgi.bundles.rpc.gen.PluginPropertyOuterClass.getDescriptor();
  }

  // @@protoc_insertion_point(outer_class_scope)
}