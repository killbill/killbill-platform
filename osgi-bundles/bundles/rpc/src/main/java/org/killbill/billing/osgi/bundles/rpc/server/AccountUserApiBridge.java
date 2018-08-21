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

import java.util.UUID;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.killbill.billing.BillingExceptionBase;
import org.killbill.billing.account.api.AccountApiException;
import org.killbill.billing.account.api.AccountData;
import org.killbill.billing.osgi.libs.killbill.OSGIKillbillAPI;
import org.killbill.billing.rpc.common.gen.BillingException;
import org.killbill.billing.rpc.killbill.account.gen.Account;
import org.killbill.billing.rpc.killbill.account.gen.Account.Builder;
import org.killbill.billing.rpc.killbill.account.gen.AccountApiGrpc;
import org.killbill.billing.rpc.killbill.account.gen.AccountRequest;
import org.killbill.billing.rpc.killbill.account.gen.AccountResponse;
import org.killbill.billing.rpc.killbill.catalog.gen.Currency;
import org.killbill.billing.util.callcontext.CallContext;
import org.killbill.billing.util.callcontext.CallOrigin;
import org.killbill.billing.util.callcontext.TenantContext;
import org.killbill.billing.util.callcontext.UserType;

import com.google.common.base.Strings;
import io.grpc.stub.StreamObserver;

public class AccountUserApiBridge extends AccountApiGrpc.AccountApiImplBase {

    private final OSGIKillbillAPI osgiKillbillAPI;

    public AccountUserApiBridge(final OSGIKillbillAPI osgiKillbillAPI) {
        this.osgiKillbillAPI = osgiKillbillAPI;
    }

    @Override
    public void createAccount(final AccountRequest request, final StreamObserver<AccountResponse> responseObserver) {
        try {
            osgiKillbillAPI.getSecurityApi().login(request.getHttpContext().getUsername(), request.getHttpContext().getPassword());

            final AccountResponse.Builder responseBuilder = AccountResponse.newBuilder();

            try {
                final org.killbill.billing.account.api.Account kbAccount = osgiKillbillAPI.getAccountUserApi().createAccount(new AccountData() {
                    @Override
                    public String getExternalKey() {
                        return request.getAccount().getExternalKey();
                    }

                    @Override
                    public String getName() {
                        return request.getAccount().getName();
                    }

                    @Override
                    public Integer getFirstNameLength() {
                        return request.getAccount().getFirstNameLength();
                    }

                    @Override
                    public String getEmail() {
                        return request.getAccount().getEmail();
                    }

                    @Override
                    public Integer getBillCycleDayLocal() {
                        return request.getAccount().getBillCycleDayLocal();
                    }

                    @Override
                    public org.killbill.billing.catalog.api.Currency getCurrency() {
                        return org.killbill.billing.catalog.api.Currency.fromCode(request.getAccount().getCurrency().toString());
                    }

                    @Override
                    public UUID getPaymentMethodId() {
                        return safeUUIDFromString(request.getAccount().getPaymentMethodId());
                    }

                    @Override
                    public DateTime getReferenceTime() {
                        return toDateTime(request.getAccount().getReferenceTime());
                    }

                    @Override
                    public DateTimeZone getTimeZone() {
                        return toDateTimeZone(request.getAccount().getTimeZone());
                    }

                    @Override
                    public String getLocale() {
                        return request.getAccount().getLocale();
                    }

                    @Override
                    public String getAddress1() {
                        return request.getAccount().getAddress1();
                    }

                    @Override
                    public String getAddress2() {
                        return request.getAccount().getAddress2();
                    }

                    @Override
                    public String getCompanyName() {
                        return request.getAccount().getCompany();
                    }

                    @Override
                    public String getCity() {
                        return request.getAccount().getCity();
                    }

                    @Override
                    public String getStateOrProvince() {
                        return request.getAccount().getState();
                    }

                    @Override
                    public String getPostalCode() {
                        return request.getAccount().getPostalCode();
                    }

                    @Override
                    public String getCountry() {
                        return request.getAccount().getCountry();
                    }

                    @Override
                    public String getPhone() {
                        return request.getAccount().getPhone();
                    }

                    @Override
                    public Boolean isMigrated() {
                        return request.getAccount().getIsMigrated();
                    }

                    @Override
                    public UUID getParentAccountId() {
                        return safeUUIDFromString(request.getAccount().getParentAccountId());
                    }

                    @Override
                    public Boolean isPaymentDelegatedToParent() {
                        return request.getAccount().getIsPaymentDelegatedToParent();
                    }

                    @Override
                    public String getNotes() {
                        return request.getAccount().getNotes();
                    }
                }, new CallContext() {
                    @Override
                    public UUID getUserToken() {
                        return safeUUIDFromString(request.getCallContext().getUserToken());
                    }

                    @Override
                    public String getUserName() {
                        return request.getCallContext().getUserName();
                    }

                    @Override
                    public CallOrigin getCallOrigin() {
                        return CallOrigin.valueOf(request.getCallContext().getCallOrigin().toString());
                    }

                    @Override
                    public UserType getUserType() {
                        return UserType.valueOf(request.getCallContext().getUserType().toString());
                    }

                    @Override
                    public String getReasonCode() {
                        return request.getCallContext().getReasonCode();
                    }

                    @Override
                    public String getComments() {
                        return request.getCallContext().getComments();
                    }

                    @Override
                    public DateTime getCreatedDate() {
                        return toDateTime(request.getCallContext().getCreatedDate());
                    }

                    @Override
                    public DateTime getUpdatedDate() {
                        return toDateTime(request.getCallContext().getUpdatedDate());
                    }

                    @Override
                    public UUID getAccountId() {
                        return safeUUIDFromString(request.getCallContext().getAccountId());
                    }

                    @Override
                    public UUID getTenantId() {
                        return safeUUIDFromString(request.getCallContext().getTenantId());
                    }
                });
                final Account gRPCAccount = kbAccountToGRPCAccount(kbAccount);
                responseBuilder.setAccount(gRPCAccount);
            } catch (final AccountApiException e) {
                final BillingException billingException = toGRPCBillingException(e);
                responseBuilder.setBillingException(billingException);
            } catch (final RuntimeException e) {
                final BillingException billingException = toGRPCBillingException(e);
                responseBuilder.setBillingException(billingException);
            }
            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } finally {
            osgiKillbillAPI.getSecurityApi().logout();
        }

    }

