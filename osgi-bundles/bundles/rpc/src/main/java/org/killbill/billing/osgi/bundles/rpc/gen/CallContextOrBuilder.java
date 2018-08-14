// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: payment/call_context.proto

package org.killbill.billing.osgi.bundles.rpc.gen;

public interface CallContextOrBuilder extends
    // @@protoc_insertion_point(interface_extends:payment.CallContext)
    com.google.protobuf.MessageOrBuilder {

  /**
   * <code>required string account_id = 1;</code>
   */
  boolean hasAccountId();
  /**
   * <code>required string account_id = 1;</code>
   */
  java.lang.String getAccountId();
  /**
   * <code>required string account_id = 1;</code>
   */
  com.google.protobuf.ByteString
      getAccountIdBytes();

  /**
   * <code>required string tenant_id = 2;</code>
   */
  boolean hasTenantId();
  /**
   * <code>required string tenant_id = 2;</code>
   */
  java.lang.String getTenantId();
  /**
   * <code>required string tenant_id = 2;</code>
   */
  com.google.protobuf.ByteString
      getTenantIdBytes();

  /**
   * <code>required string user_token = 3;</code>
   */
  boolean hasUserToken();
  /**
   * <code>required string user_token = 3;</code>
   */
  java.lang.String getUserToken();
  /**
   * <code>required string user_token = 3;</code>
   */
  com.google.protobuf.ByteString
      getUserTokenBytes();

  /**
   * <code>required string user_name = 4;</code>
   */
  boolean hasUserName();
  /**
   * <code>required string user_name = 4;</code>
   */
  java.lang.String getUserName();
  /**
   * <code>required string user_name = 4;</code>
   */
  com.google.protobuf.ByteString
      getUserNameBytes();

  /**
   * <code>required .payment.CallContext.CallOrigin call_origin = 5;</code>
   */
  boolean hasCallOrigin();
  /**
   * <code>required .payment.CallContext.CallOrigin call_origin = 5;</code>
   */
  org.killbill.billing.osgi.bundles.rpc.gen.CallContext.CallOrigin getCallOrigin();

  /**
   * <code>required .payment.CallContext.UserType user_type = 6;</code>
   */
  boolean hasUserType();
  /**
   * <code>required .payment.CallContext.UserType user_type = 6;</code>
   */
  org.killbill.billing.osgi.bundles.rpc.gen.CallContext.UserType getUserType();

  /**
   * <code>required string reason_code = 7;</code>
   */
  boolean hasReasonCode();
  /**
   * <code>required string reason_code = 7;</code>
   */
  java.lang.String getReasonCode();
  /**
   * <code>required string reason_code = 7;</code>
   */
  com.google.protobuf.ByteString
      getReasonCodeBytes();

  /**
   * <code>required string comments = 8;</code>
   */
  boolean hasComments();
  /**
   * <code>required string comments = 8;</code>
   */
  java.lang.String getComments();
  /**
   * <code>required string comments = 8;</code>
   */
  com.google.protobuf.ByteString
      getCommentsBytes();

  /**
   * <code>required string created_date = 9;</code>
   */
  boolean hasCreatedDate();
  /**
   * <code>required string created_date = 9;</code>
   */
  java.lang.String getCreatedDate();
  /**
   * <code>required string created_date = 9;</code>
   */
  com.google.protobuf.ByteString
      getCreatedDateBytes();

  /**
   * <code>required string updated_date = 10;</code>
   */
  boolean hasUpdatedDate();
  /**
   * <code>required string updated_date = 10;</code>
   */
  java.lang.String getUpdatedDate();
  /**
   * <code>required string updated_date = 10;</code>
   */
  com.google.protobuf.ByteString
      getUpdatedDateBytes();
}