    @Override
    public void updateAccount(final AccountRequest request, final StreamObserver<AccountResponse> responseObserver) {
        super.updateAccount(request, responseObserver);
    }

    @Override
    public void getAccount(final AccountRequest request, final StreamObserver<AccountResponse> responseObserver) {
        final TenantContext tenantContext = new TenantContext() {
            @Override
            public UUID getAccountId() {
                return safeUUIDFromString(request.getCallContext().getAccountId());
            }

            @Override
            public UUID getTenantId() {
                return safeUUIDFromString(request.getCallContext().getTenantId());
            }
        };
        final AccountResponse.Builder responseBuilder = AccountResponse.newBuilder();

        try {
            org.killbill.billing.account.api.Account account = null;
            if (request.getAccount().getAccountId() != null) {
                account = osgiKillbillAPI.getAccountUserApi().getAccountById(safeUUIDFromString(request.getAccount().getAccountId()), tenantContext);
            } else {
                account = osgiKillbillAPI.getAccountUserApi().getAccountByKey(request.getAccount().getExternalKey(), tenantContext);
            }

            final Account gRPCAccount = kbAccountToGRPCAccount(account);
            responseBuilder.setAccount(gRPCAccount);
        } catch (final AccountApiException e) {
            final BillingException billingException = toGRPCBillingException(e);
            responseBuilder.setBillingException(billingException);
        }

        responseObserver.onNext(responseBuilder.build());
        responseObserver.onCompleted();
    }

    private UUID safeUUIDFromString(final String s) {
        return Strings.isNullOrEmpty(s) ? null : UUID.fromString(s);
    }

    private BillingException toGRPCBillingException(final Exception e) {
        final BillingException.Builder builder = BillingException.newBuilder()
                                                                 .setClassName(e.getClass().getName());
        if (e.getMessage() != null) {
            builder.setMessage(e.getMessage());
        }
        if (e.getCause() != null) {
            if (e.getCause().getClass() != null) {
                builder.setCauseClassName(e.getCause().getClass().getName());
            }
            if (e.getCause().getMessage() != null) {
                builder.setCauseMessage(e.getCause().getMessage());
            }
        }
        if (e instanceof BillingExceptionBase) {
            builder.setCode(((BillingExceptionBase) e).getCode());
        }

        return builder.build();
    }

    private Account kbAccountToGRPCAccount(final org.killbill.billing.account.api.Account account) {
        final Builder builder = Account.newBuilder()
                                       .setAccountId(account.getId().toString())
                                       .setExternalKey(account.getExternalKey());
        if (account.getName() != null) {
            builder.setName(account.getName());
        }
        if (account.getFirstNameLength() != null) {
            builder.setFirstNameLength(account.getFirstNameLength());
        }
        if (account.getEmail() != null) {
            builder.setEmail(account.getEmail());
        }
        if (account.getBillCycleDayLocal() != null) {
            builder.setBillCycleDayLocal(account.getBillCycleDayLocal());
        }
        if (account.getCurrency() != null) {
            builder.setCurrency(Currency.valueOf(account.getCurrency().name()));
        }
        if (account.getParentAccountId() != null) {
            builder.setParentAccountId(account.getParentAccountId().toString());
        }
        if (account.isPaymentDelegatedToParent() != null) {
            builder.setIsPaymentDelegatedToParent(account.isPaymentDelegatedToParent());
        }
        if (account.getPaymentMethodId() != null) {
            builder.setPaymentMethodId(account.getPaymentMethodId().toString());
        }
        if (account.getReferenceTime() != null) {
            builder.setReferenceTime(account.getReferenceTime().toString());
        }
        if (account.getTimeZone() != null) {
            builder.setTimeZone(account.getTimeZone().toString());
        }
        if (account.getAddress1() != null) {
            builder.setAddress1(account.getAddress1());
        }
        if (account.getAddress2() != null) {
            builder.setAddress1(account.getAddress2());
        }
        if (account.getPostalCode() != null) {
            builder.setPostalCode(account.getPostalCode());
        }
        if (account.getCompanyName() != null) {
            builder.setCompany(account.getCompanyName());
        }
        if (account.getCity() != null) {
            builder.setCity(account.getCity());
        }
        if (account.getStateOrProvince() != null) {
            builder.setState(account.getStateOrProvince());
        }
        if (account.getCountry() != null) {
            builder.setCountry(account.getCountry());
        }
        if (account.getLocale() != null) {
            builder.setLocale(account.getLocale());
        }
        if (account.getPhone() != null) {
            builder.setPhone(account.getPhone());
        }
        if (account.getNotes() != null) {
            builder.setNotes(account.getNotes());
        }
        if (account.isMigrated() != null) {
            builder.setIsMigrated(account.isMigrated());
        }
        return builder.build();
    }

    private DateTimeZone toDateTimeZone(final String s) {
        return Strings.isNullOrEmpty(s) ? null : DateTimeZone.forID(s);
    }

    private DateTime toDateTime(final String s) {
        return Strings.isNullOrEmpty(s) ? null : new DateTime(s, DateTimeZone.UTC);
    }
}
